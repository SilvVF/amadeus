package io.silv.explore

import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.silv.explore.composables.DisplayOptionsBottomSheet
import io.silv.explore.composables.ExploreTopAppBar
import io.silv.explore.composables.FiltersBottomSheet
import io.silv.navigation.SharedScreen
import io.silv.navigation.push
import io.silv.ui.GlobalSearchTab
import io.silv.ui.LocalAppState
import io.silv.ui.ReselectTab
import io.silv.ui.layout.ExpandableInfoLayout
import io.silv.ui.layout.PullRefresh
import io.silv.ui.layout.rememberExpandableState
import io.silv.ui.openOnWeb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ExploreTab : ReselectTab, GlobalSearchTab {
    private fun readResolve(): Any = ExploreTab

    private val reselectChannel = Channel<Unit>()

    private val searchChannel = Channel<String?>(capacity = 1)

    private var savedStatePagedType: UiPagedType? = null

    override suspend fun onSearch(query: String?, navigator: TabNavigator) {
        searchChannel.trySend(query)
        navigator.current = this
    }

    override suspend fun onReselect(navigator: Navigator) {
        Log.d("Explore", "Sending reselect event")
        reselectChannel.send(Unit)
    }

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 0u,
                title = "Home",
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    @OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val appState = LocalAppState.current
        val tabNavigator = LocalTabNavigator.current
        val screenModel = rememberScreenModel { ExploreScreenModel(savedStatePagedType) }

        val lifecycleOwner = LocalLifecycleOwner.current

        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                withContext(Dispatchers.Main.immediate) {
                    searchChannel.receiveAsFlow()
                        .collect {
                            tabNavigator.current = this@ExploreTab
                            if (it != null) {
                                screenModel.onSearch(it)
                            }
                        }
                }
            }
        }

        val pagingFlowFlow by screenModel.mangaPagingFlow.collectAsStateWithLifecycle()
        val state by screenModel.state.collectAsStateWithLifecycle()
        val isOffline by appState.isOffline.collectAsStateWithLifecycle()

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
            state = rememberTopAppBarState()
        )

        val expandableState = rememberExpandableState()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(reselectChannel, lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                withContext(Dispatchers.Main.immediate) {
                    reselectChannel.receiveAsFlow().collectLatest {
                        expandableState.toggle()
                    }
                }
            }
        }

        var showDisplayOptionsBottomSheet by rememberSaveable {
            mutableStateOf(false)
        }

        var showFiltersBottomSheet by rememberSaveable {
            mutableStateOf(false)
        }
        val changePageType = screenModel::changePagingType
        when {
            showDisplayOptionsBottomSheet ->
                DisplayOptionsBottomSheet(
                    optionsTitle = {
                        Text("Explore display options")
                    },
                    onDismissRequest = { showDisplayOptionsBottomSheet = false },
                    clearSearchHistory = screenModel::clearSearchHistory,
                )
            showFiltersBottomSheet ->
                FiltersBottomSheet(
                    onSaveQuery = {
                        changePageType(UiPagedType.Query(it))
                    },
                ) {
                    showFiltersBottomSheet = !showFiltersBottomSheet
                }
        }

        val snackbarHostState = remember{ SnackbarHostState() }

        LaunchedEffect(Unit) {
            snapshotFlow { state.pagedType }.collect {
                savedStatePagedType = state.pagedType
            }
        }

        LaunchedEffect(isOffline) {
            if (isOffline) {
                snackbarHostState.showSnackbar(
                    message = "No network connection",
                    duration = SnackbarDuration.Indefinite,
                )
            } else {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                val scope = rememberCoroutineScope()
                val context = LocalContext.current
                ExploreTopAppBar(
                    selected = state.pagedType,
                    scrollBehavior = scrollBehavior,
                    onWebClick = {
                        context.openOnWeb("https://mangadex.org", "View manga using.")
                            .onFailure {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Couldn't open url.")
                                }
                            }
                    },
                    onDisplayOptionsClick = {
                        scope.launch {
                            if (!expandableState.isHidden) {
                                expandableState.hide()
                            } else {
                                expandableState.expand()
                            }
                        }
                    },
                    onSearch = screenModel::onSearch,
                    onPageTypeSelected = screenModel::changePagingType,
                    onFilterClick = {
                        showFiltersBottomSheet = !showFiltersBottomSheet
                    },
                )
            },
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .fillMaxSize(),
        ) { paddingValues ->

            val pagingItems = pagingFlowFlow.collectAsLazyPagingItems()

            Box(Modifier.fillMaxSize()) {
                PullRefresh(
                    paddingValues = paddingValues,
                    refreshing = false, // refresh indicator handled by BrowseMangaContent
                    onRefresh =
                    when (state.pagedType) {
                        UiPagedType.Seasonal -> screenModel::refreshSeasonalManga
                        else -> pagingItems::refresh
                    },
                ) {
                    BrowseMangaContent(
                        modifier = Modifier.fillMaxSize(),
                        contentPaddingValues = paddingValues,
                        seasonalLists = state.seasonalLists,
                        refreshingSeasonal = state.refreshingSeasonal,
                        mangaList = pagingItems,
                        onBookmarkClick = screenModel::bookmarkManga,
                        onMangaClick = {
                            navigator.push(
                                SharedScreen.MangaView(it.id),
                            )
                        },
                        onTagClick = { name, id ->
                            navigator.push(
                                SharedScreen.MangaFilter(name, id),
                            )
                        },
                        pagedType = state.pagedType,
                    )
                }
                ExpandableInfoLayout(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    state = expandableState,
                    peekContent = {
                        RecentSearchesPeekContent(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            recentSearchUiState = state.recentSearchUiState,
                            query = state.filters?.title,
                            onRecentSearchClick = screenModel::onSearch,
                        )
                    },
                ) {
                    ExpandableInfoLayoutContent(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        showGroupingOptions = {
                            showFiltersBottomSheet = !showFiltersBottomSheet
                        },
                        showDisplayOptions = {
                            showDisplayOptionsBottomSheet = !showDisplayOptionsBottomSheet
                        },
                    )
                }
            }
        }
    }
}
