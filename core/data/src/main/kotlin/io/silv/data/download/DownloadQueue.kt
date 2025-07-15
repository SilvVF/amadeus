@file:OptIn(ExperimentalCoroutinesApi::class)

package io.silv.data.download

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import io.silv.common.mutablePropertyFrom
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.collections.filter
import kotlin.collections.first
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.sequences.filter
import kotlin.sequences.groupBy

interface Notifier {
    fun onWarning(reason: String?)
    fun onError(message: String?)
    fun onComplete()
    fun onPaused()
    fun dismissProgress()

    companion object {
        val NoOpNotifier = object : Notifier {
            override fun dismissProgress() = Unit
            override fun onWarning(reason: String?) = Unit
            override fun onError(message: String?) = Unit
            override fun onComplete() = Unit
            override fun onPaused() = Unit
        }
    }
}

@Immutable
@Stable
data class QItem<T>(
    val data: T,
    val maxRetry: Int = 0,
) {
    private val _retryFlow = MutableStateFlow(0)
    val retryFlow = _retryFlow.asStateFlow()

    private val _statusFlow = MutableStateFlow(State.IDLE)
    val statusFlow = _statusFlow.asStateFlow()

    var retry: Int by mutablePropertyFrom(_retryFlow)
        internal set

    var status: State by mutablePropertyFrom(_statusFlow)
        internal set

    enum class State(val value: Int) {
        IDLE(0),
        QUEUE(1),
        RUNNING(2),
        COMPLETED(3),
        ERROR(4),
    }
}


class ThreadSafeValue<T>(initialValue: T) : ReadWriteProperty<Any?, T> {

    private val mutex = Mutex()
    private var _value: T = initialValue

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = runBlocking {
        mutex.withLock { _value }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        runBlocking {
            mutex.withLock { _value = value }
        }
    }
}

fun <T, KEY> Flow<List<QItem<T>>>.transformActiveJobsUntilError(
    keySelector: (T) -> KEY,
    parallel: Int
): Flow<List<QItem<T>>> {
    return transformLatest { queue ->
        while (coroutineContext.isActive) {
            val activeJobs = queue.asSequence()
                .filter {
                    it.status.value <= QItem.State.RUNNING.value
                } // Ignore completed items, leave them in the queue
                .groupBy(keySelector = { keySelector(it.data) })
                .toList()
                .take(parallel)
                .map { (_, items) -> items.first() }

            emit(activeJobs)

            if (activeJobs.isEmpty()) break

            // Suspend until an item enters the ERROR state
            val anyJobHasErrorFlow =
                combine(
                    activeJobs.map { it.statusFlow }
                ) { statusStates ->
                    statusStates.any { state -> state == QItem.State.ERROR }
                }

            anyJobHasErrorFlow
                .filter { hasError -> hasError }
                .first()
        }
    }
        .distinctUntilChanged()
}

suspend fun <T> retry(
    retries: Int,
    predicate: suspend (attempt: Int) -> Result<T>
): Result<T> {
    require(retries >= 0) { "Expected positive amount of retries, but had $retries" }
    var throwable: Throwable? = null
    (0..retries).forEach { attempt ->

        require(coroutineContext.isActive)

        try {
            val result = predicate(attempt)
            if (result.isSuccess) {
                return result
            }
            throwable = result.exceptionOrNull()
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            throwable = e
        }
    }
    return Result.failure(throwable ?: IllegalStateException())
}

fun <T, KEY> DownloadQueue<T, KEY>.areAllItemsFinished(): Boolean {
    return queueState.value.none { it.status.value <= QItem.State.RUNNING.value }
}

fun <T, KEY> DownloadQueue<T, KEY>.statusFlow() = queueState.flatMapLatest { downloads ->
    downloads
        .map { download ->
            download.statusFlow.drop(1).map { download }
        }
        .merge()
}
    .onStart {
        emitAll(
            queueState.value.filter { download -> download.status == QItem.State.RUNNING }.asFlow()
        )
    }

interface QueueStore<T> {
    suspend fun addAll(downloads: List<T>)
    suspend fun remove(download: T)
    suspend fun removeAll(downloads: List<T>)
    suspend fun clear()
    suspend fun restore(): List<QItem<T>>

    companion object {
        fun <T> noOpStore(): QueueStore<T> = object : QueueStore<T> {
            override suspend fun addAll(downloads: List<T>) {}
            override suspend fun remove(download: T) {}
            override suspend fun removeAll(downloads: List<T>) {}
            override suspend fun clear() {}
            override suspend fun restore(): List<QItem<T>> = emptyList()
        }
    }
}


