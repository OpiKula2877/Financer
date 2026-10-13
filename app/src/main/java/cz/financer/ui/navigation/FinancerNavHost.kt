package cz.financer.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cz.financer.data.TransactionType
import cz.financer.ui.accounting.AccountingScreen
import cz.financer.ui.clients.ClientDetailScreen
import cz.financer.ui.clients.ClientsScreen
import cz.financer.ui.products.ProductEditScreen
import cz.financer.ui.products.ProductsScreen
import cz.financer.ui.projects.ProjectsScreen
import cz.financer.ui.sales.NewSaleScreen
import cz.financer.ui.sales.SalesScreen
import cz.financer.ui.settings.ProjectSettingsScreen

private data class ProjectTab(
    val label: String,
    val icon: ImageVector,
    val route: (Long) -> String,
    val pattern: String,
)

private val projectTabs = listOf(
    ProjectTab("Klienti", Icons.Filled.Person, { Routes.clients(it) }, Routes.CLIENTS),
    ProjectTab("Produkty", Icons.Filled.Inventory2, { Routes.products(it) }, Routes.PRODUCTS),
    ProjectTab("Platby", Icons.AutoMirrored.Filled.ReceiptLong, { Routes.sales(it) }, Routes.SALES),
    ProjectTab("Účtárna", Icons.Filled.Insights, { Routes.accounting(it) }, Routes.ACCOUNTING),
)

private fun NavBackStackEntry.projectId(): Long = arguments?.getLong(Routes.ARG_PROJECT) ?: 0L

@Composable
fun FinancerNavHost() {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    val currentProjectId = entry?.takeIf { it.arguments?.containsKey(Routes.ARG_PROJECT) == true }?.projectId()
    // Spodní navigace se ukazuje jen na čtyřech záložkách projektu.
    val tabProjectId = currentProjectId.takeIf { currentRoute in Routes.projectTabs }

    Scaffold(
        bottomBar = {
            if (tabProjectId != null) {
                NavigationBar {
                    projectTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.pattern,
                            onClick = {
                                navController.navigate(tab.route(tabProjectId)) {
                                    popUpTo(Routes.PROJECTS) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.PROJECTS,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.PROJECTS) {
                ProjectsScreen(onOpenProject = { id -> navController.navigate(Routes.clients(id)) })
            }

            composable(Routes.CLIENTS, arguments = projectArgs) { backStack ->
                val projectId = backStack.projectId()
                ClientsScreen(
                    projectId = projectId,
                    onOpenClient = { clientId -> navController.navigate(Routes.clientDetail(projectId, clientId)) },
                    onOpenSettings = { navController.navigate(Routes.settings(projectId)) },
                )
            }

            composable(Routes.CLIENT_DETAIL, arguments = projectArgs + clientArgs) { backStack ->
                ClientDetailScreen(
                    projectId = backStack.projectId(),
                    clientId = backStack.arguments?.getLong(Routes.ARG_CLIENT) ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.PRODUCTS, arguments = projectArgs) { backStack ->
                val projectId = backStack.projectId()
                ProductsScreen(
                    projectId = projectId,
                    onNewProduct = { navController.navigate(Routes.productNew(projectId)) },
                    onEditProduct = { productId -> navController.navigate(Routes.productEdit(projectId, productId)) },
                    onOpenSettings = { navController.navigate(Routes.settings(projectId)) },
                )
            }

            composable(Routes.PRODUCT_NEW, arguments = projectArgs) { backStack ->
                ProductEditScreen(
                    projectId = backStack.projectId(),
                    productId = null,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.PRODUCT_EDIT, arguments = projectArgs + productArgs) { backStack ->
                ProductEditScreen(
                    projectId = backStack.projectId(),
                    productId = backStack.arguments?.getLong(Routes.ARG_PRODUCT),
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SALES, arguments = projectArgs) { backStack ->
                val projectId = backStack.projectId()
                SalesScreen(
                    projectId = projectId,
                    onNewSale = { type -> navController.navigate(Routes.saleNew(projectId, type.name)) },
                    onOpenSettings = { navController.navigate(Routes.settings(projectId)) },
                )
            }

            composable(Routes.SALE_NEW, arguments = projectArgs + typeArgs) { backStack ->
                val type = TransactionType.valueOf(backStack.arguments?.getString(Routes.ARG_TYPE) ?: "PAYMENT")
                NewSaleScreen(
                    projectId = backStack.projectId(),
                    type = type,
                    onDone = { navController.popBackStack() },
                )
            }

            composable(Routes.ACCOUNTING, arguments = projectArgs) { backStack ->
                AccountingScreen(
                    projectId = backStack.projectId(),
                    onOpenClient = { clientId -> navController.navigate(Routes.clientDetail(backStack.projectId(), clientId)) },
                    onOpenSettings = { navController.navigate(Routes.settings(backStack.projectId())) },
                )
            }

            composable(Routes.SETTINGS, arguments = projectArgs) { backStack ->
                ProjectSettingsScreen(
                    projectId = backStack.projectId(),
                    onBack = { navController.popBackStack() },
                    onDeleted = {
                        navController.navigate(Routes.PROJECTS) {
                            popUpTo(Routes.PROJECTS) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}

private val projectArgs = listOf(
    navArgument(Routes.ARG_PROJECT) { type = NavType.LongType },
)

private val clientArgs = listOf(
    navArgument(Routes.ARG_CLIENT) { type = NavType.LongType },
)

private val productArgs = listOf(
    navArgument(Routes.ARG_PRODUCT) { type = NavType.LongType },
)

private val typeArgs = listOf(
    navArgument(Routes.ARG_TYPE) { type = NavType.StringType },
)
