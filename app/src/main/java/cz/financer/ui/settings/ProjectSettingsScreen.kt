package cz.financer.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.financer.data.PropertyDefinition
import cz.financer.ui.components.ConfirmDialog
import cz.financer.ui.components.EmptyState
import cz.financer.ui.components.LocalImage
import cz.financer.ui.components.SectionTitle
import cz.financer.ui.components.TextInputDialog
import cz.financer.ui.components.UriImage
import cz.financer.ui.financer
import cz.financer.ui.project.ProjectViewModel
import cz.financer.ui.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSettingsScreen(
    projectId: Long,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onOpenReceiptSettings: () -> Unit,
) {
    val app = LocalContext.current.financer()
    val viewModel: ProjectViewModel = viewModel(
        key = "project-$projectId",
        factory = viewModelFactory { ProjectViewModel(app, projectId) },
    )
    val project by viewModel.project.collectAsState()
    val definitions by viewModel.definitions.collectAsState()

    var name by remember { mutableStateOf("") }
    var ownerFirst by remember { mutableStateOf("") }
    var ownerLast by remember { mutableStateOf("") }
    var taxpayer by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var newLogo by remember { mutableStateOf<Uri?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var identificationKeyOpen by remember { mutableStateOf(false) }
    var identificationKey by remember { mutableStateOf<String?>(null) }

    var addingProperty by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<PropertyDefinition?>(null) }
    var deletingProperty by remember { mutableStateOf<PropertyDefinition?>(null) }
    var confirmDeleteProject by remember { mutableStateOf(false) }

    LaunchedEffect(project) {
        val current = project
        if (!loaded && current != null) {
            name = current.name
            ownerFirst = current.ownerFirstName
            ownerLast = current.ownerLastName
            taxpayer = current.taxpayerName.orEmpty()
            address = current.address.orEmpty()
            phone = current.phone.orEmpty()
            loaded = true
        }
    }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) newLogo = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nastavení projektu") },
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

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionTitle("Projekt") }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    ) {
                        val picked = newLogo
                        if (picked != null) {
                            UriImage(picked, Modifier.fillMaxSize())
                        } else {
                            LocalImage(current.logoPath, Modifier.fillMaxSize())
                        }
                    }
                    OutlinedButton(onClick = { logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text("Změnit logo")
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Název projektu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = ownerFirst,
                    onValueChange = { ownerFirst = it },
                    label = { Text("Jméno") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = ownerLast,
                    onValueChange = { ownerLast = it },
                    label = { Text("Příjmení") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Button(
                    onClick = {
                        viewModel.updateProject(current, name, ownerFirst, ownerLast, newLogo)
                        newLogo = null
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Uložit projekt")
                }
            }

            item { SectionTitle("Účtenka") }
            item {
                Text(
                    "Údaje na účtenku. Prázdné pole se na účtence nezobrazí.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            item {
                OutlinedTextField(
                    value = taxpayer,
                    onValueChange = { taxpayer = it },
                    label = { Text("Název poplatníka (vaše jméno / název firmy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Adresa (napište adresu, kde zrovna prodáváte)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefon (napište zde své telefonní číslo)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Button(
                    onClick = { viewModel.updateReceiptHeader(taxpayer, address, phone) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Uložit údaje účtenky")
                }
            }
            item {
                OutlinedButton(onClick = onOpenReceiptSettings, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Text("  Upravit e-účtenku")
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        identificationKey = null
                        identificationKeyOpen = true
                        viewModel.loadIdentificationKey { identificationKey = it }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Identifikační klíč")
                }
            }

            item { SectionTitle("Vlastnosti klienta") }
            item {
                Text(
                    "Názvy vlastností se nastaví tady. U každého klienta pak stačí doplnit jen hodnotu, např. Výška: 150 cm.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            if (definitions.isEmpty()) {
                item {
                    Text(
                        "Zatím nejsou žádné vlastnosti.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            itemsIndexed(definitions, key = { _, def -> def.id }) { index, definition ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            definition.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { viewModel.swapPropertyOrder(definition, definitions[index - 1]) },
                            enabled = index > 0,
                        ) { Icon(Icons.Filled.ArrowUpward, contentDescription = "Posunout nahoru") }
                        IconButton(
                            onClick = { viewModel.swapPropertyOrder(definition, definitions[index + 1]) },
                            enabled = index < definitions.lastIndex,
                        ) { Icon(Icons.Filled.ArrowDownward, contentDescription = "Posunout dolů") }
                        IconButton(onClick = { renaming = definition }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Přejmenovat")
                        }
                        IconButton(onClick = { deletingProperty = definition }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Smazat vlastnost")
                        }
                    }
                }
            }

            item {
                OutlinedButton(onClick = { addingProperty = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("  Přidat vlastnost")
                }
            }

            item { SectionTitle("Nebezpečná zóna") }
            item {
                TextButton(onClick = { confirmDeleteProject = true }) {
                    Text("Smazat projekt", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (addingProperty) {
        TextInputDialog(
            title = "Nová vlastnost",
            label = "Název, např. Výška",
            confirmLabel = "Přidat",
            onConfirm = { text ->
                viewModel.addPropertyDefinition(text)
                addingProperty = false
            },
            onDismiss = { addingProperty = false },
        )
    }

    renaming?.let { definition ->
        TextInputDialog(
            title = "Přejmenovat vlastnost",
            label = "Název",
            initial = definition.name,
            confirmLabel = "Uložit",
            onConfirm = { text ->
                viewModel.renamePropertyDefinition(definition, text)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }

    deletingProperty?.let { definition ->
        ConfirmDialog(
            title = "Smazat vlastnost?",
            text = "„${definition.name}“ se odstraní u všech klientů včetně zapsaných hodnot.",
            confirmLabel = "Smazat",
            onConfirm = {
                viewModel.deletePropertyDefinition(definition)
                deletingProperty = null
            },
            onDismiss = { deletingProperty = null },
        )
    }

    if (confirmDeleteProject) {
        ConfirmDialog(
            title = "Smazat projekt?",
            text = "Smažou se všichni klienti, produkty, platby, dluhy i účtenky tohoto projektu. Tuto akci nelze vrátit zpět.",
            confirmLabel = "Smazat projekt",
            onConfirm = {
                confirmDeleteProject = false
                viewModel.deleteProject(onDeleted)
            },
            onDismiss = { confirmDeleteProject = false },
        )
    }

    if (identificationKeyOpen) {
        IdentificationKeyDialog(
            key = identificationKey,
            onDismiss = { identificationKeyOpen = false },
        )
    }
}

/**
 * Identifikační klíč instalace. Je skrytý, dokud ho nezobrazíte. Jde jen prohlížet a kopírovat.
 * Úpravu ani přegenerování nenabízí, jinak by přestaly jít ověřit dřívější účtenky.
 */
@Composable
private fun IdentificationKeyDialog(key: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var revealed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Identifikační klíč") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Klíč šifruje účtenky a slouží k ověření BKP. Nikomu ho neukazujte. Záloha ZIP ho obsahuje, takže i soubor zálohy chraňte jako heslo.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                when {
                    key == null -> Text("Načítání…", style = MaterialTheme.typography.bodyMedium)
                    revealed -> SelectionContainer {
                        Text(key, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                    }
                    else -> Text("•".repeat(20), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = { revealed = !revealed }, enabled = key != null) {
                    Text(if (revealed) "Skrýt" else "Zobrazit")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    key?.let { value ->
                        context.getSystemService(ClipboardManager::class.java)
                            ?.setPrimaryClip(ClipData.newPlainText("Identifikační klíč", value))
                        Toast.makeText(context, "Klíč zkopírován.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = key != null,
            ) {
                Text("Kopírovat")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zavřít") } },
    )
}
