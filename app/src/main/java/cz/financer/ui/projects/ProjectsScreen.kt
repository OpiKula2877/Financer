package cz.financer.ui.projects

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.financer.data.Project
import cz.financer.ui.components.EmptyState
import cz.financer.ui.components.LocalImage
import cz.financer.ui.components.UriImage
import cz.financer.ui.financer
import cz.financer.ui.viewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(onOpenProject: (Long) -> Unit) {
    val context = LocalContext.current
    val app = context.financer()
    val viewModel: ProjectsViewModel = viewModel(factory = viewModelFactory { ProjectsViewModel(app) })
    val projects by viewModel.projects.collectAsState()
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { app.backup.export(uri) }
                    .onSuccess { Toast.makeText(context, "Záloha uložena.", Toast.LENGTH_SHORT).show() }
                    .onFailure { Toast.makeText(context, "Zálohu se nepodařilo uložit: ${it.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { app.backup.import(uri) }
                    .onSuccess { Toast.makeText(context, "Data obnovena ze zálohy.", Toast.LENGTH_SHORT).show() }
                    .onFailure { Toast.makeText(context, "Obnovení selhalo: ${it.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financer", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { exportLauncher.launch("financer-zaloha.zip") }) {
                        Icon(Icons.Filled.Backup, contentDescription = "Exportovat zálohu")
                    }
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }) {
                        Icon(Icons.Filled.Restore, contentDescription = "Obnovit zálohu")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Vytvořit projekt") },
            )
        },
    ) { padding ->
        if (projects.isEmpty()) {
            EmptyState(
                "Zatím nemáte žádný projekt. Vytvořte první tlačítkem Vytvořit projekt.",
                Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectCard(project, onClick = { onOpenProject(project.id) })
                }
            }
        }
    }

    if (creating) {
        CreateProjectDialog(
            onConfirm = { name, first, last, logo ->
                viewModel.createProject(name, first, last, logo) { id ->
                    creating = false
                    onOpenProject(id)
                }
            },
            onDismiss = { creating = false },
        )
    }
}

@Composable
private fun ProjectCard(project: Project, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))) {
                LocalImage(project.logoPath, Modifier.fillMaxSize())
            }
            Column(Modifier.padding(start = 16.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${project.ownerFirstName} ${project.ownerLastName}".trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onConfirm: (name: String, first: String, last: String, logo: Uri?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var logo by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) logo = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nový projekt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))) {
                        val picked = logo
                        if (picked != null) {
                            UriImage(picked, Modifier.fillMaxSize())
                        } else {
                            Box(
                                Modifier.fillMaxSize().clickable {
                                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = "Vybrat logo")
                            }
                        }
                    }
                    OutlinedButton(onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text(if (logo == null) "Logo (volitelné)" else "Změnit logo")
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Název projektu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = first,
                    onValueChange = { first = it },
                    label = { Text("Jméno") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = last,
                    onValueChange = { last = it },
                    label = { Text("Příjmení") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, first, last, logo) },
                enabled = name.isNotBlank(),
            ) { Text("Potvrdit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}
