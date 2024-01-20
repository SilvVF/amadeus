package io.silv.manga.storeage

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.model.MangaCover
import io.silv.common.model.MangaDexSource
import io.silv.data.download.DownloadCache
import io.silv.data.download.DownloadManager
import io.silv.domain.manga.interactor.GetManga
import io.silv.domain.manga.model.toResource
import io.silv.domain.manga.repository.MangaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class StorageScreenModel(
    mangaRepository: MangaRepository,
    private val downloadCache: DownloadCache,
    private val getManga: GetManga,
    private val downloadManager: DownloadManager,
) : StateScreenModel<StorageScreenState>(StorageScreenState.Loading) {

    init {
        screenModelScope.launch(Dispatchers.IO) {
            combine(
                downloadCache.changes,
                downloadCache.isInitializing,
                mangaRepository.observeLibraryManga()
            ) { _, _, libraryManga ->
                    // initialize the screen with an empty state
                    mutableState.update {
                        StorageScreenState.Success(
                            items = emptyList(),
                            nonLibraryItems = emptyList()
                        )
                    }

                    downloadCache.nonLibraryMangaFiles(libraryManga.map { it.toResource() }).forEach { (name, size, count) ->
                        val random = Random(
                            name.sumOf { it.code }
                        )

                        val manga = mangaRepository.getMangaByTitle(name)

                        val item = StorageItem(
                            id = manga?.id ?: "",
                            title = name,
                            size = size,
                            cover = MangaCover(
                                manga?.id ?: "",
                                manga?.coverArt ?: "",
                                false,
                                0L
                            ),
                            entriesCount = count,
                            color = Color(
                                random.nextInt(255),
                                random.nextInt(255),
                                random.nextInt(255),
                            ),
                        )

                        mutableState.update { state ->
                            when (state) {
                                is StorageScreenState.Success -> state.copy(
                                    nonLibraryItems = (state.nonLibraryItems + item).sortedByDescending { it.size },
                                )

                                else -> state
                            }
                        }
                    }

                    libraryManga.forEach { manga ->
                        val random = Random(
                            manga.id.sumOf { it.code }
                        )
                        val item = StorageItem(
                            id = manga.id,
                            title = manga.titleEnglish,
                            size = downloadCache.getDownloadSize(manga.toResource()),
                            cover = MangaCover(
                                manga.id,
                                manga.coverArt,
                                manga.inLibrary,
                                manga.coverLastModified
                            ),
                            entriesCount = downloadCache.getDownloadCount(manga.toResource()),
                            color = Color(
                                random.nextInt(255),
                                random.nextInt(255),
                                random.nextInt(255),
                            ),
                        )

                        mutableState.update { state ->
                            when (state) {
                                is StorageScreenState.Success -> state.copy(
                                    items = (state.items + item).sortedByDescending { it.size },
                                )

                                else -> state
                            }
                        }
                    }
                }
                .collect()
        }
    }

    fun deleteItemByTitle(title: String) {
        screenModelScope.launch(Dispatchers.IO) {

            downloadCache.removeManga(title)
        }
    }

    fun onDeleteItem(id: String) {
        screenModelScope.launch(Dispatchers.IO) {

            val manga = getManga.await(id) ?: return@launch

            downloadManager.deleteManga(manga.toResource(), MangaDexSource)
        }
    }
}

sealed class StorageScreenState {

    @Immutable
    data object Loading : StorageScreenState()

    @Immutable
    data class Success(
        val nonLibraryItems: List<StorageItem>,
        val items: List<StorageItem>
    ) : StorageScreenState()
}

fun Long.toSize(): String {
    val kb = 1000
    val mb = kb * kb
    val gb = mb * kb
    return when {
        this >= gb -> "%.2f GB".format(this.toFloat() / gb)
        this >= mb -> "%.2f MB".format(this.toFloat() / mb)
        this >= kb -> "%.2f KB".format(this.toFloat() / kb)
        else -> "$this B"
    }
}
