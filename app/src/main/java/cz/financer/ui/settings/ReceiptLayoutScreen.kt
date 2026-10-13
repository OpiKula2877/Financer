package cz.financer.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.financer.data.Project
import cz.financer.data.TransactionType
import cz.financer.receipt.ReceiptArt
import cz.financer.receipt.ReceiptData
import cz.financer.receipt.ReceiptFormat
import cz.financer.receipt.ReceiptLayout
import cz.financer.receipt.ReceiptLine
import cz.financer.receipt.ReceiptQr
import cz.financer.receipt.ReceiptRenderer
import cz.financer.ui.components.EmptyState
import cz.financer.ui.components.SectionTitle
import cz.financer.ui.financer
import cz.financer.ui.project.ProjectViewModel
import cz.financer.ui.viewModelFactory

/** Nastavení, co se zobrazí na účtence. Změny platí pro účtenky vystavené od této chvíle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptLayoutScreen(
    projectId: Long,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.financer()
    val viewModel: ProjectViewModel = viewModel(
        key = "project-$projectId",
        factory = viewModelFactory { ProjectViewModel(app, projectId) },
    )
    val project by viewModel.project.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upravit e-účtenku") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
            )
        },
    ) { padding ->
        val current = project
        if (current == null) {
            EmptyState("Projekt nebyl nalezen.", Modifier.padding(padding))
            return@Scaffold
        }
        val layout = ReceiptLayout.fromJson(current.receiptLayout)

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Přepínače určují, co se na účtence zobrazí. Hlavička se zobrazí jen tehdy, když ji vyplníte v nastavení projektu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            item {
                LayoutSwitch("Hlavička (název, adresa, telefon)", layout.showHeader) {
                    viewModel.saveReceiptLayout(layout.copy(showHeader = it))
                }
            }
            item {
                LayoutSwitch("Datum a čas", layout.showDateTime) {
                    viewModel.saveReceiptLayout(layout.copy(showDateTime = it))
                }
            }
            item {
                LayoutSwitch("Číslo účtenky", layout.showNumber) {
                    viewModel.saveReceiptLayout(layout.copy(showNumber = it))
                }
            }
            item {
                LayoutSwitch("Typ (platba, na dluh, splacení)", layout.showType) {
                    viewModel.saveReceiptLayout(layout.copy(showType = it))
                }
            }
            item {
                LayoutSwitch("Jméno klienta", layout.showClient) {
                    viewModel.saveReceiptLayout(layout.copy(showClient = it))
                }
            }
            item {
                LayoutSwitch("Položky", layout.showItems) {
                    viewModel.saveReceiptLayout(layout.copy(showItems = it))
                }
            }
            item {
                LayoutSwitch("Množství a cena za kus", layout.showQuantities) {
                    viewModel.saveReceiptLayout(layout.copy(showQuantities = it))
                }
            }
            item {
                LayoutSwitch("Celková částka", layout.showTotal) {
                    viewModel.saveReceiptLayout(layout.copy(showTotal = it))
                }
            }
            item {
                LayoutSwitch("BKP", layout.showBkp) {
                    viewModel.saveReceiptLayout(layout.copy(showBkp = it))
                }
            }
            item {
                LayoutSwitch("QR kód", layout.showQr) {
                    viewModel.saveReceiptLayout(layout.copy(showQr = it))
                }
            }

            item { SectionTitle("Další číslo účtenky") }
            item { NextNumberField(current.nextReceiptNumber, viewModel) }

            item { SectionTitle("Náhled") }
            item { ReceiptPreview(current, layout) }
        }
    }
}

@Composable
private fun LayoutSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NextNumberField(current: Long, viewModel: ProjectViewModel) {
    var text by remember(current) { mutableStateOf(current.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Příští účtenka dostane číslo ${ReceiptFormat.number(current)}. Číslo se zvyšuje po každé účtence.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it.filter(Char::isDigit)
                error = null
            },
            label = { Text("Další číslo") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = {
                val value = text.toLongOrNull()
                if (value == null || value < 1) {
                    error = "Zadejte kladné číslo."
                } else {
                    viewModel.setNextReceiptNumber(value) { error = it }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Nastavit další číslo")
        }
    }
}

@Composable
private fun ReceiptPreview(project: Project, layout: ReceiptLayout) {
    val preview = remember(project, layout) { ReceiptRenderer.toBitmap(sampleArt(project, layout)) }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Image(
            bitmap = preview.asImageBitmap(),
            contentDescription = "Náhled účtenky",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(0.8f),
        )
    }
}

/** Ukázková účtenka s údaji z projektu. Slouží jen pro náhled, nic se neukládá. */
private fun sampleArt(project: Project, layout: ReceiptLayout): ReceiptArt {
    val sampleBkp = "XXXXXXXX-XXXXXXXX-XXXXXXXX-XXXXXXXX-XXXXXXXX"
    return ReceiptArt(
        data = ReceiptData(
            number = project.nextReceiptNumber,
            issuedAt = System.currentTimeMillis(),
            type = TransactionType.DEBT,
            taxpayerName = project.taxpayerName?.takeIf { it.isNotBlank() },
            address = project.address?.takeIf { it.isNotBlank() },
            phone = project.phone?.takeIf { it.isNotBlank() },
            clientName = "Ukázkový klient",
            lines = listOf(ReceiptLine("Mýdlo", 5, 200), ReceiptLine("Ručník", 4, 300)),
            totalHalire = 2200,
            layout = layout,
        ),
        bkp = sampleBkp,
        cancelled = false,
        qr = ReceiptQr.create(sampleBkp, "ukazka", 240).bitmap,
        qrHasFullPayload = true,
    )
}
