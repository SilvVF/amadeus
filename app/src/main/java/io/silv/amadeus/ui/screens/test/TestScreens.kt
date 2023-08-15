//package io.silv.amadeus.ui.screens.test
//
//import android.widget.Toast
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.material3.Button
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.DisposableEffect
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.paging.ExperimentalPagingApi
//import androidx.paging.LoadState
//import androidx.paging.LoadType
//import androidx.paging.Pager
//import androidx.paging.PagingConfig
//import androidx.paging.PagingSource
//import androidx.paging.PagingState
//import androidx.paging.RemoteMediator
//import androidx.paging.cachedIn
//import androidx.paging.compose.LazyPagingItems
//import androidx.paging.compose.collectAsLazyPagingItems
//import androidx.paging.compose.itemKey
//import androidx.paging.map
//import androidx.room.Dao
//import androidx.room.Database
//import androidx.room.Entity
//import androidx.room.Insert
//import androidx.room.OnConflictStrategy
//import androidx.room.PrimaryKey
//import androidx.room.Query
//import androidx.room.Room
//import androidx.room.RoomDatabase
//import androidx.room.TypeConverters
//import androidx.room.withTransaction
//import cafe.adriel.voyager.core.model.coroutineScope
//import cafe.adriel.voyager.core.screen.Screen
//import cafe.adriel.voyager.koin.getScreenModel
//import io.silv.amadeus.LocalBottomBarVisibility
//import io.silv.amadeus.ui.composables.MangaListItem
//import io.silv.amadeus.ui.shared.AmadeusScreenModel
//import io.silv.ktor_response_mapper.getOrThrow
//import io.silv.manga.domain.alternateTitles
//import io.silv.manga.domain.artists
//import io.silv.manga.domain.authors
//import io.silv.manga.domain.coverArtUrl
//import io.silv.manga.domain.descriptionEnglish
//import io.silv.manga.domain.models.SavableManga
//import io.silv.manga.domain.parseMangaDexTimeToDateTime
//import io.silv.manga.domain.suspendRunCatching
//import io.silv.manga.domain.tagToId
//import io.silv.manga.domain.timeNow
//import io.silv.manga.domain.titleEnglish
//import io.silv.manga.local.entity.MangaResource
//import io.silv.manga.local.entity.converters.Converters
//import io.silv.manga.network.mangadex.MangaDexApi
//import io.silv.manga.network.mangadex.models.ContentRating
//import io.silv.manga.network.mangadex.models.PublicationDemographic
//import io.silv.manga.network.mangadex.models.Status
//import io.silv.manga.network.mangadex.models.manga.Manga
//import io.silv.manga.network.mangadex.requests.MangaRequest
//import kotlinx.coroutines.flow.map
//import kotlinx.datetime.LocalDateTime
//import org.koin.android.ext.koin.androidContext
//import org.koin.core.module.dsl.factoryOf
//import org.koin.dsl.module
//
//@OptIn(ExperimentalPagingApi::class)
//val testModule = module {
//
//    factoryOf(::TestSM)
//
//    single {
//        get<TestDb>().testDao()
//    }
//
//    single {
//        Pager(
//            config = PagingConfig(pageSize = 60),
//            remoteMediator = TestMediator(get(), get()),
//            pagingSourceFactory = {
//                get<TestDao>().pagingSource()
//            }
//        )
//    }
//
//    single {
//        Room.databaseBuilder(
//            androidContext(),
//            TestDb::class.java,
//            "test.db"
//        )
//            .fallbackToDestructiveMigration()
//            .build()
//    }
//}
//
//@Dao
//interface TestDao {
//
//    @Query("DELETE FROM testmangaresource")
//    suspend fun clearAll()
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun upsertAll(list: List<TestMangaResource>)
//
//    @Query("SELECT * FROM testmangaresource")
//    fun pagingSource(): PagingSource<Int, TestMangaResource>
//}
//
//@Entity
//data class TestMangaResource(
//    @PrimaryKey override val id: String,
//    override val coverArt: String,
//    override val description: String,
//    override val titleEnglish: String,
//    override val alternateTitles: Map<String, String>,
//    override val originalLanguage: String,
//    override val availableTranslatedLanguages: List<String>,
//    override val status: Status,
//    override val tagToId: Map<String, String>,
//    override val contentRating: ContentRating,
//    override val lastVolume: Int,
//    override val lastChapter: Long,
//    override val version: Int,
//    override val createdAt: LocalDateTime,
//    override val updatedAt: LocalDateTime,
//    override val publicationDemographic: PublicationDemographic?,
//    override val volumeToCoverArt: Map<String, String> = emptyMap(),
//    override val savedAtLocal: LocalDateTime = timeNow(),
//    override val year: Int,
//    override val latestUploadedChapter: String?,
//    override val authors: List<String>,
//    override val artists: List<String>,
//    val offset: Int = 0
//): MangaResource
//
//private fun Manga.toTestResource(): TestMangaResource {
//    return TestMangaResource(
//        id = id,
//        description = descriptionEnglish,
//        coverArt = coverArtUrl(this),
//        titleEnglish = titleEnglish,
//        alternateTitles = alternateTitles,
//        originalLanguage = attributes.originalLanguage,
//        availableTranslatedLanguages = attributes.availableTranslatedLanguages
//            .filterNotNull(),
//        status = attributes.status,
//        contentRating = attributes.contentRating,
//        lastVolume = attributes.lastVolume?.toIntOrNull() ?: -1,
//        lastChapter = attributes.lastChapter?.toLongOrNull() ?: -1L,
//        version = attributes.version,
//        createdAt = attributes.createdAt.parseMangaDexTimeToDateTime(),
//        updatedAt = attributes.updatedAt.parseMangaDexTimeToDateTime(),
//        tagToId = tagToId,
//        publicationDemographic = attributes.publicationDemographic,
//        latestUploadedChapter = attributes.latestUploadedChapter,
//        year = attributes.year ?: -1,
//        authors = authors,
//        artists = artists
//    )
//}
//
//
//@Database(
//    version = 1,
//    entities = [TestMangaResource::class]
//)
//@TypeConverters(Converters::class)
//abstract class TestDb: RoomDatabase() {
//
//    abstract fun testDao(): TestDao
//}
//
//
//@OptIn(ExperimentalPagingApi::class)
//class TestMediator(
//    private val mangaDexApi: MangaDexApi,
//    private val db: TestDb,
//): RemoteMediator<Int, TestMangaResource>() {
//
//    private val itemToOffset = mutableMapOf<String, Int>()
//
//    override suspend fun load(
//        loadType: LoadType,
//        state: PagingState<Int, TestMangaResource>
//    ): MediatorResult {
//        return suspendRunCatching {
//            val offset = when(loadType) {
//                LoadType.REFRESH -> 0
//                LoadType.PREPEND -> return@suspendRunCatching MediatorResult.Success(
//                    endOfPaginationReached = true
//                )
//                LoadType.APPEND -> {
//                    val lastItem = state.lastItemOrNull()
//                    if(lastItem == null) { 0 } else {
//                        lastItem.offset + state.config.pageSize
//                    }
//                }
//            }
//            val response = mangaDexApi.getMangaList(
//                MangaRequest(
//                    limit = state.config.pageSize,
//                    offset = offset,
//                    includes = listOf("cover_art", "author", "artist")
//                )
//            )
//                .getOrThrow()
//
//            val dao = db.testDao()
//            db.withTransaction {
//                if(loadType == LoadType.REFRESH) {
//                    dao.clearAll()
//                }
//                val entities = response.data.map { it.toTestResource().copy(offset = offset) }
//                entities.forEach { itemToOffset[it.id] = offset }
//                dao.upsertAll(entities)
//            }
//
//            MediatorResult.Success(
//                endOfPaginationReached = offset >= response.total
//            )
//        }.getOrElse {
//            MediatorResult.Error(it)
//        }
//    }
//}
//
//class TestSM(
//    pager: Pager<Int, TestMangaResource>
//): AmadeusScreenModel<TestEvent>() {
//
//
//
//    val mangaPagingFlow = pager
//        .flow
//        .map { pagingData ->
//            pagingData.map { SavableManga(it, null) }
//        }
//        .cachedIn(coroutineScope)
//
//}
//
//sealed interface TestEvent
//
//class TestScreen: Screen {
//
//
//    @Composable
//    override fun Content() {
//
//        val sm = getScreenModel<TestSM>()
//
//        val pagingItems = sm.mangaPagingFlow.collectAsLazyPagingItems()
//        var bottomBarVisible by LocalBottomBarVisibility.current
//
//        DisposableEffect(Unit) {
//            bottomBarVisible = false
//            onDispose { bottomBarVisible = true }
//        }
//
//        Box(modifier = Modifier.fillMaxSize()) {
//            TestMangaScreenContent(manga = pagingItems)
//        }
//    }
//}
//
//@Composable
//private fun TestMangaScreenContent(
//    manga: LazyPagingItems<SavableManga>
//) {
//    val context = LocalContext.current
//    LaunchedEffect(key1 = manga.loadState) {
//        if(manga.loadState.refresh is LoadState.Error) {
//            Toast.makeText(
//                context,
//                "Error: " + (manga.loadState.refresh as LoadState.Error).error.message,
//                Toast.LENGTH_LONG
//            ).show()
//        }
//    }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        if(manga.loadState.refresh is LoadState.Loading) {
//            CircularProgressIndicator(
//                modifier = Modifier.align(Alignment.Center)
//            )
//        } else {
//            LazyColumn(
//                modifier = Modifier.fillMaxSize(),
//                verticalArrangement = Arrangement.spacedBy(16.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                items(
//                    count = manga.itemCount,
//                    key = manga.itemKey()
//                ) { i ->
//                    manga[i]?.let {
//                        MangaListItem(
//                            manga = it,
//                            modifier = Modifier.fillMaxWidth(),
//                            onTagClick = {},
//                            onBookmarkClick = {}
//                        )
//                    }
//                }
//                item {
//                    if(manga.loadState.append is LoadState.Loading) {
//                        CircularProgressIndicator()
//                    }
//                    if (manga.loadState.append is LoadState.Error || manga.loadState.refresh is LoadState.Error) {
//                        Button(onClick = { manga.retry() }) {
//                            Text("Retry loading manga")
//                        }
//                    }
//                }
//            }
//        }
//    }
//}