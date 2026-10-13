package cz.financer.ui.clients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.financer.data.PropertyDefinition
import cz.financer.data.SaleWithItems
import cz.financer.data.TransactionType
import cz.financer.domain.Money
import cz.financer.ui.components.ConfirmDialog
import cz.financer.ui.components.DateTimeField
import cz.financer.ui.components.EmptyState
import cz.financer.ui.components.LabeledValue
import cz.financer.ui.components.SectionTitle
import cz.financer.ui.components.TextInputDialog
import cz.financer.ui.components.displayLabel
import cz.financer.ui.components.formatDateTime
import cz.financer.ui.financer
import cz.financer.ui.project.ClientRow
import cz.financer.ui.project.ProjectViewModel
import cz.financer.ui.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    projectId: Long,
    clientId: Long,
    onBack: () -> Unit,
    onReceipt: (Long) -> Unit,
) {
    val app = LocalContext.current.financer()
    val viewModel: ProjectViewModel = viewModel(
        key = "project-$projectId",
        factory = viewModelFactory { ProjectViewModel(app, projectId) },
    )
    val clients by viewModel.clients.collectAsState()
    val definitions by viewModel.definitions.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val row = clients.firstOrNull { it.client.id == clientId }

    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var repaying by remember { mutableStateOf(false) }
    var addingPersonal by remember { mutableStateOf(false) }
    var editingValueFor by remember { mutableStateOf<PropertyDefinition?>(null) }
    var deletingPersonal by remember { mutableStateOf<PropertyDefinition?>(null) }
    var saleToDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(row?.client?.name ?: "Klient", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    if (row != null) {
                        IconButton(onClick = { renaming = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Přejmenovat klienta")
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Smazat klienta")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (row == null) {
            EmptyState("Klient nebyl nalezen.", Modifier.padding(padding))
            return@Scaffold
        }

        val clientSales = sales.filter { it.sale.clientId == clientId }

        // Všechny klíče v LazyColumn musí být unikátní napříč sekcemi, proto mají prefix.
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SummaryCard(row)
            }

            item {
                Button(
                    onClick = { repaying = true },
                    enabled = row.debt > 0,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Splatit dluh")
                }
            }

            item { SectionTitle("Vlastnosti") }

            if (definitions.isEmpty() && row.personalProperties.isEmpty()) {
                item {
                    Text(
                        "Zatím nejsou žádné vlastnosti. Globální vlastnosti nastavíte v Nastavení projektu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            items(definitions, key = { "global-${it.id}" }) { definition ->
                PropertyRow(
                    name = definition.name,
                    value = row.values[definition.id].orEmpty(),
                    onClick = { editingValueFor = definition },
                )
            }

            items(row.personalProperties, key = { "personal-${it.id}" }) { definition ->
                PropertyRow(
                    name = definition.name,
                    value = row.values[definition.id].orEmpty(),
                    onClick = { editingValueFor = definition },
                    onDelete = { deletingPersonal = definition },
                )
            }

            item {
                OutlinedButton(onClick = { addingPersonal = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("  Přidat osobní vlastnost")
                }
            }

            item { SectionTitle("Historie") }
            if (clientSales.isEmpty()) {
                item {
                    Text(
                        "Zatím žádné záznamy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            } else {
                items(clientSales, key = { "sale-${it.sale.id}" }) { sale ->
                    HistoryRow(sale, onDelete = { saleToDelete = sale.sale.id })
                }
            }
        }
    }

    if (renaming && row != null) {
        TextInputDialog(
            title = "Přejmenovat klienta",
            label = "Jméno klienta",
            initial = row.client.name,
            confirmLabel = "Uložit",
            onConfirm = { name ->
                viewModel.updateClient(row.client, name)
                renaming = false
            },
            onDismiss = { renaming = false },
        )
    }

    if (confirmDelete && row != null) {
        ConfirmDialog(
            title = "Smazat klienta?",
            text = "Klient „${row.client.name}“ i všechny jeho záznamy budou odstraněny.",
            confirmLabel = "Smazat",
            onConfirm = {
                viewModel.deleteClient(row.client)
                confirmDelete = false
                onBack()
            },
            onDismiss = { confirmDelete = false },
        )
    }

    val editing = editingValueFor
    if (editing != null && row != null) {
        TextInputDialog(
            title = editing.name,
            label = "Hodnota",
            initial = row.values[editing.id].orEmpty(),
            confirmLabel = "Uložit",
            onConfirm = { value ->
                viewModel.setClientValue(row.client.id, editing.id, value)
                editingValueFor = null
            },
            onDismiss = { editingValueFor = null },
        )
    }

    if (addingPersonal && row != null) {
        AddPersonalPropertyDialog(
            onConfirm = { name, value ->
                viewModel.addPersonalProperty(row.client.id, name, value)
                addingPersonal = false
            },
            onDismiss = { addingPersonal = false },
        )
    }

    val personalToDelete = deletingPersonal
    if (personalToDelete != null) {
        ConfirmDialog(
            title = "Smazat vlastnost?",
            text = "Vlastnost „${personalToDelete.name}“ se odstraní jen u tohoto klienta.",
            confirmLabel = "Smazat",
            onConfirm = {
                viewModel.deletePropertyDefinition(personalToDelete)
                deletingPersonal = null
            },
            onDismiss = { deletingPersonal = null },
        )
    }

    if (repaying && row != null) {
        RepaymentDialog(
            row = row,
            onConfirm = { amount, date ->
                viewModel.recordRepayment(row.client.id, amount, date, onIssued = onReceipt)
            },
            onDismiss = { repaying = false },
        )
    }

    val deleting = saleToDelete
    if (deleting != null) {
        ConfirmDialog(
            title = "Smazat záznam?",
            text = "Záznam se odstraní z historie a přepočítá se dluh klienta.",
            confirmLabel = "Smazat",
            onConfirm = {
                viewModel.deleteSale(deleting)
                saleToDelete = null
            },
            onDismiss = { saleToDelete = null },
        )
    }
}

@Composable
private fun SummaryCard(row: ClientRow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            LabeledValue(label = "Dluh", value = Money.format(row.debt), modifier = Modifier)
            LabeledValue(label = "Vydělal jsi", value = Money.format(row.earned), modifier = Modifier)
        }
    }
}

@Composable
private fun PropertyRow(
    name: String,
    value: String,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f),
            )
            Text(
                value.ifEmpty { "doplnit" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (value.isEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            )
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Smazat vlastnost")
                }
            }
        }
    }
}

