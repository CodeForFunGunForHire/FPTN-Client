package com.filantrop.pvnclient.ui

//
// import androidx.compose.runtime.Composable
// import androidx.compose.runtime.Stable
// import androidx.compose.runtime.collectAsState
// import androidx.compose.runtime.mutableStateOf
// import androidx.compose.runtime.remember
// import androidx.compose.runtime.rememberCoroutineScope
// import androidx.navigation.NavDestination
// import androidx.navigation.NavHostController
// import androidx.navigation.compose.rememberNavController
// import com.filantrop.pvnclient.core.network.NetworkMonitor
// import kotlinx.coroutines.CoroutineScope
// import kotlinx.coroutines.flow.SharingStarted
// import kotlinx.coroutines.flow.map
// import kotlinx.coroutines.flow.stateIn
//
object PvnAppState

// @Composable
// fun rememberPvnAppState(
//    networkMonitor: NetworkMonitor,
//    coroutineScope: CoroutineScope = rememberCoroutineScope(),
//    navController: NavHostController = rememberNavController(),
// ): PvnAppState {
//    return remember(
//        navController,
//        coroutineScope,
//        networkMonitor,
//    ) {
//        PvnAppState(
//            navController = navController,
//            coroutineScope = coroutineScope,
//            networkMonitor = networkMonitor,
//        )
//    }
// }
//
//
// @Stable
// class PvnAppState(
//    val navController: NavHostController,
//    coroutineScope: CoroutineScope,
//    networkMonitor: NetworkMonitor,
// ) {
//    private val previousDestination = mutableStateOf<NavDestination?>(null)
//
//    val currentDestination: NavDestination?
//        @Composable get() {
//            // Collect the currentBackStackEntryFlow as a state
//            val currentEntry = navController.currentBackStackEntryFlow
//                .collectAsState(initial = null)
//
//            // Fallback to previousDestination if currentEntry is null
//            return currentEntry.value?.destination.also { destination ->
//                if (destination != null) {
//                    previousDestination.value = destination
//                }
//            } ?: previousDestination.value
//        }
//
//    val currentTopLevelDestination: TopLevelDestination?
//        @Composable get() {
//            return TopLevelDestination.entries.firstOrNull { topLevelDestination ->
//                currentDestination?.hasRoute(route = topLevelDestination.route) == true
//            }
//        }
//
//    val isOffline = networkMonitor.isOnline
//        .map(Boolean::not)
//        .stateIn(
//            scope = coroutineScope,
//            started = SharingStarted.WhileSubscribed(5_000),
//            initialValue = false,
//        )
//
//    /**
//     * Map of top level destinations to be used in the TopBar, BottomBar and NavRail. The key is the
//     * route.
//     */
//    val topLevelDestinations: List<TopLevelDestination> = TopLevelDestination.entries
//
//    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestination) {
//        trace("Navigation: ${topLevelDestination.name}") {
//            val topLevelNavOptions = navOptions {
//                // Pop up to the start destination of the graph to
//                // avoid building up a large stack of destinations
//                // on the back stack as users select items
//                popUpTo(navController.graph.findStartDestination().id) {
//                    saveState = true
//                }
//                // Avoid multiple copies of the same destination when
//                // reselecting the same item
//                launchSingleTop = true
//                // Restore state when reselecting a previously selected item
//                restoreState = true
//            }
//
//            when (topLevelDestination) {
//                FOR_YOU -> navController.navigateToForYou(topLevelNavOptions)
//                BOOKMARKS -> navController.navigateToBookmarks(topLevelNavOptions)
//                INTERESTS -> navController.navigateToInterests(null, topLevelNavOptions)
//            }
//        }
//    }
//
//    fun navigateToSearch() = navController.navigateToSearch()
// }
