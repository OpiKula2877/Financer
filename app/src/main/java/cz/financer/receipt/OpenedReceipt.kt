package cz.financer.receipt

/**
 * Účtenka otevřená z QR kódu nebo ručního zadání, která není uložená v tomto telefonu.
 * Předává se na obrazovku účtenky přes aplikaci, ne přes adresu.
 */
data class OpenedReceipt(
    val data: ReceiptData,
    val bkp: String,
    val payload: String,
)
