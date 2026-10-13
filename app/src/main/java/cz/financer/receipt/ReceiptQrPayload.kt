package cz.financer.receipt

import cz.financer.security.ReceiptCrypto

/**
 * Text v QR kódu účtenky: `FIN1:<BKP>:<zašifrovaný obsah>`.
 * Obsah chybí, pokud se celá účtenka do QR kódu nevešla.
 */
object ReceiptQrPayload {
    private const val PREFIX = "FIN1"

    data class Parsed(val bkp: String, val payload: String?)

    fun content(bkp: String, payload: String?): String =
        if (payload == null) "$PREFIX:$bkp" else "$PREFIX:$bkp:$payload"

    /** Vrací null, pokud text není účtenka Financeru. */
    fun parse(text: String): Parsed? {
        val parts = text.trim().split(":", limit = 3)
        if (parts.size < 2 || parts[0] != PREFIX || !ReceiptCrypto.isValidBkp(parts[1])) return null
        return Parsed(parts[1], parts.getOrNull(2)?.takeIf { it.isNotEmpty() })
    }
}
