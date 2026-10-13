package cz.financer.ui.receipt

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import cz.financer.FinancerApplication
import cz.financer.receipt.ReceiptArt
import cz.financer.receipt.ReceiptData
import cz.financer.receipt.ReceiptFormat
import cz.financer.receipt.ReceiptPdf
import cz.financer.receipt.ReceiptQr
import cz.financer.receipt.ReceiptRenderer
import cz.financer.ui.components.EmptyState
import cz.financer.ui.financer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val QR_PIXELS = 360

private sealed interface ReceiptState {
    data object Loading : ReceiptState
    class Ready(val art: ReceiptArt, val preview: Bitmap) : ReceiptState
    class Failed(val message: String) : ReceiptState
}

/** Odkud se účtenka načetla: z databáze (receiptId > 0) nebo z QR kódu (receiptId 0). */
private class Source(
    val data: ReceiptData,
    val bkp: String,
    val payload: String,
    val cancelled: Boolean,
)

/**
 * Zobrazení účtenky. Vystavená účtenka se otevírá podle ID, účtenka z QR kódu přes [FinancerApplication.openedReceipt].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(receiptId: Long, onDone: () -> Unit) {
    val context = LocalContext.current
    val app = context.financer()
    val scope = rememberCoroutineScope()
    val state by produceState<ReceiptState>(ReceiptState.Loading, receiptId) {
        value = withContext(Dispatchers.IO) { loadReceipt(app, receiptId) }
    }
    val current = state

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = (current as? ReceiptState.Ready)?.art?.data?.number
                    Text(if (title != null) "Účtenka č. ${ReceiptFormat.number(title)}" else "Účtenka", fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
            )
        },
    ) { padding ->
        when (current) {
            ReceiptState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            is ReceiptState.Failed -> EmptyState(current.message, Modifier.padding(padding))

            is ReceiptState.Ready -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    bitmap = current.preview.asImageBitmap(),
                    contentDescription = "Náhled účtenky",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(0.9f),
                )
                Text(
                    current.art.bkp,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { copyBkp(context, current.art.bkp) },
                )
                Text(
                    "Klepnutím na BKP ho zkopírujete.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Button(
                    onClick = {
                        scope.launch { sharePdf(context, current.art) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Sdílet PDF")
                }
                OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("Hotovo")
                }
            }
        }
    }
}

private suspend fun loadReceipt(app: FinancerApplication, receiptId: Long): ReceiptState {
    return try {
        val source = sourceOf(app, receiptId) ?: return ReceiptState.Failed("Účtenka už není k dispozici.")
        val qr = ReceiptQr.create(source.bkp, source.payload, QR_PIXELS)
        val art = ReceiptArt(source.data, source.bkp, source.cancelled, qr.bitmap, qr.hasFullPayload)
        ReceiptState.Ready(art, ReceiptRenderer.toBitmap(art))
    } catch (_: Exception) {
        ReceiptState.Failed("Účtenku nelze zobrazit. Zkontrolujte, že obsah účtenky není poškozený.")
    }
}

private suspend fun sourceOf(app: FinancerApplication, receiptId: Long): Source? {
    if (receiptId == 0L) {
        val opened = app.openedReceipt ?: return null
        return Source(opened.data, opened.bkp, opened.payload, cancelled = false)
    }
    val stored = app.repository.getReceipt(receiptId) ?: return null
    val data = app.repository.openReceipt(app.repository.identificationKey(), stored.bkp, stored.payload)
    return Source(data, stored.bkp, stored.payload, stored.cancelled)
}

private fun copyBkp(context: android.content.Context, bkp: String) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("BKP", bkp))
    Toast.makeText(context, "BKP zkopírováno.", Toast.LENGTH_SHORT).show()
}

private suspend fun sharePdf(context: android.content.Context, art: ReceiptArt) {
    val file = withContext(Dispatchers.IO) { ReceiptPdf(context).write(art) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Sdílet účtenku"))
}
