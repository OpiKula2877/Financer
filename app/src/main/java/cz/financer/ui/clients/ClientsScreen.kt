package cz.financer.ui.clients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.financer.domain.Money
import cz.financer.domain.TextSearch
import cz.financer.ui.components.EmptyState
import cz.financer.ui.financer
import cz.financer.ui.project.ClientRow
import cz.financer.ui.project.ProjectViewModel
import cz.financer.ui.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    projectId: Long,
    onOpenClient: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val app = LocalContext.current.financer()
    val viewModel: ProjectViewModel = viewModel(
        key = "project-$projectId",
        factory = viewModelFactory { ProjectViewModel(app, projectId) },
    )
    val clients by viewModel.clients.collectAsState()
    val project by viewModel.project.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val visible = clients.filter { TextSearch.matches(it.client.name, query) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Klienti", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Nastavení projektu")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Přidat klienta")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (clients.isNotEmpty()) {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Hledat klienta",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            when {
                clients.isEmpty() -> EmptyState("Zatím žádní klienti. Přidejte prvního tlačítkem +.")
                visible.isEmpty() -> EmptyState("Žádný klient neodpovídá hledání.")
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(visible, key = { it.client.id }) { row ->
                        ClientCard(row, onClick = { onOpenClient(row.client.id) })
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddClientDialog(
            onConfirm = { name, debt, paid ->
                viewModel.addClient(name, debt, paid)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        modifier = modifier,
    )
}

/**
 * Nový klient. Dluh a již zaplacená částka jsou volitelné a zapíší se jako
 * ručně nastavený stav (typ „Nastavený ručně“).
 */
@Composable
private fun AddClientDialog(
    onConfirm: (name: String, debtHalire: Long, paidHalire: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var debtText by remember { mutableStateOf("") }
    var paidText by remember { mutableStateOf("") }

    val debt = if (debtText.isBlank()) 0L else Money.parse(debtText)
    val paid = if (paidText.isBlank()) 0L else Money.parse(paidText)
    val valid = name.isNotBlank() && debt != null && paid != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nový klient") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Jméno klienta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = debtText,
                    onValueChange = { debtText = it },
                    label = { Text("Dluh, který zbývá zaplatit") },
                    suffix = { Text("CZK") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = debt == null,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = paidText,
                    onValueChange = { paidText = it },
                    label = { Text("Už zaplaceno") },
                    suffix = { Text("CZK") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = paid == null,
                    supportingText = { Text("Nepovinné. Nechte prázdné, pokud nic neplatil.") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, debt ?: 0L, paid ?: 0L) },
                enabled = valid,
            ) { Text("Přidat") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}

@Composable
private fun ClientCard(row: ClientRow, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(row.client.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Vydělal jsi: ${Money.format(row.earned)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Dluh",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    Money.format(row.debt),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (row.debt > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