@Composable
private fun AddPersonalPropertyDialog(
    onConfirm: (name: String, value: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Osobní vlastnost") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Název, např. Výška") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Hodnota, např. 150 cm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, value) },
                enabled = name.isNotBlank(),
            ) { Text("Přidat") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}

@Composable
private fun HistoryRow(sale: SaleWithItems, onDelete: () -> Unit) {
    val items = sale.items.joinToString(", ") { "${it.quantity}× ${it.productNameSnapshot}" }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${sale.sale.displayLabel()} · ${formatDateTime(sale.sale.createdAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                if (items.isNotEmpty()) {
                    Text(items, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    Money.format(sale.sale.totalHalire),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (sale.sale.type == TransactionType.REPAYMENT) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Smazat záznam")
            }
        }
    }
}

/** Splacení dluhu libovolnou částkou, nejvýše do výše dluhu. */
@Composable
private fun RepaymentDialog(
    row: ClientRow,
    onConfirm: (amountHalire: Long, dateMillis: Long) -> String?,
    onDismiss: () -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }
    val amount = Money.parse(amountText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Splatit dluh") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Aktuální dluh: ${Money.format(row.debt)}", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        error = null
                    },
                    label = { Text("Částka") },
                    suffix = { Text("CZK") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                DateTimeField(millis = date, onChange = { date = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount != null,
                onClick = {
                    val value = amount ?: return@TextButton
                    error = onConfirm(value, date)
                    if (error == null) onDismiss()
                },
            ) { Text("Splatit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}
