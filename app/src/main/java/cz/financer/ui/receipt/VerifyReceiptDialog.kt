package cz.financer.ui.receipt

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import cz.financer.receipt.OpenedReceipt
import cz.financer.receipt.ReceiptQrPayload
import cz.financer.security.ReceiptCrypto
import cz.financer.ui.financer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Ověření účtenky. BKP a klíč najdou účtenku v tomto telefonu. QR kód nese celou zašifrovanou
 * účtenku, takže ji lze otevřít i z jiného telefonu, pokud v něm zadáte klíč.
 */
@Composable
fun VerifyReceiptDialog(
    onDismiss: () -> Unit,
    onOpenStored: (Long) -> Unit,
    onOpenScanned: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.financer()
    val scope = rememberCoroutineScope()
    var bkp by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        key = app.repository.identificationKey()
    }

    fun lookupStored(code: String, secret: String) {
        scope.launch {
            val receipt = app.repository.findReceiptByBkp(code)
            if (receipt == null) {
                error = "Účtenka s tímto BKP v telefonu není. Naskenujte QR kód z účtenky."
                return@launch
            }
            val opened = withContext(Dispatchers.IO) {
                runCatching { app.repository.openReceipt(secret, receipt.bkp, receipt.payload) }
            }
            if (opened.isSuccess) {
                onOpenStored(receipt.id)
            } else {
                error = "Klíč nesedí, účtenku nejde otevřít."
            }
        }
    }

    fun openScanned(parsedBkp: String, payload: String, secret: String) {
        scope.launch {
            val opened = withContext(Dispatchers.IO) {
                runCatching { app.repository.openReceipt(secret, parsedBkp, payload) }
            }
            if (opened.isSuccess) {
                app.openedReceipt = OpenedReceipt(opened.getOrThrow(), parsedBkp, payload)
                onOpenScanned()
            } else {
                error = "Klíč nesedí, účtenku nejde otevřít."
            }
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val text = result.contents ?: return@rememberLauncherForActivityResult
        val parsed = ReceiptQrPayload.parse(text)
        val secret = key.trim()
        when {
            parsed == null -> error = "Naskenovaný kód není účtenka Financeru."
            !ReceiptCrypto.isValidKey(secret) -> error = "Nejdřív zadejte identifikační klíč."
            parsed.payload == null -> {
                bkp = parsed.bkp
                lookupStored(parsed.bkp, secret)
            }
            else -> openScanned(parsed.bkp, parsed.payload, secret)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            scanLauncher.launch(scanOptions())
        } else {
            error = "Bez přístupu ke kameře nejde účtenku naskenovat."
        }
    }

    fun startScan() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            scanLauncher.launch(scanOptions())
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ověřit účtenku") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Zadejte BKP a identifikační klíč, nebo naskenujte QR kód účtenky.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = bkp,
                    onValueChange = {
                        bkp = it.uppercase()
                        error = null
                    },
                    label = { Text("BKP") },
                    placeholder = { Text("XXXXXXXX-XXXXXXXX-XXXXXXXX-XXXXXXXX-XXXXXXXX") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = {
                        key = it
                        error = null
                    },
                    label = { Text("Identifikační klíč") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { startScan() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Naskenovat QR kód")
                }
                error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { lookupStored(bkp.trim(), key.trim()) },
                enabled = ReceiptCrypto.isValidBkp(bkp.trim()) && ReceiptCrypto.isValidKey(key.trim()),
            ) {
                Text("Najít v telefonu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}

private fun scanOptions(): ScanOptions = ScanOptions()
    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
    .setPrompt("Namiřte kameru na QR kód účtenky")
    .setBeepEnabled(false)
