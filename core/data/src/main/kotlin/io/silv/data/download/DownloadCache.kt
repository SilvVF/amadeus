package io.silv.data.download

import android.content.Context
import android.net.Uri
import com.hippo.unifile.UniFile
import io.silv.common.model.ChapterResource
import io.silv.common.model.MangaDexSource
import io.silv.common.model.Source
import io.silv.common.model.MangaResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.encodeToString
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File
import kotlin.time.Duration.Companion.hours

/**
 * Cache where we dump the downloads directory from the filesystem. This class is needed because
 * directory checking is expensive and it slows down the app. The cache is invalidated by the time
 * defined in [renewInterval] as we don't have any control over the filesystem and the user can
 * delete the folders at any time without the app noticing.
 */
@OptIn(ExperimentalSerializationApi::class, ExperimentalSerializationApi::class)
internal class DownloadCache(
    private val context: Context,
    private val provider: DownloadProvider,
    private val storageManager: StorageManager,
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _changes: Channel<Unit> = Channel(Channel.UNLIMITED)
    val changes = _changes.receiveAsFlow()
        .onStart { emit(Unit) }
        .shareIn(scope, SharingStarted.Lazily, 1)

    /**
     * The interval after which this cache should be invalidated. 1 hour shouldn't cause major
     * issues, as the cache is only used for UI feedback.
     */
    private val renewInterval = 1.hours.inWholeMilliseconds

    /**
     * The last time the cache was refreshed.
     */
    private var lastRenew = 0L
    private var renewalJob: Job? = null

    private val _isInitializing = MutableStateFlow(false)
    @OptIn(FlowPreview::class)
    val isInitializing = _isInitializing
        .debounce(1000L) // Don't notify if it finishes quickly enough
        .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    private val diskCacheFile: File
        get() = File(context.cacheDir, "dl_index_cache_v3")

    private val rootDownloadsDirLock = Mutex()
    private var rootDownloadsDir = RootDirectory(storageManager.getDownloadsDirectory())

    init {
        // Attempt to read cache file
        scope.launch {
            rootDownloadsDirLock.withLock {
                try {
                    val diskCache = diskCacheFile.inputStream().use {
                        Json.decodeFromStream<RootDirectory>(it)
                    }
                    rootDownloadsDir = diskCache
                    lastRenew = System.currentTimeMillis()
                } catch (e: Throwable) {
                    diskCacheFile.delete()
                }
            }
        }
    }

    /**
     * Returns true if the chapter is downloaded.
     *
     * @param chapterName the name of the chapter to query.
     * @param chapterScanlator scanlator of the chapter to query
     * @param mangaTitle the title of the manga to query.
     * @param sourceId the id of the source of the chapter.
     * @param skipCache whether to skip the directory cache and check in the filesystem.
     */
    fun isChapterDownloaded(
        chapterName: String,
        chapterScanlator: String?,
        mangaTitle: String,
        sourceId: Long,
        skipCache: Boolean,
    ): Boolean {
        if (skipCache) {
            return provider.findChapterDir(chapterName, chapterScanlator, mangaTitle, MangaDexSource) != null
        }

        renewCache()

        val sourceDir = rootDownloadsDir.sourceDirs[sourceId]
        if (sourceDir != null) {
            val mangaDir = sourceDir.mangaDirs[provider.getMangaDirName(mangaTitle)]
            if (mangaDir != null) {
                return provider.getValidChapterDirNames(
                    chapterName,
                    chapterScanlator,
                ).any { it in mangaDir.chapterDirs }
            }
        }
        return false
    }

    /**
     * Returns the amount of downloaded chapters.
     */
    fun getTotalDownloadCount(): Int {
        renewCache()

        return rootDownloadsDir.sourceDirs.values.sumOf { sourceDir ->
            sourceDir.mangaDirs.values.sumOf { mangaDir ->
                mangaDir.chapterDirs.size
            }
        }
    }

    /**
     * Returns the amount of downloaded chapters for a manga.
     *
     * @param manga the manga to check.
     */
    fun getDownloadCount(manga: MangaResource): Int {
        renewCache()

        val sourceDir = rootDownloadsDir.sourceDirs[MangaDexSource.id]
        if (sourceDir != null) {
            val mangaDir = sourceDir.mangaDirs[provider.getMangaDirName(manga.title)]
            if (mangaDir != null) {
                return mangaDir.chapterDirs.size
            }
        }
        return 0
    }

    /**
     * Adds a chapter that has just been download to this cache.
     *
     * @param chapterDirName the downloaded chapter's directory name.
     * @param mangaUniFile the directory of the manga.
     * @param manga the manga of the chapter.
     */
    suspend fun addChapter(chapterDirName: String, mangaUniFile: UniFile, manga: MangaResource) {
        rootDownloadsDirLock.withLock {
            // Retrieve the cached source directory or cache a new one
            var sourceDir = rootDownloadsDir.sourceDirs[MangaDexSource.id]
            if (sourceDir == null) {
                val source = MangaDexSource
                val sourceUniFile = provider.findSourceDir(source) ?: return
                sourceDir = SourceDirectory(sourceUniFile)
                rootDownloadsDir.sourceDirs += source.id to sourceDir
            }

            // Retrieve the cached manga directory or cache a new one
            val mangaDirName = provider.getMangaDirName(manga.title)
            var mangaDir = sourceDir.mangaDirs[mangaDirName]
            if (mangaDir == null) {
                mangaDir = MangaDirectory(mangaUniFile)
                sourceDir.mangaDirs += mangaDirName to mangaDir
            }

            // Save the chapter directory
            mangaDir.chapterDirs += chapterDirName
        }

        notifyChanges()
    }

    /**
     * Removes a chapter that has been deleted from this cache.
     *
     * @param chapter the chapter to remove.
     * @param manga the manga of the chapter.
     */
    suspend fun removeChapter(chapter: ChapterResource, manga: MangaResource) {
        rootDownloadsDirLock.withLock {
            val sourceDir = rootDownloadsDir.sourceDirs[MangaDexSource.id] ?: return
            val mangaDir = sourceDir.mangaDirs[provider.getMangaDirName(manga.title)] ?: return
            provider.getValidChapterDirNames(chapter.title, chapter.scanlator).forEach {
                if (it in mangaDir.chapterDirs) {
                    mangaDir.chapterDirs -= it
                }
            }
        }

        notifyChanges()
    }

    /**
     * Removes a list of chapters that have been deleted from this cache.
     *
     * @param chapters the list of chapter to remove.
     * @param manga the manga of the chapter.
     */
    suspend fun removeChapters(chapters: List<ChapterResource>, manga: MangaResource) {
        rootDownloadsDirLock.withLock {
            val sourceDir = rootDownloadsDir.sourceDirs[MangaDexSource.id] ?: return
            val mangaDir = sourceDir.mangaDirs[provider.getMangaDirName(manga.title)] ?: return
            chapters.forEach { chapter ->
                provider.getValidChapterDirNames(chapter.title, chapter.scanlator).forEach {
                    if (it in mangaDir.chapterDirs) {
                        mangaDir.chapterDirs -= it
                    }
                }
            }
        }

        notifyChanges()
    }

    /**
     * Removes a manga that has been deleted from this cache.
     *
     * @param manga the manga to remove.
     */
    suspend fun removeManga(manga: MangaResource) {
        rootDownloadsDirLock.withLock {
            val sourceDir = rootDownloadsDir.sourceDirs[MangaDexSource.id] ?: return
            val mangaDirName = provider.getMangaDirName(manga.title)
            if (sourceDir.mangaDirs.containsKey(mangaDirName)) {
                sourceDir.mangaDirs -= mangaDirName
            }
        }

        notifyChanges()
    }

    suspend fun removeSource(source: Source) {
        rootDownloadsDirLock.withLock {
            rootDownloadsDir.sourceDirs -= source.id
        }

        notifyChanges()
    }

    fun invalidateCache() {
        lastRenew = 0L
        renewalJob?.cancel()
        diskCacheFile.delete()
        renewCache()
    }

    /**
     * Renews the downloads cache.
     */
    @OptIn(InternalCoroutinesApi::class)
    private fun renewCache() {
        // Avoid renewing cache if in the process nor too often
        if (lastRenew + renewInterval >= System.currentTimeMillis() || renewalJob?.isActive == true) {
            return
        }

        renewalJob = scope.launch(Dispatchers.IO ) {
            if (lastRenew == 0L) {
                _isInitializing.emit(true)
            }

            // Try to wait until extensions and sources have loaded
            val sources = getSources()
            val sourceMap = sources.associate { provider.getSourceDirName(it).lowercase() to it.id }

            rootDownloadsDirLock.withLock {
                rootDownloadsDir = RootDirectory(storageManager.getDownloadsDirectory())

                val sourceDirs = rootDownloadsDir.dir?.listFiles().orEmpty()
                    .filter { it.isDirectory && !it.name.isNullOrBlank() }
                    .mapNotNull { dir ->
                        val sourceId = sourceMap[dir.name!!.lowercase()]
                        sourceId?.let { it to SourceDirectory(dir) }
                    }
                    .toMap()

                rootDownloadsDir.sourceDirs = sourceDirs

                sourceDirs.values
                    .map { sourceDir ->
                        async {
                            sourceDir.mangaDirs = sourceDir.dir?.listFiles().orEmpty()
                                .filter { it.isDirectory && !it.name.isNullOrBlank() }
                                .associate { it.name!! to MangaDirectory(it) }

                            sourceDir.mangaDirs.values.forEach { mangaDir ->
                                val chapterDirs = mangaDir.dir?.listFiles().orEmpty()
                                    .mapNotNull {
                                        when {
                                            // Ignore incomplete downloads
                                            it.name?.endsWith(Downloader.TMP_DIR_SUFFIX) == true -> null
                                            // Folder of images
                                            it.isDirectory -> it.name
                                            else -> null
                                        }
                                    }
                                    .toMutableSet()

                                mangaDir.chapterDirs = chapterDirs
                            }
                        }
                    }
                    .awaitAll()
            }

            _isInitializing.emit(false)
        }.also {
            it.invokeOnCompletion(onCancelling = true) { exception ->
                if (exception != null && exception !is CancellationException) {
                    exception.printStackTrace()
                }
                lastRenew = System.currentTimeMillis()
                notifyChanges()
            }
        }

        // Mainly to notify the indexing notifier UI
        notifyChanges()
    }

    private fun getSources(): List<Source> {
        return listOf(MangaDexSource)
    }

    private fun notifyChanges() {
        scope.launch(NonCancellable) {
            _changes.send(Unit)
        }
        updateDiskCache()
    }

    private var updateDiskCacheJob: Job? = null

    @OptIn(InternalSerializationApi::class)
    private fun updateDiskCache() {
        updateDiskCacheJob?.cancel()
        updateDiskCacheJob = scope.launch(Dispatchers.IO) {
            delay(1000)
            ensureActive()
            val bytes = encodeToString(RootDirectory::class.serializer(), rootDownloadsDir).toByteArray()
            ensureActive()
            try {
                diskCacheFile.writeBytes(bytes)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}

/**
 * Class to store the files under the root downloads directory.
 */
@Serializable
private class RootDirectory(
    @Serializable(with = UniFileAsStringSerializer::class)
    val dir: UniFile?,
    var sourceDirs: Map<Long, SourceDirectory> = mapOf(),
)

/**
 * Class to store the files under a source directory.
 */
@Serializable
private class SourceDirectory(
    @Serializable(with = UniFileAsStringSerializer::class)
    val dir: UniFile?,
    var mangaDirs: Map<String, MangaDirectory> = mapOf(),
)

/**
 * Class to store the files under a manga directory.
 */
@Serializable
private class MangaDirectory(
    @Serializable(with = UniFileAsStringSerializer::class)
    val dir: UniFile?,
    var chapterDirs: MutableSet<String> = mutableSetOf(),
)

private object UniFileAsStringSerializer : KSerializer<UniFile?>, KoinComponent {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UniFile", PrimitiveKind.STRING)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: UniFile?) {
        return if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeString(value.uri.toString())
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): UniFile? {
        return if (decoder.decodeNotNullMark()) {
            UniFile.fromUri(get(), Uri.parse(decoder.decodeString()))
        } else {
            decoder.decodeNull()
        }
    }
}