class DownloadQueue<T, KEY>(
    private val workDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val parallel: Int = 3,
    private val notifier: Notifier = Notifier.NoOpNotifier,
    private val store: QueueStore<T> = QueueStore.noOpStore<T>(),
    private val keySelector: (T) -> KEY,
    private val initialItems: suspend () -> List<QItem<T>> = { emptyList() },
    private val doWork: suspend CoroutineScope.(T) -> Result<Unit>
) : Notifier by notifier {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            addAllToQueue(store.restore())
            addAllToQueue(initialItems())
        }
    }

    private val _queueState = MutableStateFlow<List<QItem<T>>>(emptyList())
    val queueState = _queueState.asStateFlow()

    private var queueJob: Job? = null

    var isPaused by ThreadSafeValue(false)

    val isRunning: Boolean
        get() = queueJob?.isActive == true

    private fun launchQueueJob() {
        if (isRunning) return

        queueJob = scope.launch {
            isPaused = false

            // transform active jobs in state < RUNNING until any enters the error state
            val activeJobsFlow = queueState
                .transformActiveJobsUntilError(keySelector, parallel)
                .onCompletion { t ->
                    // a job entered the error state
                    // or no items left stop the queue
                    when {
                        areAllItemsFinished() -> onComplete()
                        t != null -> onWarning(t.message)
                        isPaused -> onPaused()
                        else -> onComplete()
                    }
                }

            supervisorScope {
                val queueJobs = mutableMapOf<QItem<T>, Job>()

                activeJobsFlow.collectLatest { activeJobs ->

                    val jobsToStop = queueJobs.filter { it.key !in activeJobs }
                    jobsToStop.forEach { (item, job) ->
                        job.cancel()
                        queueJobs.remove(item)
                    }

                    val itemsToStart = activeJobs.filter { it !in queueJobs }
                    itemsToStart.forEach { item ->
                        queueJobs[item] = launch(workDispatcher) {
                            try {
                                retry(item.maxRetry) {

                                    item.status = QItem.State.RUNNING
                                    item.retry = it

                                    doWork(item.data)
                                }
                                    .onSuccess {
                                        item.status = QItem.State.COMPLETED
                                        removeFromQueue(item)
                                    }
                                    .onFailure {
                                        item.status = QItem.State.ERROR
                                    }

                                if (areAllItemsFinished()) {
                                    stop()
                                }
                            } catch (e: Throwable) {
                                if (e is CancellationException) {
                                    throw e
                                } else {
                                    onError(e.message)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun stop(reason: String? = null) {
        queueJob?.cancel()
        queueState.value
            .filter { it.status == QItem.State.RUNNING }
            .forEach { it.status = QItem.State.ERROR }

        if (reason != null) {
            onWarning(reason)
            return
        }
    }


    fun pause() {
        queueJob?.cancel()
        queueState.value
            .filter { it.status == QItem.State.RUNNING }
            .forEach { it.status = QItem.State.QUEUE }
        isPaused = true
    }

    fun start(): Boolean {
        if (isRunning || queueState.value.isEmpty()) {
            return false
        }

        val pending = queueState.value.filter { it.status != QItem.State.COMPLETED }
        pending.forEach { if (it.status != QItem.State.QUEUE) it.status = QItem.State.QUEUE }

        launchQueueJob()

        return pending.isNotEmpty()
    }

    private fun internalClearQueue() {
        _queueState.update {
            it.forEach { item ->
                if (item.status == QItem.State.RUNNING ||
                    item.status == QItem.State.QUEUE
                ) {
                    item.status = QItem.State.IDLE
                }
            }
            emptyList()
        }
    }

    fun clear() {
        queueJob?.cancel()
        internalClearQueue()
        stop()
        dismissProgress()
    }

    suspend fun updateQueue(items: List<QItem<T>>) {
        val wasRunning = isRunning

        if (items.isEmpty()) {
            clear()
            return
        }

        pause()
        internalClearQueue()
        addAllToQueue(items)

        if (wasRunning) {
            start()
        }
    }

    suspend fun addAllToQueue(items: List<QItem<T>>) {
        _queueState.update {
            store.addAll(items.map(QItem<T>::data))
            it + items.onEach {
                it.status = QItem.State.QUEUE
            }
        }
    }

    suspend fun removeFromQueueIf(predicate: (QItem<T>) -> Boolean) {
        _queueState.update { queue ->
            val downloads = queue.filter { predicate(it) }
            store.removeAll(queue.map(QItem<T>::data))
            downloads.forEach { download ->
                if (download.status == QItem.State.RUNNING || download.status == QItem.State.QUEUE) {
                    download.status = QItem.State.IDLE
                }
            }
            queue - downloads
        }
    }

    suspend fun removeFromQueue(item: QItem<T>) {
        _queueState.update {
            store.remove(item.data)
            if (item.status == QItem.State.RUNNING || item.status == QItem.State.QUEUE) {
                item.status = QItem.State.COMPLETED
            }
            it - item
        }
    }
}