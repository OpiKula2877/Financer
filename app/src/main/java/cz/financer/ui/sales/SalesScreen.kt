package cz.financer.ui.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.financer.data.SaleWithItems
import cz.financer.data.TransactionType
import cz.financer.domain.Money
import cz.financer.ui.components.EmptyState
import cz.financer.ui.components.displayLabel
import cz.financer.ui.components.formatDateTime
import cz.financer.ui.financer
import cz.financer.ui.project.ProjectViewModel
import cz.financer.ui.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    projectId: Long,
    onNewSale: (TransactionType) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val app = LocalContext.current.financer()
    val viewModel: ProjectViewModel = viewModel(
        key = "project-$projectId",
        factory = viewModelFactory { ProjectViewModel(app, projectId) },
    )
    val sales by viewModel.sales.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val project by viewModel.project.collectAsState()
    val clientNames = clients.associate { it.client.id to it.client.name }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Platby", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Nastavení projektu")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onNewSale(TransactionType.PAYMENT) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("  Nová platba")
                    }
                    OutlinedButton(
                        onClick = { onNewSale(TransactionType.DEBT) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("  Přidat dluh")
                    }
                }
            }

            item {
                Text(
                    "Poslední záznamy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (sales.isEmpty()) {
                item {
                    EmptyState("Zatím nejsou žádné záznamy. Začněte tlačítkem Nová platba.")
                }
            } else {
                items(sales, key = { it.sale.id }) { sale ->
                    SaleCard(sale, clientName = clientNames[sale.sale.clientId] ?: "Smazaný klient")
                }
            }
        }
    }
}

@Composable
private fun SaleCard(sale: SaleWithItems, clientName: String) {
    val summary = sale.items.joinToString(", ") { "${it.quantity}× ${it.productNameSnapshot}" }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(clientName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    Money.format(sale.sale.totalHalire),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (sale.sale.type == TransactionType.DEBT) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                )
            }
            Text(
                "${sale.sale.displayLabel()} · ${formatDateTime(sale.sale.createdAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            if (summary.isNotEmpty()) {
                Text(summary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
