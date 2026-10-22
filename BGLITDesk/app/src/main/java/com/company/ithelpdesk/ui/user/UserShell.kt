package com.company.ithelpdesk.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.company.ithelpdesk.ui.components.CountBubble
import com.company.ithelpdesk.ui.shared.NotificationsScreen
import com.company.ithelpdesk.ui.shared.NotificationsViewModel
import com.company.ithelpdesk.ui.theme.LocalBrand

object UserRoutes {
    const val HOME = "home"
    const val TICKETS = "tickets"
    const val ALERTS = "alerts"
    const val ME = "me"
    const val NEW = "new?category={category}"
    const val TICKET = "ticket/{ticketId}"
    fun newTicket(category: String? = null) = "new?category=${category.orEmpty()}"
    fun ticket(id: String) = "ticket/$id"
    val tabs = setOf(HOME, TICKETS, ALERTS, ME)
}

/** The user app: its own navigation, bottom bar and look. Admins never see this. */
@Composable
fun UserShell(pendingTicketId: String?, onPendingTicketHandled: () -> Unit) {
    val nav = rememberNavController()
    val alertsVm: NotificationsViewModel = hiltViewModel()
    val alerts by alertsVm.ui.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    LaunchedEffect(pendingTicketId) {
        if (pendingTicketId != null) {
            nav.navigate(UserRoutes.ticket(pendingTicketId))
            onPendingTicketHandled()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (route in UserRoutes.tabs) {
                UserBottomBar(route, alerts.unread, onTab = { nav.switchTab(it) }, onNew = { nav.navigate(UserRoutes.newTicket()) })
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = UserRoutes.HOME, modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            composable(UserRoutes.HOME) {
                UserHomeScreen(
                    onNewTicket = { nav.navigate(UserRoutes.newTicket(it)) },
                    onOpenTicket = { nav.navigate(UserRoutes.ticket(it)) },
                    onSeeAll = { nav.switchTab(UserRoutes.TICKETS) },
                    onAlerts = { nav.switchTab(UserRoutes.ALERTS) },
                    unread = alerts.unread,
                )
            }
            composable(UserRoutes.TICKETS) {
                UserTicketsScreen(onOpenTicket = { nav.navigate(UserRoutes.ticket(it)) }, onNewTicket = { nav.navigate(UserRoutes.newTicket()) })
            }
            composable(UserRoutes.ALERTS) {
                NotificationsScreen(alertsVm, onOpenTicket = { nav.navigate(UserRoutes.ticket(it)) })
            }
            composable(UserRoutes.ME) { UserProfileScreen() }
            composable(
                UserRoutes.NEW,
                arguments = listOf(navArgument("category") { type = NavType.StringType; defaultValue = ""; nullable = true }),
            ) {
                NewTicketScreen(
                    onBack = { nav.popBackStack() },
                    onCreated = { id ->
                        nav.popBackStack()
                        nav.navigate(UserRoutes.ticket(id))
                    },
                )
            }
            composable(UserRoutes.TICKET, arguments = listOf(navArgument("ticketId") { type = NavType.StringType })) {
                UserTicketScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun UserBottomBar(route: String?, unread: Int, onTab: (String) -> Unit, onNew: () -> Unit) {
    val brand = LocalBrand.current
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(16.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TabItem("Home", Icons.Filled.Home, Icons.Outlined.Home, route == UserRoutes.HOME) { onTab(UserRoutes.HOME) }
            TabItem("Tickets", Icons.Filled.ConfirmationNumber, Icons.Outlined.ConfirmationNumber, route == UserRoutes.TICKETS) { onTab(UserRoutes.TICKETS) }
            Box(Modifier.size(56.dp)) // space for the centre button
            TabItem("Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications, route == UserRoutes.ALERTS, badge = unread) { onTab(UserRoutes.ALERTS) }
            TabItem("Me", Icons.Filled.Person, Icons.Outlined.Person, route == UserRoutes.ME) { onTab(UserRoutes.ME) }
        }
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(y = (-14).dp)
                .size(62.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(brand.heroBrush)
                .clickable(onClick = onNew)
                .semantics { contentDescription = "New ticket" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun TabItem(label: String, selectedIcon: ImageVector, icon: ImageVector, selected: Boolean, badge: Int = 0, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Icon(if (selected) selectedIcon else icon, contentDescription = null, tint = color)
            if (badge > 0) CountBubble(badge, Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-6).dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
