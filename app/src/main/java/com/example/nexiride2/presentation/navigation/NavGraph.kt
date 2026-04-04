package com.example.nexiride2.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.nexiride2.presentation.auth.*
import com.example.nexiride2.presentation.booking.*
import com.example.nexiride2.presentation.home.*
import com.example.nexiride2.presentation.mybookings.*
import com.example.nexiride2.presentation.notifications.*
import com.example.nexiride2.presentation.onboarding.*
import com.example.nexiride2.presentation.profile.*
import com.example.nexiride2.presentation.search.*
import com.example.nexiride2.presentation.tracking.LiveTrackingScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object ForgotPassword : Screen("forgot_password")
    object Home : Screen("home")
    object Search : Screen("search")
    object SearchResults : Screen("search_results")
    object BusDetail : Screen("bus_detail/{routeId}") { fun createRoute(routeId: String) = "bus_detail/$routeId" }
    object SeatSelection : Screen("seat_selection")
    object PassengerDetails : Screen("passenger_details")
    object Review : Screen("review")
    object MyBookings : Screen("my_bookings")
    object TicketDetail : Screen("ticket_detail/{bookingId}") { fun createRoute(bookingId: String) = "ticket_detail/$bookingId" }
    object LiveTracking : Screen("live_tracking/{bookingId}") { fun createRoute(bookingId: String) = "live_tracking/$bookingId" }
    object Notifications : Screen("notifications")
    object Profile : Screen("profile")
}

data class BottomNavItem(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Search.route, "Search", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem(Screen.MyBookings.route, "Bookings", Icons.Filled.ConfirmationNumber, Icons.Outlined.ConfirmationNumber),
    BottomNavItem(Screen.Notifications.route, "Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    BottomNavItem(Screen.Profile.route, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

private fun NavHostController.safeNavigate(
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    runCatching { navigate(route, builder) }
}

@Composable
fun NexiRideNavHost(navController: NavHostController = rememberNavController()) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val searchViewModel: SearchViewModel = hiltViewModel()
    val bookingViewModel: BookingViewModel = hiltViewModel()
    val myBookingsViewModel: MyBookingsViewModel = hiltViewModel()
    val notificationsViewModel: NotificationsViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            },
                            icon = { Icon(if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon, item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Splash.route, Modifier.padding(innerPadding)) {
            composable(Screen.Splash.route) {
                val isLoggedIn = authViewModel.uiState.collectAsState().value.isLoggedIn
                SplashScreen {
                    if (isLoggedIn) {
                        navController.safeNavigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.safeNavigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onGetStarted = {
                        navController.safeNavigate(Screen.SignUp.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onLogin = { navController.safeNavigate(Screen.Login.route) { launchSingleTop = true } }
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(authViewModel,
                    onNavigateToSignUp = { navController.safeNavigate(Screen.SignUp.route) { launchSingleTop = true } },
                    onNavigateToForgotPassword = { navController.safeNavigate(Screen.ForgotPassword.route) { launchSingleTop = true } },
                    onLoginSuccess = {
                        navController.safeNavigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.SignUp.route) {
                SignUpScreen(authViewModel,
                    onNavigateToLogin = { navController.safeNavigate(Screen.Login.route) { launchSingleTop = true } },
                    onSignUpSuccess = {
                        navController.safeNavigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(authViewModel, onBack = { navController.popBackStack() },
                    onResetSuccess = {
                        navController.safeNavigate(Screen.Login.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    })
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onSearchClick = { from, to -> searchViewModel.searchWithParams(from, to); navController.navigate(Screen.SearchResults.route) },
                    onRouteClick = { routeId -> searchViewModel.selectRoute(routeId); navController.navigate(Screen.BusDetail.createRoute(routeId)) },
                    onProfileClick = { navController.navigate(Screen.Profile.route) { launchSingleTop = true } }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(searchViewModel, onSearchResults = { navController.navigate(Screen.SearchResults.route) })
            }
            composable(Screen.SearchResults.route) {
                SearchResultsScreen(searchViewModel, onBack = { navController.popBackStack() },
                    onRouteSelected = { routeId -> navController.navigate(Screen.BusDetail.createRoute(routeId)) })
            }
            composable(Screen.BusDetail.route) { backStackEntry ->
                val routeId = backStackEntry.arguments?.getString("routeId") ?: ""
                BusDetailScreen(searchViewModel, routeId, onBack = { navController.popBackStack() },
                    onSelectBus = { bookingViewModel.setRouteId(routeId); navController.navigate(Screen.SeatSelection.route) })
            }
            composable(Screen.SeatSelection.route) {
                SeatSelectionScreen(searchViewModel.uiState.collectAsState().value.seats, bookingViewModel,
                    onBack = { navController.popBackStack() }, onContinue = { navController.navigate(Screen.PassengerDetails.route) })
            }
            composable(Screen.PassengerDetails.route) {
                PassengerDetailsScreen(bookingViewModel, onBack = { navController.popBackStack() },
                    onContinue = { navController.navigate(Screen.Review.route) })
            }
            composable(Screen.Review.route) {
                ReviewScreen(searchViewModel.uiState.collectAsState().value.selectedRoute, bookingViewModel,
                    onBack = { navController.popBackStack() },
                    onPaymentSuccess = { navController.navigate(Screen.MyBookings.route) { popUpTo(Screen.Home.route) } })
            }
            composable(Screen.MyBookings.route) {
                MyBookingsScreen(myBookingsViewModel) { bookingId ->
                    navController.navigate(Screen.TicketDetail.createRoute(bookingId))
                }
            }
            composable(Screen.TicketDetail.route) {
                TicketDetailScreen(
                    viewModel = myBookingsViewModel,
                    onBack = { navController.popBackStack() },
                    onLiveTracking = { bookingId -> navController.navigate(Screen.LiveTracking.createRoute(bookingId)) }
                )
            }
            composable(Screen.LiveTracking.route) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                val booking = myBookingsViewModel.uiState.collectAsState().value.selectedBooking
                LiveTrackingScreen(
                    title = booking?.let { "${it.route.origin} → ${it.route.destination}" } ?: "Live Tracking",
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Notifications.route) { NotificationsScreen(notificationsViewModel) }
            composable(Screen.Profile.route) {
                ProfileScreen(profileViewModel, onLogout = {
                    authViewModel.logout()
                    navController.safeNavigate(Screen.Login.route) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                })
            }
        }
    }
}
