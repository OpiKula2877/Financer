package cz.financer.ui.accounting

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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.financer.data.SaleWithItems
import cz.financer.data.TransactionType
import cz.financer.domain.Money
import cz.financer.domain.SaleCalculator
import cz.financer.domain.Statistics
import cz.financer.ui.components.BarChart
import cz.financer.ui.components.ChartLegend
import cz.financer.ui.components.ChartPalette
import cz.financer.ui.components.ChartSlice
import cz.financer.ui.components.DonutChart
import cz.financer.ui.components.LineChart
import cz.financer.ui.components.SectionTitle
import cz.financer.ui.components.displayLabel
import cz.financer.ui.components.formatDateTime
import cz.financer.ui.financer
import cz.financer.ui.project.ProjectViewModel
import cz.financer.ui.viewModelFactory
import java.time.format.DateTimeFormatter

private enum class ArchiveFilter(val label: String, val type: TransactionType?) {
    ALL("Vše", null),
    PAYMENTS("Platby", TransactionType.PAYMENT),
    DEBTS("Dluhy", TransactionType.DEBT),
    REPAYMENTS("Splacení", TransactionType.REPAYMENT),
}

private val monthLabel: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/yy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingScreen(
    projectId: Long,
    onOpenClient: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val app = LocalContext.current.financer()
    val viewModel: ProjectViewModel = viewModel(
        key = "project-$projectId",
        factory = viewModelFactory { ProjectViewModel(app, projectId) },
    )
    val project by viewModel.project.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val clients by viewModel.clients.collectAsState()
    var showExport by remember { mutableStateOf(false) }
    var archiveFilter by remember { mutableStateOf(ArchiveFilter.ALL) }

    val clientNames = clients.associate { it.client.id to it.client.name }
    val debtors = SaleCalculator.debtorBalances(clients.map { it.client.id }, sales)
    val shares = SaleCalculator.debtShares(debtors)
    val debtSlices = debtors.entries
        .sortedByDescending { it.value }
        .mapIndexed { index, entry ->
            ChartSlice(
                label = clientNames[entry.key] ?: "Klient",
                value = entry.value.toDouble(),
                color = ChartPalette[index % ChartPalette.size],
            )
        }
    val topProducts = Statistics.productSales(sales).take(6)
    val monthly = Statistics.monthlySales(sales).takeLast(12)
    val archive = sales.filter { archiveFilter.type == null || it.sale.type == archiveFilter.type }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Účtárna", fontWeight = FontWeight.SemiBold) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { OverviewCard(totalSales = Statistics.totalSales(sales), totalDebt = Statistics.totalDebt(sales)) }

            item {
                val best = topProducts.firstOrNull()
                Text(
                    if (best == null) "Nejprodávanější produkt: zatím žádný prodej"
                    else "Nejprodávanější produkt: ${best.productName} (${best.quantity} ks)",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            item { SectionTitle("Dluhy") }
            if (debtors.isEmpty()) {
                item {
                    Text("Nikdo teď nedluží.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                item { DebtTableHeader() }
                // Klíče musí být unikátní v celém LazyColumn, proto mají prefix podle sekce.
                items(debtors.entries.sortedByDescending { it.value }.toList(), key = { "debt-${it.key}" }) { entry ->
                    DebtRow(
                        name = clientNames[entry.key] ?: "Klient",
                        debt = entry.value,
                        share = shares[entry.key] ?: 0.0,
                        onClick = { onOpenClient(entry.key) },
                    )
                }
                item { SectionTitle("Podíl dluhů") }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DonutChart(debtSlices)
                        ChartLegend(debtSlices, percentOf = { slice ->
                            "%.0f %%".format(slice.value * 100.0 / debtSlices.sumOf { it.value })
                        })
                    }
                }
            }

            item { SectionTitle("Prodané kusy podle produktu") }
            item {
                if (topProducts.isEmpty()) {
                    Text("Zatím žádné prodeje.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                } else {
                    BarChart(topProducts.map { it.productName to it.quantity })
                }
            }

            item { SectionTitle("Tržby podle měsíců") }
            item {
                if (monthly.size < 2) {
                    Text("Graf se zobrazí, až budou záznamy alespoň ve dvou měsících.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                } else {
                    LineChart(monthly.map { it.month.format(monthLabel) to (it.totalHalire / 100.0) })
                }
            }

            item { SectionTitle("Výpis") }
            item {
                Button(onClick = { showExport = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    Text("  Vytvořit PDF výpis")
                }
            }

            item { SectionTitle("Archiv") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArchiveFilter.entries.forEach { option ->
                        FilterChip(
                            selected = archiveFilter == option,
                            onClick = { archiveFilter = option },
                            label = { Text(option.label) },
                        )
                    }
                }
            }
            if (archive.isEmpty()) {
                item { Text("Žádné záznamy.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
            } else {
                items(archive, key = { "sale-${it.sale.id}" }) { sale ->
                    ArchiveRow(sale, clientName = clientNames[sale.sale.clientId] ?: "Smazaný klient") {
                        onOpenClient(sale.sale.clientId)
                    }
                }
            }
        }
    }

    val currentProject = project
    if (showExport && currentProject != null) {
        PdfExportDialog(
            project = currentProject,
            sales = sales,
            clients = clients.map { it.client },
            onDismiss = { showExport = false },
        )
    }
}

@Composable
private fun OverviewCard(totalSales: Long, totalDebt: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Celkový dluh všech: ${Money.format(totalDebt)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            Text("Tržby celkem: ${Money.format(totalSales)}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun DebtTableHeader() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Text("Klient", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Text("Dluh", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(0.8f))
        Text("Podíl", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(0.6f))
    }
}

@Composable
private fun DebtRow(name: String, debt: Long, share: Double, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(Money.format(debt), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.8f))
            Text("%.0f %%".format(share), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(0.6f))
        }
    }
}

@Composable
private fun ArchiveRow(sale: SaleWithItems, clientName: String, onClick: () -> Unit) {
    val items = sale.items.joinToString(", ") { "${it.quantity}× ${it.productNameSnapshot}" }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(clientName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(Money.format(sale.sale.totalHalire), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "${sale.sale.displayLabel()} · ${formatDateTime(sale.sale.createdAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            if (items.isNotEmpty()) Text(items, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
