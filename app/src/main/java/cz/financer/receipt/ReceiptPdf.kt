package cz.financer.receipt

import android.content.Context
import android.graphics.pdf.PdfDocument
import java.io.File
import kotlin.math.ceil

/** Účtenka jako jednostránkový PDF soubor v cache složce pro sdílení přes FileProvider. */
class ReceiptPdf(private val context: Context) {

    fun write(art: ReceiptArt): File {
        val pageHeight = ceil(ReceiptRenderer.height(art)).toInt()
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(ReceiptRenderer.WIDTH.toInt(), pageHeight, 1).create())
        ReceiptRenderer.draw(page.canvas, art)
        document.finishPage(page)

        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(directory, "uctenka_${ReceiptFormat.number(art.data.number)}.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }
}
