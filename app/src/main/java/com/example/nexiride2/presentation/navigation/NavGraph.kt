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
import com.example.nexiride2.presentation.admin.AdminScreen
import com.example.nexiride2.presentation.auth.*
import com.example.nexiride2.presentation.booking.*
import com.example.nexiride2.presentation.driver.DriverAuthViewModel
import com.example.nexiride2.presentation.driver.DriverHomeScreen
import com.example.nexiride2.presentation.driver.DriverHomeViewModel
import com.example.nexiride2.presentation.driver.DriverPortalScreen
import com.example.nexiride2.presentation.home.*
import com.example.nexiride2.presentation.mybookings.*
import com.example.nexiride2.presentation.notifications.*
import com.example.nexiride2.presentation.onboarding.*
import com.example.nexiride2.presentation.profile.*
import com.example.nexiride2.presentation.search.*
import com.example.nexiride2.presentation.tracking.LiveTrackingScreen
import com.example.nexiride2.presentation.tracking.LiveTrackingViewModel

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
    object Admin : Screen("admin")
    object DriverPortal : Screen("driver_portal")
    object DriverHome : Screen("driver_home")
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
                val authState = authViewModel.uiState.collectAsState().value
                SplashScreen {
                    val dest = when {
                        authState.isLoggedIn && authState.isDriver -> Screen.DriverHome.route
                        authState.isLoggedIn -> Screen.Home.route
                        else -> Screen.Onboarding.route
                    }
                    navController.safeNavigate(dest) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                        launchSingleTop = true
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
                    onLogin = { navController.safeNavigate(Screen.Login.route) { launchSingleTop = true } },
                    onDriverPortal = {
                        navController.safeNavigate(Screen.DriverPortal.route) { launchSingleTop = true }
                    }
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(authViewModel,
                    onNavigateToSignUp = { navController.safeNavigate(Screen.SignUp.route) { launchSingleTop = true } },
                    onNavigateToForgotPassword = { navController.safeNavigate(Screen.ForgotPassword.route) { launchSingleTop = true } },
                    onNavigateToDriverPortal = {
                        navController.safeNavigate(Screen.DriverPortal.route) { launchSingleTop = true }
                    },
                    onLoginSuccess = {
                        // If the account is actually a driver, redirect to the driver
                        // portal so they don't land in the passenger home by mistake.
                        val target = if (authViewModel.uiState.value.isDriver) Screen.DriverHome.route
                        else Screen.Home.route
                        navController.safeNavigate(target) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.SignUp.route) {
                SignUpScreen(authViewModel,
                    onNavigateToLogin = { navController.safeNavigate(Screen.Login.route) { launchSingleTop = true } },
                    onNavigateToDriverPortal = {
                        navController.safeNavigate(Screen.DriverPortal.route) { launchSingleTop = true }
                    },
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
                val bottomNavOpts: NavOptionsBuilder.() -> Unit = {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                HomeScreen(
                    onOpenSearch = { navController.navigate(Screen.Search.route, bottomNavOpts) },
                    onOpenBookings = { navController.navigate(Screen.MyBookings.route, bottomNavOpts) },
                    onPopularCityClick = { city ->
                        searchViewModel.updateDestination(city)
                        navController.navigate(Screen.Search.route, bottomNavOpts)
                    },
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
                    onPaymentSuccess = {
                        myBookingsViewModel.loadBookings()
                        navController.navigate(Screen.MyBookings.route) { popUpTo(Screen.Home.route) }
                    })
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
            composable(Screen.LiveTracking.route) {
                val liveTrackingViewModel: LiveTrackingViewModel = hiltViewModel()
                val booking = myBookingsViewModel.uiState.collectAsState().value.selectedBooking
                LiveTrackingScreen(
                    title = booking?.let { "${it.route.origin} → ${it.route.destination}" } ?: "Live Tracking",
                    onBack = { navController.popBackStack() },
                    viewModel = liveTrackingViewModel,
                    origin = booking?.route?.origin.orEmpty(),
                    destination = booking?.route?.destination.orEmpty()
                )
            }
            composable(Screen.Notifications.route) { NotificationsScreen(notificationsViewModel) }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onLogout = {
                        authViewModel.logout()
                        navController.safeNavigate(Screen.Login.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToMyBookings = {
                        navController.safeNavigate(Screen.MyBookings.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToNotifications = {
                        navController.safeNavigate(Screen.Notifications.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAdmin = {
                        navController.safeNavigate(Screen.Admin.route) { launchSingleTop = true }
                    },
                    onNavigateToDriverPortal = {
                        navController.safeNavigate(Screen.DriverPortal.route) { launchSingleTop = true }
                    }
                )
            }
            composable(Screen.Admin.route) {
                AdminScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.DriverPortal.route) {
                val driverAuthVm: DriverAuthViewModel = hiltViewModel()
                // If the currently signed-in account is already a registered
                // driver, skip the sign-in form entirely and take them straight
                // to the driver home — no need to re-enter credentials.
                val authState = authViewModel.uiState.collectAsState().value
                LaunchedEffect(authState.isLoggedIn, authState.isDriver) {
                    if (authState.isLoggedIn && authState.isDriver) {
                        navController.safeNavigate(Screen.DriverHome.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                DriverPortalScreen(
                    viewModel = driverAuthVm,
                    onBack = {
                        // Per request: exiting the portal always lands on the
                        // passenger sign-in page instead of whatever screen
                        // happened to be behind it on the back stack.
                        navController.safeNavigate(Screen.Login.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onAuthenticated = {
                        navController.safeNavigate(Screen.DriverHome.route) {
                            // Clear the portal + onboarding/login stack so logout
                            // bounces correctly.
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.DriverHome.route) {
                val driverHomeVm: DriverHomeViewModel = hiltViewModel()
                DriverHomeScreen(
                    viewModel = driverHomeVm,
                    onSignedOut = {
                        navController.safeNavigate(Screen.Onboarding.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
