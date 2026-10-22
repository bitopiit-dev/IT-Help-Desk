package com.company.ithelpdesk.ui.admin

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.company.ithelpdesk.ui.shared.NotificationsScreen
import com.company.ithelpdesk.ui.shared.NotificationsViewModel
import com.company.ithelpdesk.ui.shared.ProfileContent

object AdminRoutes {
    const val OVERVIEW = "a/overview"
    const val TICKETS = "a/tickets?assignee={assignee}"
    const val TICKETS_BASE = "a/tickets"
    const val PEOPLE = "a/people"
    const val ALERTS = "a/alerts"
    const val MORE = "a/more"
    const val TICKET = "a/ticket/{ticketId}"
    const val CATEGORIES = "a/categories"
    const val DEPARTMENTS = "a/departments"
    const val SLA = "a/sla"
    const val PROFILE = "a/profile"
    fun ticket(id: String) = "a/ticket/$id"
    fun tickets(assignee: String? = null) = "a/tickets?assignee=${assignee.orEmpty()}"
    val tabs = setOf(OVERVIEW, TICKETS, PEOPLE, ALERTS, MORE)
}

private data class AdminTab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val adminTabs = listOf(
    AdminTab(AdminRoutes.OVERVIEW, "Overview", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
    AdminTab(AdminRoutes.TICKETS, "Tickets", Icons.Outlined.Inbox, Icons.Filled.Inbox),
    AdminTab(AdminRoutes.PEOPLE, "People", Icons.Outlined.Groups, Icons.Filled.Groups),
    AdminTab(AdminRoutes.ALERTS, "Alerts", Icons.Outlined.Notifications, Icons.Filled.Notifications),
    AdminTab(AdminRoutes.MORE, "Setup", Icons.Outlined.Tune, Icons.Filled.Tune),
)

/** The admin app: separate navigation, neon dark theme, approvals and the full ticket queue. */
@Composable
fun AdminShell(pendingTicketId: String?, onPendingTicketHandled: () -> Unit) {
    val nav = rememberNavController()
    val alertsVm: NotificationsViewModel = hiltViewModel()
    val peopleVm: PeopleViewModel = hiltViewModel()
    val alerts by alertsVm.ui.collectAsStateWithLifecycle()
    val people by peopleVm.ui.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    LaunchedEffect(pendingTicketId) {
        if (pendingTicketId != null) {
            nav.navigate(AdminRoutes.ticket(pendingTicketId))
            onPendingTicketHandled()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (route in AdminRoutes.tabs) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    adminTabs.forEach { tab ->
                        val selected = route == tab.route
                        val badge = when (tab.route) {
                            AdminRoutes.PEOPLE -> people.pendingCount
                            AdminRoutes.ALERTS -> alerts.unread
                            else -> 0
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.switchTab(if (tab.route == AdminRoutes.TICKETS) AdminRoutes.tickets() else tab.route) },
                            icon = {
                                BadgedBox(badge = { if (badge > 0) Badge { Text(if (badge > 99) "99+" else "$badge") } }) {
                                    Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null)
                                }
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = AdminRoutes.OVERVIEW, modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            composable(AdminRoutes.OVERVIEW) {
                AdminOverviewScreen(
                    pendingSignups = people.pendingCount,
                    onOpenTicket = { nav.navigate(AdminRoutes.ticket(it)) },
                    onOpenPeople = { nav.switchTab(AdminRoutes.PEOPLE) },
                    onOpenQueue = { nav.switchTab(AdminRoutes.tickets()) },
                    onOpenAssignee = { nav.navigate(AdminRoutes.tickets(it)) },
                )
            }
            composable(
                AdminRoutes.TICKETS,
                arguments = listOf(navArgument("assignee") { type = NavType.StringType; defaultValue = ""; nullable = true }),
            ) { entry ->
                AdminTicketsScreen(
                    assignee = entry.arguments?.getString("assignee")?.takeIf { it.isNotBlank() },
                    onOpenTicket = { nav.navigate(AdminRoutes.ticket(it)) },
                )
            }
            composable(AdminRoutes.PEOPLE) { PeopleScreen(peopleVm) }
            composable(AdminRoutes.ALERTS) {
                NotificationsScreen(
                    alertsVm,
                    onOpenTicket = { nav.navigate(AdminRoutes.ticket(it)) },
                    onOpenSignups = { nav.switchTab(AdminRoutes.PEOPLE) },
                )
            }
            composable(AdminRoutes.MORE) {
                AdminMoreScreen(
                    onCategories = { nav.navigate(AdminRoutes.CATEGORIES) },
                    onDepartments = { nav.navigate(AdminRoutes.DEPARTMENTS) },
                    onSla = { nav.navigate(AdminRoutes.SLA) },
                    onProfile = { nav.navigate(AdminRoutes.PROFILE) },
                )
            }
            composable(AdminRoutes.TICKET, arguments = listOf(navArgument("ticketId") { type = NavType.StringType })) {
                AdminTicketScreen(onBack = { nav.popBackStack() })
            }
            composable(AdminRoutes.CATEGORIES) { CategoriesScreen(onBack = { nav.popBackStack() }) }
            composable(AdminRoutes.DEPARTMENTS) { DepartmentsScreen(onBack = { nav.popBackStack() }) }
            composable(AdminRoutes.SLA) { SlaScreen(onBack = { nav.popBackStack() }) }
            composable(AdminRoutes.PROFILE) { ProfileContent(hiltViewModel()) }
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
