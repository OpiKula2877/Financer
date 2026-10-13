package cz.financer.security

import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Kryptografie účtenek. Používají se jen standardní algoritmy z javax.crypto:
 * HMAC-SHA256 pro BKP a AES-256-GCM pro zašifrovaný obsah. Z identifikačního klíče
 * se oddělenými popisky odvodí dva klíče, aby se podpis a šifra nepletly.
 */
object ReceiptCrypto {

    const val KEY_LENGTH = 50
    private const val KEY_ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,;:!?#$%&*+-=@_()[]"
    private const val NONCE_LENGTH = 12
    private const val TAG_BITS = 128
    private const val BUFFER_SIZE = 1024
    private const val MAC_LABEL = "financer-bkp-mac"
    private const val ENC_LABEL = "financer-payload-enc"
    private val bkpPattern = Regex("^[0-9A-F]{8}(-[0-9A-F]{8}){4}$")
    private val random = SecureRandom()

    /** Náhodný identifikační klíč. Vytváří se jen jednou, při prvním spuštění aplikace. */
    fun generateKey(): String =
        (0 until KEY_LENGTH).map { KEY_ALPHABET[random.nextInt(KEY_ALPHABET.length)] }.joinToString("")

    fun isValidKey(key: String): Boolean =
        key.length == KEY_LENGTH && key.all { it in KEY_ALPHABET }

    fun isValidBkp(bkp: String): Boolean = bkpPattern.matches(bkp)

    /** BKP = prvních 40 hex znaků HMAC obsahu účtenky, ve tvaru 8-8-8-8-8 (44 znaků). */
    fun bkp(key: String, canonicalJson: String): String {
        val digest = hmac(derive(key, MAC_LABEL), canonicalJson.toByteArray(Charsets.UTF_8))
        val hex = digest.take(20).joinToString("") { "%02X".format(it.toInt() and 0xFF) }
        return hex.chunked(8).joinToString("-")
    }

    /** Zašifruje obsah účtenky. Výstup je base64url a obsahuje IV i autentizační značku. */
    fun encrypt(key: String, canonicalJson: String): String {
        val iv = ByteArray(NONCE_LENGTH).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(derive(key, ENC_LABEL), "AES"), GCMParameterSpec(TAG_BITS, iv))
        val body = cipher.doFinal(deflate(canonicalJson.toByteArray(Charsets.UTF_8)))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(iv + body)
    }

    /** Vyhodí výjimku, pokud klíč nesedí nebo byl payload změněn (GCM kontroluje integritu). */
    fun decrypt(key: String, token: String): String {
        val bytes = Base64.getUrlDecoder().decode(token)
        require(bytes.size > NONCE_LENGTH) { "Zašifrovaný obsah je příliš krátký." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = bytes.copyOfRange(0, NONCE_LENGTH)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(derive(key, ENC_LABEL), "AES"), GCMParameterSpec(TAG_BITS, iv))
        val plain = inflate(cipher.doFinal(bytes.copyOfRange(NONCE_LENGTH, bytes.size)))
        return String(plain, Charsets.UTF_8)
    }

    private fun derive(key: String, label: String): ByteArray =
        hmac(key.toByteArray(Charsets.UTF_8), label.toByteArray(Charsets.UTF_8))

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun deflate(input: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setInput(input)
        deflater.finish()
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        try {
            while (!deflater.finished()) {
                output.write(buffer, 0, deflater.deflate(buffer))
            }
        } finally {
            deflater.end()
        }
        return output.toByteArray()
    }

    private fun inflate(input: ByteArray): ByteArray {
        val inflater = Inflater(true)
        inflater.setInput(input)
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        try {
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                check(count > 0 || !inflater.needsInput()) { "Zašifrovaný obsah je neúplný." }
                output.write(buffer, 0, count)
            }
        } finally {
            inflater.end()
        }
        return output.toByteArray()
    }
}
