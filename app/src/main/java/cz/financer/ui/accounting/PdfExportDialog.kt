package cz.financer.ui.accounting

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import cz.financer.data.Client
import cz.financer.data.Project
import cz.financer.data.SaleWithItems
import cz.financer.pdf.PdfColumn
import cz.financer.pdf.PdfExporter
import cz.financer.pdf.PdfPeriod
import cz.financer.pdf.PdfRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Výběr období, klienta a sloupců před vygenerováním PDF výpisu. */
@Composable
fun PdfExportDialog(
    project: Project,
    sales: List<SaleWithItems>,
    clients: List<Client>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var period by remember { mutableStateOf(PdfPeriod.LAST_MONTH) }
    var clientId by remember { mutableStateOf<Long?>(null) }
    var columns by remember {
        mutableStateOf(setOf(PdfColumn.DATE, PdfColumn.CLIENT, PdfColumn.TYPE, PdfColumn.VALUE))
    }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text("Výpis do PDF") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Období", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PdfPeriod.entries.forEach { option ->
                        FilterChip(
                            selected = period == option,
                            onClick = { period = option },
                            label = { Text(option.label) },
                        )
                    }
                }

                Text("Klient", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = clientId == null,
                            onClick = { clientId = null },
                            label = { Text("Všichni") },
                        )
                    }
                    items(clients, key = { it.id }) { client ->
                        FilterChip(
                            selected = clientId == client.id,
                            onClick = { clientId = client.id },
                            label = { Text(client.name) },
                        )
                    }
                }

                Text("Sloupce tabulky", style = MaterialTheme.typography.labelLarge)
                PdfColumn.entries.forEach { column ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = column in columns,
                            onCheckedChange = { checked ->
                                columns = if (checked) columns + column else columns - column
                            },
                        )
                        Text(column.label)
                    }
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !working && columns.isNotEmpty(),
                onClick = {
                    working = true
                    error = null
                    scope.launch {
                        try {
                            val file = withContext(Dispatchers.IO) {
                                PdfExporter(context).export(
                                    project = project,
                                    sales = sales,
                                    clientNames = clients.associate { it.id to it.name },
                                    request = PdfRequest(period, clientId, columns),
                                )
                            }
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(share, "Sdílet výpis"))
                            onDismiss()
                        } catch (e: Exception) {
                            error = "PDF se nepodařilo vytvořit: ${e.message ?: "neznámá chyba"}"
                        } finally {
                            working = false
                        }
                    }
                },
            ) { Text(if (working) "Vytvářím…" else "Vytvořit PDF") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !working) { Text("Zrušit") } },
    )
}
