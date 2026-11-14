package com.gideon.buecherwelt.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gideon.buecherwelt.di.AppContainer
import com.gideon.buecherwelt.ui.screens.add.AddBookScreen
import com.gideon.buecherwelt.ui.screens.detail.BookDetailScreen
import com.gideon.buecherwelt.ui.screens.import.ImportScreen
import com.gideon.buecherwelt.ui.screens.tropes.TropeManagerScreen
import com.gideon.buecherwelt.ui.screens.library.LibraryScreen
import com.gideon.buecherwelt.ui.screens.scanner.BarcodeScannerScreen
import com.gideon.buecherwelt.ui.screens.settings.SettingsScreen
import com.gideon.buecherwelt.ui.screens.stats.StatsScreen

sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object Discover : Screen("discover")
    data object Stats : Screen("stats")
    data object Settings : Screen("settings")
    data object BookDetail : Screen("book/{bookId}") {
        fun create(bookId: Long) = "book/$bookId"
    }
    data object AddBook : Screen("add?editBookId={editBookId}") {
        fun create(editBookId: Long? = null) =
            if (editBookId == null) "add" else "add?editBookId=$editBookId"
    }
    data object Scanner : Screen("scanner")
    data object Tropes : Screen("tropes")
    data object Import : Screen("import")
}

@Composable
fun AppNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    val scannedIsbn = remember { mutableStateOf<String?>(null) }
    val mainRoutes = listOf(
        Screen.Library.route,
        Screen.Stats.route,
        Screen.Settings.route
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun navigateToMain(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationRoute ?: Screen.Library.route) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in mainRoutes) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Library.route,
                        onClick = { navigateToMain(Screen.Library.route) },
                        icon = { Icon(Icons.Default.Book, contentDescription = null) },
                        label = { Text("Bibliothek") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Stats.route,
                        onClick = { navigateToMain(Screen.Stats.route) },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                        label = { Text("Statistik") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Settings.route,
                        onClick = { navigateToMain(Screen.Settings.route) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Einstellungen") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Library.route) {
                LibraryScreen(container = container, navController = navController)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(container = container, navController = navController)
            }
            composable(Screen.Stats.route) {
                StatsScreen(container = container)
            }
            composable(Screen.Tropes.route) {
                TropeManagerScreen(container = container, navController = navController)
            }
            composable(Screen.Import.route) {
                ImportScreen(container = container, navController = navController)
            }
            composable(
                route = Screen.BookDetail.route,
                arguments = listOf(navArgument("bookId") { type = NavType.LongType })
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
                BookDetailScreen(
                    container = container,
                    navController = navController,
                    bookId = bookId
                )
            }
            composable(
                route = Screen.AddBook.route,
                arguments = listOf(navArgument("editBookId") {
                    type = NavType.LongType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val editBookId = backStackEntry.arguments?.getLong("editBookId")
                AddBookScreen(
                    container = container,
                    navController = navController,
                    editBookId = editBookId,
                    scannedIsbn = scannedIsbn
                )
            }
            composable(Screen.Scanner.route) {
                BarcodeScannerScreen(
                    onIsbn = { isbn ->
                        scannedIsbn.value = isbn
                        navController.popBackStack()
                    },
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}
