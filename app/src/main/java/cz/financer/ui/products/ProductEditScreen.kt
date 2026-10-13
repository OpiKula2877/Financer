package cz.financer.ui.products

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.financer.domain.Money
import cz.financer.ui.components.ConfirmDialog
import cz.financer.ui.components.LocalImage
import cz.financer.ui.components.UriImage
import cz.financer.ui.financer
import cz.financer.ui.project.ProjectViewModel
import cz.financer.ui.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    projectId: Long,
    productId: Long?,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.financer()
    val viewModel: ProjectViewModel = viewModel(
        key = "project-$projectId",
        factory = viewModelFactory { ProjectViewModel(app, projectId) },
    )
    val rows by viewModel.products.collectAsState()
    val existing = rows.firstOrNull { it.product.id == productId }?.product

    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var isSoldOut by remember { mutableStateOf(false) }
    var newImage by remember { mutableStateOf<Uri?>(null) }
    var loaded by remember { mutableStateOf(productId == null) }
    var confirmDelete by remember { mutableStateOf(false) }

    // Při úpravě se formulář předvyplní, jakmile přijdou data z databáze.
    LaunchedEffect(existing) {
        if (!loaded && existing != null) {
            name = existing.name
            priceText = Money.toEditable(existing.unitPriceHalire)
            isSoldOut = existing.isSoldOut
            loaded = true
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) newImage = uri
    }
    val pickImage = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    val priceHalire = Money.parse(priceText)
    val priceInvalid = priceText.isNotBlank() && (priceHalire == null || priceHalire <= 0)
    val canSave = name.isNotBlank() && priceHalire != null && priceHalire > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productId == null) "Nový produkt" else "Upravit produkt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Smazat produkt")
                        }
                    }
                },
            )
        },
        bottomBar = {
            // Tlačítko je vždy vidět dole, ne až za obsahem formuláře.
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = {
                        viewModel.saveProduct(
                            existing = existing,
                            name = name,
                            priceHalire = priceHalire ?: 0,
                            isSoldOut = isSoldOut,
                            newImage = newImage,
                            onSaved = onBack,
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text("Potvrdit")
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Obrázek 1:1, zmenšený na 70 % šířky, aby se formulář vešel na obrazovku.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { pickImage() },
                ) {
                    val picked = newImage
                    if (picked != null) {
                        UriImage(picked, Modifier.fillMaxSize())
                    } else {
                        LocalImage(existing?.imagePath, Modifier.fillMaxSize())
                    }
                }
            }
            OutlinedButton(onClick = pickImage, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Image, contentDescription = null)
                Text("  Vybrat obrázek (ořízne se na 1:1)")
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Název produktu") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Cena za kus") },
                suffix = { Text("CZK") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = priceInvalid,
                supportingText = {
                    if (priceInvalid) Text("Zadejte kladnou cenu, např. 2 nebo 2,50")
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isSoldOut, onCheckedChange = { isSoldOut = it })
                Text("Vyprodáno", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "Smazat produkt?",
            text = "Produkt „${existing.name}“ se odstraní. Už zapsané prodeje zůstanou v historii.",
            confirmLabel = "Smazat",
            onConfirm = {
                viewModel.deleteProduct(existing)
                confirmDelete = false
                onBack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
