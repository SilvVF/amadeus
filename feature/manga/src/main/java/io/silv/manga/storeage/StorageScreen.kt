package io.silv.manga.storeage

import android.content.Context
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEach
import androidx.datastore.core.Storage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.common.DependencyAccessor
import io.silv.data.download.ChapterCache
import io.silv.data.manga.repository.MangaRepository
import io.silv.data.util.DiskUtil
import io.silv.di.dataDeps
import io.silv.di.rememberDataDependency

import io.silv.manga.R
import io.silv.ui.theme.LocalSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import java.io.File



class StorageScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val space = LocalSpacing.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { StorageScreenModel() }
        val state = screenModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current

        val storageState = storageInfoPresenter(context)

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    title = { Text("Data and storage") }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
            ) {
                StorageInfo(
                    storageState,
                    Modifier
                        .fillMaxWidth()
                        .padding(space.large)
                )
                Spacer(modifier = Modifier.height(space.med))
                ChapterCacheInfo(
                    storageState,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(space.med))
                UnusedMangaInfo(
                    storageState,
                    modifier = Modifier.fillMaxWidth()
                )
                when (val storage = state.value) {
                    StorageScreenState.Loading -> Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }

                    is StorageScreenState.Success -> {
                        if (storage.items.isEmpty()) {
                            Text(
                                text = "No downloaded chapters for library manga",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally)
                            )
                        } else {
                            Text(
                                text = remember(storage.items) {
                                    storage.items.fastFold(0L) { acc, storageItem -> acc + storageItem.size }
                                        .toSize()
                                },
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally)
                            )
                            storage.items.fastForEach {
                                StorageItem(
                                    item = it,
                                    onDelete = screenModel::onDeleteItem
                                )
                                Spacer(modifier = Modifier.height(space.small))
                            }
                        }
                        if (storage.nonLibraryItems.isEmpty()) {
                            Text(
                                text = "No downloaded chapters for non-library manga",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally)
                            )
                        } else {
                            Text(
                                text = remember(storage.nonLibraryItems) {
                                    storage.nonLibraryItems.fastFold(0L) { acc, storageItem -> acc + storageItem.size }
                                        .toSize()
                                },
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally)
                            )
                            storage.nonLibraryItems.fastForEach { item ->
                                StorageItem(
                                    item = item,
                                    onDelete = screenModel::onDeleteItem
                                        .takeIf { item.id.isNotBlank() }
                                        ?: { screenModel::deleteItemByTitle.invoke(item.title) }
                                )
                                Spacer(modifier = Modifier.height(space.small))
                            }
                        }
                    }
                }
            }
        }
    }
}


sealed interface StorageEvent {
    data object DeleteUnusedManga: StorageEvent
    data object ClearCache: StorageEvent
}

data class StorageInfoState(
    val unusedManga: Int,
    val cacheSize: String,
    val infos: List<FileInfo>,
    val events: (StorageEvent) -> Unit
)

data class FileInfo(
    val absPath: String,
    val available: Long,
    val availableText: String,
    val total: Long,
    val totalText: String
)

@OptIn(DependencyAccessor::class)
@Composable
fun storageInfoPresenter(
    context: Context,
    mangaRepository: MangaRepository = dataDeps.mangaRepository,
    chapterCache: ChapterCache = dataDeps.chapterCache,
): StorageInfoState {
    val scope = rememberCoroutineScope()
    val unusedCount by mangaRepository.observeUnusedCount()
        .collectAsStateWithLifecycle(initialValue = 0)
    var cacheInvalidate by remember { mutableIntStateOf(0) }

    var readableSize by remember { mutableStateOf("") }

    val storages by produceState<List<FileInfo>>(emptyList()) {
        val info = withContext(Dispatchers.IO) {
            DiskUtil.getExternalStorages(context).map { file ->
                val available = DiskUtil.getAvailableStorageSpace(file)
                val total = DiskUtil.getTotalStorageSpace(file)
                FileInfo(
                    absPath = file.absolutePath,
                    available,
                    Formatter.formatFileSize(context, available),
                    total,
                    Formatter.formatFileSize(context, total)
                )
            }
        }
        value = info
    }

    LaunchedEffect(cacheInvalidate) {
        readableSize = withContext(Dispatchers.IO) {
            try {
                chapterCache.readableSize()
            } catch (e: IOException) {
                "failed to get size ${e.message}"
            }
        }
    }

    return StorageInfoState(
        unusedManga = unusedCount,
        cacheSize = readableSize,
        infos = storages
    ) { event ->
        when(event) {
            StorageEvent.DeleteUnusedManga -> {
                scope.launch(NonCancellable) {
                    try {
                        mangaRepository.deleteUnused()

                        withContext(Dispatchers.Main.immediate) {
                            Toast
                                .makeText(
                                    context,
                                    "Cleared Unused Manga",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    } catch (e: Throwable) {
                        withContext(Dispatchers.Main.immediate) {
                            Toast
                                .makeText(
                                    context,
                                    context.getString(R.string.cache_delete_error),
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    }
                }
            }

            StorageEvent.ClearCache -> {
                scope.launch(NonCancellable) {
                    try {
                        val deletedFiles = chapterCache.clear()
                        withContext(Dispatchers.Main.immediate) {
                            Toast
                                .makeText(
                                    context,
                                    context.getString(R.string.cache_deleted, deletedFiles),
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                            cacheInvalidate += 1
                        }
                    } catch (e: Throwable) {
                        withContext(Dispatchers.Main.immediate) {
                            Toast
                                .makeText(
                                    context,
                                    context.getString(R.string.cache_delete_error),
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(DependencyAccessor::class)
@Composable
fun UnusedMangaInfo(
    state: StorageInfoState,
    modifier: Modifier = Modifier
) {
    val space = LocalSpacing.current

    Column(
        modifier = modifier
            .clickable {
                state.events(StorageEvent.DeleteUnusedManga)
            }
            .padding(space.large)
    ) {
        Text("Clear unused manga", style = MaterialTheme.typography.titleMedium)
        Text(text = "Unused: ${state.unusedManga}", style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(DependencyAccessor::class)
@Composable
private fun ChapterCacheInfo(
    state: StorageInfoState,
    modifier: Modifier = Modifier
) {
    val space = LocalSpacing.current
    Column(
        modifier = modifier
            .clickable {
               state.events(StorageEvent.ClearCache)
            }
            .padding(space.large)
    ) {
        Text("Clear chapter cache", style = MaterialTheme.typography.titleMedium)
        Text(text = "Used: ${state.cacheSize}", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun StorageInfo(
    state: StorageInfoState,
    modifier: Modifier = Modifier,
) {
    val space = LocalSpacing.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space.small),
    ) {
        state.infos.forEach {
            StorageInfo(it)
        }
    }
}

@Composable
private fun StorageInfo(
    file: FileInfo,
) {
    val space = LocalSpacing.current
    Column(
        verticalArrangement = Arrangement.spacedBy(space.xs),
    ) {
        Text(
            text = file.absPath,
            style = MaterialTheme.typography.headlineSmall,
        )

        LinearProgressIndicator(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .fillMaxWidth()
                .height(12.dp),
            progress = { (1 - (file.available / file.total.toFloat())) },
        )

        Text(
            text = stringResource(R.string.available_disk_space_info, file.availableText, file.totalText),
            modifier = Modifier.alpha(0.78f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}