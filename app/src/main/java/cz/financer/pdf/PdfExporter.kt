package cz.financer.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import cz.financer.data.Project
import cz.financer.data.SaleWithItems
import cz.financer.data.TransactionType
import cz.financer.domain.Money
import cz.financer.domain.Statistics
import cz.financer.ui.components.formatDate
import cz.financer.ui.components.formatDateTime
import java.io.File
import java.time.ZonedDateTime

enum class PdfPeriod(val label: String) {
    LAST_MONTH("Poslední měsíc"),
    LAST_YEAR("Poslední rok"),
    ALL("Vše"),
}

enum class PdfColumn(val label: String) {
    DATE("Datum"),
    CLIENT("Jméno klienta"),
    TYPE("Typ"),
    VALUE("Hodnota"),
    ITEMS("Položky"),
}

data class PdfRequest(
    val period: PdfPeriod,
    /** null = všichni klienti. */
    val clientId: Long?,
    val columns: Set<PdfColumn>,
)

/**
 * Vytvoří výpis jako PDF (A4) nativním PdfDocument, bez externí knihovny.
 * Tabulka se stránkuje a na každé stránce se opakuje hlavička.
 */
class PdfExporter(private val context: Context) {

    fun export(
        project: Project,
        sales: List<SaleWithItems>,
        clientNames: Map<Long, String>,
        request: PdfRequest,
    ): File {
        val now = ZonedDateTime.now()
        val from = when (request.period) {
            PdfPeriod.LAST_MONTH -> now.minusMonths(1).toInstant().toEpochMilli()
            PdfPeriod.LAST_YEAR -> now.minusYears(1).toInstant().toEpochMilli()
            PdfPeriod.ALL -> null
        }
        val rows = sales
            .filter { from == null || it.sale.createdAt >= from }
            .filter { request.clientId == null || it.sale.clientId == request.clientId }
            .sortedBy { it.sale.createdAt }
        val columns = PdfColumn.entries.filter { it in request.columns }.ifEmpty { listOf(PdfColumn.DATE) }

        val debt = if (request.clientId != null) {
            cz.financer.domain.SaleCalculator.debtOf(request.clientId, sales)
        } else {
            Statistics.totalDebt(sales)
        }
        val sumSales = rows.filter { it.sale.type != TransactionType.REPAYMENT }.sumOf { it.sale.totalHalire }

        val document = PdfDocument()
        val writer = PageWriter(document)
        val logo = project.logoPath?.let { BitmapFactory.decodeFile(it) }

        writer.newPage()
        drawHeader(writer, project, request, logo, now.toInstant().toEpochMilli())
        drawTableHeader(writer, columns)

        rows.forEach { row ->
            writer.ensureSpace(ROW_HEIGHT) {
                drawTableHeader(writer, columns)
            }
            drawRow(writer, columns, row, clientNames)
        }

        writer.ensureSpace(FOOTER_HEIGHT) { }
        drawFooter(writer, sumSales, debt, request.clientId != null)
        writer.finish()

        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = project.name.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "projekt" }
        val file = File(directory, "vypis_${safeName}_${now.toLocalDate()}.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun drawHeader(
        writer: PageWriter,
        project: Project,
        request: PdfRequest,
        logo: Bitmap?,
        generatedAt: Long,
    ) {
        val canvas = writer.canvas
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f; isFakeBoldText = true; color = Color.rgb(30, 58, 95) }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = Color.DKGRAY }

        val logoSize = 64f
        if (logo != null) {
            val scaled = Bitmap.createScaledBitmap(logo, logoSize.toInt(), logoSize.toInt(), true)
            canvas.drawBitmap(scaled, PAGE_WIDTH - MARGIN - logoSize, MARGIN, null)
        }

        canvas.drawText(project.name, MARGIN, writer.y + 20f, title)
        val owner = "${project.ownerFirstName} ${project.ownerLastName}".trim()
        if (owner.isNotEmpty()) canvas.drawText(owner, MARGIN, writer.y + 38f, text)
        canvas.drawText("Období: ${request.period.label}", MARGIN, writer.y + 54f, text)
        canvas.drawText("Vygenerováno: ${formatDateTime(generatedAt)}", MARGIN, writer.y + 68f, text)

        writer.y += 96f
        val line = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        canvas.drawLine(MARGIN, writer.y, PAGE_WIDTH - MARGIN, writer.y, line)
        writer.y += 12f
    }

    private fun drawTableHeader(writer: PageWriter, columns: List<PdfColumn>) {
        val canvas = writer.canvas
        val fill = Paint().apply { color = Color.rgb(30, 58, 95) }
        canvas.drawRect(MARGIN, writer.y, PAGE_WIDTH - MARGIN, writer.y + ROW_HEIGHT, fill)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; isFakeBoldText = true; color = Color.WHITE }
        val widths = columnWidths(columns)
        var x = MARGIN + CELL_PADDING
        columns.forEachIndexed { index, column ->
            canvas.drawText(column.label, x, writer.y + 14f, textPaint)
            x += widths[index]
        }
        writer.y += ROW_HEIGHT
    }

    private fun drawRow(
        writer: PageWriter,
        columns: List<PdfColumn>,
        row: SaleWithItems,
        clientNames: Map<Long, String>,
    ) {
        val canvas = writer.canvas
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = Color.BLACK }
        val divider = Paint().apply { color = Color.rgb(220, 224, 230); strokeWidth = 0.7f }
        val widths = columnWidths(columns)
        var x = MARGIN + CELL_PADDING
        columns.forEachIndexed { index, column ->
            val value = when (column) {
                PdfColumn.DATE -> formatDate(row.sale.createdAt)
                PdfColumn.CLIENT -> clientNames[row.sale.clientId] ?: "—"
                PdfColumn.TYPE -> when (row.sale.type) {
                    TransactionType.PAYMENT -> "Platba"
                    TransactionType.DEBT -> "Dluh"
                    TransactionType.REPAYMENT -> "Splacení"
                }
                PdfColumn.VALUE -> Money.format(row.sale.totalHalire)
                PdfColumn.ITEMS -> row.items.joinToString(", ") { "${it.quantity}× ${it.productNameSnapshot}" }
            }
            val fitted = TextUtils.ellipsize(value, textPaint, widths[index] - CELL_PADDING, TextUtils.TruncateAt.END)
            canvas.drawText(fitted, 0, fitted.length, x, writer.y + 14f, textPaint)
            x += widths[index]
        }
        writer.y += ROW_HEIGHT
        canvas.drawLine(MARGIN, writer.y, PAGE_WIDTH - MARGIN, writer.y, divider)
    }

    private fun drawFooter(writer: PageWriter, sumSales: Long, debt: Long, singleClient: Boolean) {
        val canvas = writer.canvas
        writer.y += 16f
        val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; isFakeBoldText = true; color = Color.BLACK }
        canvas.drawText("Tržby za výpis: ${Money.format(sumSales)}", MARGIN, writer.y, bold)
        writer.y += 18f
        val debtLabel = if (singleClient) "Dluh klienta (aktuální)" else "Celkový dluh všech (aktuální)"
        canvas.drawText("$debtLabel: ${Money.format(debt)}", MARGIN, writer.y, bold)
        writer.y += 60f
        val signature = Paint().apply { color = Color.GRAY; strokeWidth = 1f }
        canvas.drawLine(MARGIN, writer.y, MARGIN + 200f, writer.y, signature)
        canvas.drawText("Podpis", MARGIN, writer.y + 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = Color.GRAY })
    }

    private fun columnWidths(columns: List<PdfColumn>): List<Float> {
        val total = PAGE_WIDTH - 2 * MARGIN
        // Hodnota a položky mají víc místa než datum.
        val weights = columns.map {
            when (it) {
                PdfColumn.DATE -> 1.1f
                PdfColumn.CLIENT -> 1.5f
                PdfColumn.TYPE -> 1f
                PdfColumn.VALUE -> 1.2f
                PdfColumn.ITEMS -> 2.6f
            }
        }
        val sum = weights.sum()
        return weights.map { total * it / sum }
    }

    /** Správa stránek a kurzoru y. */
    private class PageWriter(private val document: PdfDocument) {
        private var pageNumber = 0
        private lateinit var page: PdfDocument.Page
        lateinit var canvas: android.graphics.Canvas
        var y = MARGIN

        fun newPage() {
            if (pageNumber > 0) document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageNumber).create())
            canvas = page.canvas
            y = MARGIN
        }

        /** Pokud se prvek nevejde, přejde na novou stránku a zavolá [onNewPage]. */
        fun ensureSpace(height: Float, onNewPage: () -> Unit) {
            if (y + height > PAGE_HEIGHT - MARGIN) {
                newPage()
                onNewPage()
            }
        }

        fun finish() {
            document.finishPage(page)
        }
    }

    private companion object {
        const val PAGE_WIDTH = 595f
        const val PAGE_HEIGHT = 842f
        const val MARGIN = 40f
        const val ROW_HEIGHT = 22f
        const val CELL_PADDING = 6f
        const val FOOTER_HEIGHT = 120f
    }
}
