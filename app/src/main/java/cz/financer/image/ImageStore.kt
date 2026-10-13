package cz.financer.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.min

/**
 * Ukládá obrázky produktů a log projektů do interního úložiště aplikace.
 * Každý obrázek se ořízne na čtverec 1:1 a zmenší na [OUTPUT_SIZE] px.
 */
class ImageStore(private val context: Context) {

    private val directory: File = File(context.filesDir, "images").apply { mkdirs() }

    /** Zkopíruje obrázek z galerie, ořízne ho na čtverec a vrátí cestu k uloženému souboru. */
    fun saveSquare(source: Uri): String? {
        val temp = File(context.cacheDir, "import-${UUID.randomUUID()}")
        try {
            context.contentResolver.openInputStream(source)?.use { input ->
                temp.outputStream().use { input.copyTo(it) }
            } ?: return null

            val decoded = decodeDownsampled(temp.absolutePath) ?: return null
            val oriented = applyExifOrientation(decoded, temp)
            val square = centerCropSquare(oriented)
            val scaled = Bitmap.createScaledBitmap(square, OUTPUT_SIZE, OUTPUT_SIZE, true)

            val target = File(directory, "${UUID.randomUUID()}.jpg")
            FileOutputStream(target).use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            return target.absolutePath
        } finally {
            temp.delete()
        }
    }

    fun delete(path: String?) {
        if (path != null) File(path).delete()
    }

    private fun decodeDownsampled(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val largest = maxOf(bounds.outWidth, bounds.outHeight)
        if (largest <= 0) return null

        var sample = 1
        while (largest / (sample * 2) >= MAX_DECODE_SIZE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(path, options)
    }

    private fun applyExifOrientation(bitmap: Bitmap, file: File): Bitmap {
        val orientation = ExifInterface(file.absolutePath)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun centerCropSquare(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }

    companion object {
        const val OUTPUT_SIZE = 512
        private const val MAX_DECODE_SIZE = 1024
    }
}
