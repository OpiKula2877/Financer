package cz.financer.receipt

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import cz.financer.domain.Money
import kotlin.math.ceil

/** Účtenka připravená k vykreslení: data, BKP, stav storna a QR kód, pokud se vytvořil. */
class ReceiptArt(
    val data: ReceiptData,
    val bkp: String,
    val cancelled: Boolean,
    val qr: Bitmap?,
    val qrHasFullPayload: Boolean,
)

/**
 * Černobílá účtenka 80 mm (226 bodů) vykreslená do Canvas. Stejný kód plní PDF i náhled,
 * takže vzhled je všude stejný. Výška se nejdřív změří průchodem bez Canvas.
 */
object ReceiptRenderer {
    const val WIDTH = 226f
    private const val MARGIN = 12f
    private const val CONTENT_WIDTH = WIDTH - 2 * MARGIN
    private const val QR_SIZE = 120f

    fun height(art: ReceiptArt): Float = draw(null, art)

    /** Vykreslí účtenku do canvas (nebo jen spočítá výšku, pokud je canvas null). Vrací výšku. */
    fun draw(canvas: Canvas?, art: ReceiptArt): Float {
        val pen = Pen(canvas)
        val data = art.data
        val layout = data.layout

        if (layout.showHeader) {
            data.taxpayerName?.let { pen.centered(it, 12f, bold = true) }
            data.address?.let { pen.centered(it, 9f) }
            data.phone?.let { pen.centered("Telefon: $it", 9f) }
            pen.space(4f)
        }
        if (art.cancelled) pen.stamp("STORNOVÁNO")
        pen.divider()
        if (layout.showDateTime) {
            pen.row("Datum: ${ReceiptFormat.date(data.issuedAt)}", "Čas: ${ReceiptFormat.time(data.issuedAt)}", 9f)
        }
        if (layout.showNumber) pen.row("Číslo účtenky:", ReceiptFormat.number(data.number), 9f, bold = true)
        if (layout.showType) pen.row("Typ:", ReceiptBuilder.typeLabel(data.type), 9f, bold = true)
        if (layout.showClient) data.clientName?.let { pen.row("Klient:", it, 9f) }
        pen.divider()
        if (layout.showItems && data.lines.isNotEmpty()) {
            data.lines.forEach { pen.item(it, layout.showQuantities) }
            pen.divider()
        }
        if (layout.showTotal) {
            pen.box("Celková částka: ${Money.format(data.totalHalire)}", 13f)
            pen.space(6f)
        }
        if (layout.showBkp) {
            pen.divider()
            pen.centered("BKP", 9f, bold = true)
            pen.centered(art.bkp, 7f, mono = true)
            pen.space(6f)
        }
        if (layout.showQr) art.qr?.let { pen.qr(it, art.qrHasFullPayload) }
        return pen.y + MARGIN
    }

    /** Náhled účtenky jako bitmapa. Scale 2 dá ostrý text na obrazovce. */
    fun toBitmap(art: ReceiptArt, scale: Float = 2f): Bitmap {
        val height = ceil(height(art) * scale).toInt()
        val bitmap = Bitmap.createBitmap((WIDTH * scale).toInt(), height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.scale(scale, scale)
        draw(canvas, art)
        return bitmap
    }

    private fun paint(size: Float, bold: Boolean = false, mono: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            color = Color.BLACK
            isFakeBoldText = bold
            typeface = if (mono) Typeface.MONOSPACE else Typeface.DEFAULT
            textAlign = align
        }

    /** Zalomí text podle slov tak, aby se vešel do šířky. */
    private fun wrap(value: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        var current = ""
        value.split(" ").filter { it.isNotEmpty() }.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lines += current
                current = word
            }
        }
        if (current.isNotEmpty()) lines += current
        return lines
    }

    /** Kurzor po řádcích účtenky. Při canvas null jen posouvá y, takže měří stejně jako kreslí. */
    private class Pen(private val canvas: Canvas?) {
        var y = MARGIN

        private val dashed = Paint().apply {
            color = Color.BLACK
            strokeWidth = 0.8f
            pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
        }
        private val frame = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        fun space(height: Float) {
            y += height
        }

        fun divider() {
            y += 4f
            canvas?.drawLine(MARGIN, y, WIDTH - MARGIN, y, dashed)
            y += 8f
        }

        fun centered(value: String, size: Float, bold: Boolean = false, mono: Boolean = false) {
            val paint = paint(size, bold, mono, Paint.Align.CENTER)
            wrap(value, paint, CONTENT_WIDTH).forEach { line(it, WIDTH / 2, size, paint) }
        }

        fun row(left: String, right: String, size: Float, bold: Boolean = false) {
            y += size
            canvas?.drawText(left, MARGIN, y, paint(size, bold))
            canvas?.drawText(right, WIDTH - MARGIN, y, paint(size, bold, align = Paint.Align.RIGHT))
            y += 3f
        }

        /** Položka: název vlevo, cena vpravo, pod tím množství a cena za kus. */
        fun item(item: ReceiptLine, showQuantities: Boolean) {
            val total = Money.format(item.totalHalire)
            val totalPaint = paint(9f, bold = true, align = Paint.Align.RIGHT)
            val namePaint = paint(9f, bold = true)
            val nameLines = wrap(item.name, namePaint, CONTENT_WIDTH - totalPaint.measureText(total) - 6f)
                .ifEmpty { listOf("") }
            nameLines.forEachIndexed { index, text ->
                y += 9f
                canvas?.drawText(text, MARGIN, y, namePaint)
                if (index == 0) canvas?.drawText(total, WIDTH - MARGIN, y, totalPaint)
                y += 3f
            }
            if (showQuantities) {
                line("${item.quantity} × ${Money.format(item.unitPriceHalire)}", MARGIN, 8f, paint(8f))
            }
            y += 4f
        }

        /** Celková částka v rámečku, uprostřed. */
        fun box(value: String, size: Float) {
            val top = y
            val height = size + 14f
            canvas?.drawRect(MARGIN, top, WIDTH - MARGIN, top + height, frame)
            y = top + 7f + size
            canvas?.drawText(value, WIDTH / 2, y, paint(size, bold = true, align = Paint.Align.CENTER))
            y = top + height + 3f
        }

        fun stamp(value: String) {
            val top = y
            canvas?.drawRect(MARGIN, top, WIDTH - MARGIN, top + STAMP_HEIGHT, frame)
            canvas?.drawText(value, WIDTH / 2, top + 20f, paint(14f, bold = true, align = Paint.Align.CENTER))
            y = top + STAMP_HEIGHT + 6f
        }

        fun qr(bitmap: Bitmap, fullPayload: Boolean) {
            val left = (WIDTH - QR_SIZE) / 2
            canvas?.drawBitmap(bitmap, null, RectF(left, y, left + QR_SIZE, y + QR_SIZE), null)
            y += QR_SIZE + 4f
            if (!fullPayload) centered("QR kód obsahuje jen BKP, celá účtenka se do něj nevešla.", 7f)
            y += 4f
        }

        private fun line(value: String, x: Float, size: Float, paint: Paint) {
            y += size
            canvas?.drawText(value, x, y, paint)
            y += 3f
        }

        private companion object {
            const val STAMP_HEIGHT = 30f
        }
    }
}
