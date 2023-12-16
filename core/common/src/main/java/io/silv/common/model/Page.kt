package io.silv.common.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

@Serializable
open class Page(
    val index: Int,
    val url: String = "",
    var imageUrl: String? = null,
) : ProgressListener {

    val number: Int
        get() = index + 1

    @kotlinx.serialization.Transient
    private val _statusFlow = MutableStateFlow(State.QUEUE)

    @kotlinx.serialization.Transient
    val statusFlow = _statusFlow.asStateFlow()
    var status: State
        get() = _statusFlow.value
        set(value) {
            _statusFlow.value = value
        }

    @kotlinx.serialization.Transient
    private val _progressFlow = MutableStateFlow(0)

    @kotlinx.serialization.Transient
    val progressFlow = _progressFlow.asStateFlow()

    var progress: Int
        get() = _progressFlow.value
        set(value) {
            _progressFlow.value = value
        }

    override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
        progress = if (contentLength > 0) {
            (100 * bytesRead / contentLength).toInt()
        } else {
            -1
        }
    }

    enum class State {
        QUEUE,
        LOAD_PAGE,
        DOWNLOAD_IMAGE,
        READY,
        ERROR,
    }
}

interface ProgressListener {
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}