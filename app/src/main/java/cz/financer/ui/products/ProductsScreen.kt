package cz.financer.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.financer.domain.Money
import cz.financer.domain.TextSearch
import cz.financer.ui.clients.SearchField
import cz.financer.ui.components.EmptyState
import cz.financer.ui.components.LocalImage
import cz.financer.ui.components.SoldOutRibbon
import cz.financer.ui.financer
import cz.financer.ui.project.ProductRow
import cz.financer.ui.project.ProjectViewModel
import cz.financer.ui.viewModelFactory

enum class ProductFilter(val label: String) {
    ALL("Vše"),
    AVAILABLE("Jen dostupné"),
    SOLD_OUT("Jen vyprodané"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    projectId: Long,
    onNewProduct: () -> Unit,
    onEditProduct: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val app = LocalContext.current.financer()
    val viewModel: ProjectViewModel = viewModel(
        key = "project-$projectId",
        factory = viewModelFactory { ProjectViewModel(app, projectId) },
    )
    val products by viewModel.products.collectAsState()
    val project by viewModel.project.collectAsState()
    var filter by remember { mutableStateOf(ProductFilter.ALL) }

    var query by rememberSaveable { mutableStateOf("") }
    val visible = products.filter { TextSearch.matches(it.product.name, query) }.filter {
        when (filter) {
            ProductFilter.ALL -> true
            ProductFilter.AVAILABLE -> !it.product.isSoldOut
            ProductFilter.SOLD_OUT -> it.product.isSoldOut
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Produkty", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Nastavení projektu")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewProduct) {
                Icon(Icons.Filled.Add, contentDescription = "Nový produkt")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Hledat produkt",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProductFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.label) },
                    )
                }
            }

            if (visible.isEmpty()) {
                EmptyState(
                    if (products.isEmpty()) "Zatím tu nejsou žádné produkty. Přidejte první tlačítkem +."
                    else "Žádné produkty pro tento filtr.",
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(visible, key = { it.product.id }) { row ->
                        ProductCard(row, onClick = { onEditProduct(row.product.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(row: ProductRow, onClick: () -> Unit) {
    val product = row.product
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                LocalImage(product.imagePath, Modifier.fillMaxSize())
                if (product.isSoldOut) SoldOutRibbon()
            }
            Column(Modifier.padding(12.dp)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    Money.format(product.unitPriceHalire) + " / ks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Prodáno: ${row.soldQuantity} ks",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
