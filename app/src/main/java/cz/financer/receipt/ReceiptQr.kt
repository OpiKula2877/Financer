package cz.financer.receipt

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** QR kód účtenky. Pokud se celá účtenka nevejde, vytvoří se jen s BKP. */
object ReceiptQr {

    /** Bitmapa QR kódu a informace, jestli obsahuje celou zašifrovanou účtenku. */
    class Code(val bitmap: Bitmap, val hasFullPayload: Boolean)

    fun create(bkp: String, payload: String, sizePx: Int): Code {
        val full = encode(ReceiptQrPayload.content(bkp, payload), sizePx)
        if (full != null) return Code(full, true)
        val short = encode(ReceiptQrPayload.content(bkp, null), sizePx) ?: error("QR kód nelze vytvořit.")
        return Code(short, false)
    }

    private fun encode(text: String, sizePx: Int): Bitmap? {
        val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L, EncodeHintType.MARGIN to 0)
        return try {
            val matrix: BitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val pixels = IntArray(matrix.width * matrix.height) { i ->
                if (matrix.get(i % matrix.width, i / matrix.width)) Color.BLACK else Color.WHITE
            }
            Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        } catch (_: WriterException) {
            null
        }
    }
}
