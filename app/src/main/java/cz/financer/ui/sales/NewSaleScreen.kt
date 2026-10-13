package cz.financer.ui.sales

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.financer.data.TransactionType
import cz.financer.domain.CartLine
import cz.financer.domain.Money
import cz.financer.domain.SaleCalculator
import cz.financer.ui.components.DateTimeField
import cz.financer.ui.components.LocalImage
import cz.financer.ui.components.SectionTitle
import cz.financer.ui.financer
import cz.financer.ui.project.ClientRow
import cz.financer.ui.project.ProductRow
import cz.financer.ui.project.ProjectViewModel
import cz.financer.ui.viewModelFactory

/** Filtr produktů ve formuláři platby. */
private enum class SaleProductFilter(val label: String) {
    ALL("Vše"),
    WITHOUT_SOLD_OUT("Bez vyprodaných"),
}

/**
 * Formulář platby (PAYMENT) nebo dluhu (DEBT). Stejný formulář, liší se jen typ záznamu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSaleScreen(
    projectId: Long,
    type: TransactionType,
    onDone: () -> Unit,
    onIssued: (Long) -> Unit,
) {
    val app = LocalContext.current.financer()
    val viewModel: ProjectViewModel = viewModel(
        key = "project-$projectId",
        factory = viewModelFactory { ProjectViewModel(app, projectId) },
    )
    val clients by viewModel.clients.collectAsState()
    val products by viewModel.products.collectAsState()

    var clientId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pickingClient by remember { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(SaleProductFilter.ALL) }
    var date by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    val quantities = remember { mutableStateMapOf<Long, Int>() }

    val selectedClient = clients.firstOrNull { it.client.id == clientId }
    val visibleProducts = products.filter {
        filter == SaleProductFilter.ALL || !it.product.isSoldOut
    }
    val lines = products
        .filter { (quantities[it.product.id] ?: 0) > 0 }
        .map {
            CartLine(
                productId = it.product.id,
                productName = it.product.name,
                unitPriceHalire = it.product.unitPriceHalire,
                quantity = quantities[it.product.id] ?: 0,
            )
        }
    val total = SaleCalculator.cartTotal(lines)
    val containsSoldOut = lines.any { line -> products.any { it.product.id == line.productId && it.product.isSoldOut } }
    val title = if (type == TransactionType.PAYMENT) "Nová platba" else "Přidat dluh"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cena: ${Money.format(total)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    if (containsSoldOut) {
                        Text(
                            "V košíku jsou vyprodané produkty.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f)) {
                            Text("Zrušit")
                        }
                        Button(
                            onClick = {
                                val id = clientId ?: return@Button
                                viewModel.recordSale(id, type, date, lines, onIssued = onIssued)
                            },
                            enabled = selectedClient != null && lines.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Potvrdit")
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ClientSelector(
                    selected = selectedClient,
                    onClick = { pickingClient = true },
                )
            }

            item {
                DateTimeField(millis = date, onChange = { date = it })
            }

            item { SectionTitle("Produkty") }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SaleProductFilter.entries.forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = { Text(option.label) },
                        )
                    }
                }
            }

            if (visibleProducts.isEmpty()) {
                item {
                    Text(
                        "Žádné produkty. Nejprve je přidejte v záložce Produkty.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            items(visibleProducts, key = { it.product.id }) { row ->
                ProductLine(
                    row = row,
                    quantity = quantities[row.product.id] ?: 0,
                    onQuantityChange = { newQuantity ->
                        if (newQuantity <= 0) quantities.remove(row.product.id)
                        else quantities[row.product.id] = newQuantity
                    },
                )
            }
        }
    }

    if (pickingClient) {
        ClientPickerDialog(
            clients = clients,
            onPick = {
                clientId = it
                pickingClient = false
            },
            onDismiss = { pickingClient = false },
        )
    }
}

@Composable
private fun ClientSelector(selected: ClientRow?, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(
                if (selected == null) "Vyberte klienta" else selected.client.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (selected != null) {
                Text(
                    "Vydělal jsi: ${Money.format(selected.earned)} · Dluží: ${Money.format(selected.debt)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ClientPickerDialog(
    clients: List<ClientRow>,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vyberte klienta") },
        text = {
            if (clients.isEmpty()) {
                Text("Zatím nejsou žádní klienti. Přidejte je v záložce Klienti.")
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(clients, key = { it.client.id }) { row ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(row.client.id) }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(row.client.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Vydělal jsi: ${Money.format(row.earned)} · Dluží: ${Money.format(row.debt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}

@Composable
private fun ProductLine(
    row: ProductRow,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
) {
    val product = row.product
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))) {
            LocalImage(product.imagePath, Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                Money.format(product.unitPriceHalire) + " / ks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            if (product.isSoldOut) {
                Text("Vyprodáno", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
        IconButton(onClick = { onQuantityChange(quantity - 1) }, enabled = quantity > 0) {
            Icon(Icons.Filled.Remove, contentDescription = "Ubrat")
        }
        Text(
            quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        IconButton(onClick = { onQuantityChange(quantity + 1) }) {
            Icon(Icons.Filled.Add, contentDescription = "Přidat")
        }
    }
}
