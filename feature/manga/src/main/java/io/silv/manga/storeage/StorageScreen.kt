package io.silv.manga.storeage

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.data.download.ChapterCache
import io.silv.data.util.DiskUtil
import io.silv.domain.manga.repository.MangaRepository
import io.silv.manga.R
import io.silv.ui.theme.LocalSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File


class StorageScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val space = LocalSpacing.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<StorageScreenModel>()
        val state = screenModel.state.collectAsStateWithLifecycle()

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
                    Modifier
                        .fillMaxWidth()
                        .padding(space.large)
                )
                Spacer(modifier = Modifier.height(space.med))
                ChapterCacheInfo(
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(space.med))
                UnusedMangaInfo(
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

@Composable
fun UnusedMangaInfo(
    modifier: Modifier = Modifier
) {
    val mangaRepository = koinInject<MangaRepository>()
    val unusedCount by mangaRepository.observeUnusedCount()
        .collectAsStateWithLifecycle(initialValue = 0)

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val space = LocalSpacing.current

    Column(
        modifier = modifier
            .clickable {
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
            .padding(space.large)
    ) {
        Text("Clear unused manga", style = MaterialTheme.typography.titleMedium)
        Text(text = "Unused: $unusedCount", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ChapterCacheInfo(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val space = LocalSpacing.current

    val chapterCache = koinInject<ChapterCache>()
    var cacheReadableSizeSema by remember { mutableIntStateOf(0) }
    val cacheReadableSize = remember(cacheReadableSizeSema) { chapterCache.readableSize }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .clickable {
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
                            cacheReadableSizeSema++
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
            .padding(space.large)
    ) {
        Text("Clear chapter cache", style = MaterialTheme.typography.titleMedium)
        Text(text = "Used: $cacheReadableSize", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun StorageInfo(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val space = LocalSpacing.current
    val storages = remember { DiskUtil.getExternalStorages(context) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space.small),
    ) {
        storages.forEach {
            StorageInfo(it)
        }
    }
}

@Composable
private fun StorageInfo(
    file: File,
) {
    val context = LocalContext.current
    val space = LocalSpacing.current

    val available = remember(file) { DiskUtil.getAvailableStorageSpace(file) }
    val availableText = remember(available) { Formatter.formatFileSize(context, available) }
    val total = remember(file) { DiskUtil.getTotalStorageSpace(file) }
    val totalText = remember(total) { Formatter.formatFileSize(context, total) }

    Column(
        verticalArrangement = Arrangement.spacedBy(space.xs),
    ) {
        Text(
            text = file.absolutePath,
            style = MaterialTheme.typography.headlineSmall,
        )

        LinearProgressIndicator(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .fillMaxWidth()
                .height(12.dp),
            progress = { (1 - (available / total.toFloat())) },
        )

        Text(
            text = stringResource(R.string.available_disk_space_info, availableText, totalText),
            modifier = Modifier.alpha(0.78f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}