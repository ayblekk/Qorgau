package kz.qorgau.scamguardian.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kz.qorgau.scamguardian.R
import kz.qorgau.scamguardian.ui.AppViewModelFactory
import kz.qorgau.scamguardian.ui.check.CheckScreen
import kz.qorgau.scamguardian.ui.check.CheckViewModel
import kz.qorgau.scamguardian.ui.history.HistoryScreen
import kz.qorgau.scamguardian.ui.history.HistoryViewModel
import kz.qorgau.scamguardian.ui.permissions.PermissionBanner
import kz.qorgau.scamguardian.ui.permissions.PermissionOnboardingScreen
import kz.qorgau.scamguardian.ui.permissions.PermissionPrefs
import kz.qorgau.scamguardian.ui.permissions.PermissionStatus
import kz.qorgau.scamguardian.ui.settings.SettingsScreen
import kz.qorgau.scamguardian.ui.settings.SettingsViewModel

private enum class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    History("history", R.string.nav_history, Icons.Outlined.History),
    Check("check", R.string.nav_check, Icons.Outlined.Search),
    Settings("settings", R.string.nav_settings, Icons.Outlined.Settings),
}

@Composable
fun ScamGuardianApp(
    viewModelFactory: AppViewModelFactory,
    startDestination: String = TopLevelDestination.History.route,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionPrefs = remember { PermissionPrefs(context) }

    var permissionSnapshot by remember {
        mutableStateOf(PermissionStatus.snapshot(context))
    }
    var showOnboarding by remember {
        mutableStateOf(
            !permissionPrefs.onboardingCompleted ||
                !PermissionStatus.snapshot(context).allCriticalGranted,
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val snap = PermissionStatus.snapshot(context)
                permissionSnapshot = snap
                if (!snap.allCriticalGranted && permissionPrefs.onboardingCompleted) {
                    // Critical permission revoked → force onboarding again.
                    showOnboarding = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showOnboarding) {
        PermissionOnboardingScreen(
            onAllReady = {
                permissionPrefs.onboardingCompleted = true
                permissionSnapshot = PermissionStatus.snapshot(context)
                showOnboarding = false
            },
            onContinueAnyway = {
                permissionPrefs.onboardingCompleted = true
                permissionSnapshot = PermissionStatus.snapshot(context)
                showOnboarding = false
            },
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        topBar = {
            PermissionBanner(
                snapshot = permissionSnapshot,
                onClick = { showOnboarding = true },
            )
        },
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { dest ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = stringResource(dest.labelRes),
                            )
                        },
                        label = { Text(stringResource(dest.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
            ) {
                composable(TopLevelDestination.History.route) {
                    val vm: HistoryViewModel = viewModel(factory = viewModelFactory)
                    HistoryScreen(viewModel = vm)
                }
                composable(TopLevelDestination.Check.route) {
                    val vm: CheckViewModel = viewModel(factory = viewModelFactory)
                    CheckScreen(viewModel = vm)
                }
                composable(TopLevelDestination.Settings.route) {
                    val vm: SettingsViewModel = viewModel(factory = viewModelFactory)
                    SettingsScreen(viewModel = vm)
                }
            }
        }
    }
}
