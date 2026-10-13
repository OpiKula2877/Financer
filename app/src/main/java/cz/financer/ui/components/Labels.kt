package cz.financer.ui.components

import cz.financer.data.SaleTransaction
import cz.financer.data.TransactionType

fun TransactionType.czechLabel(): String = when (this) {
    TransactionType.PAYMENT -> "Platba"
    TransactionType.DEBT -> "Dluh"
    TransactionType.REPAYMENT -> "Splacení dluhu"
}

/** Název typu záznamu pro historii a archiv. Ručně zadané záznamy jsou označené „Nastavený ručně“. */
fun SaleTransaction.displayLabel(): String = when {
    isManual && type == TransactionType.DEBT -> "Nastavený ručně – dluh"
    isManual && type == TransactionType.REPAYMENT -> "Nastavený ručně – zaplaceno"
    else -> type.czechLabel()
}
