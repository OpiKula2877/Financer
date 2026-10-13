package cz.financer.receipt

import cz.financer.data.Project
import cz.financer.data.SaleWithItems
import cz.financer.data.TransactionType

/** Sestaví obsah účtenky z uloženého záznamu. Číslo přiděluje repozitář v jedné transakci. */
object ReceiptBuilder {

    fun build(project: Project, sale: SaleWithItems, clientName: String?, number: Long): ReceiptData =
        ReceiptData(
            number = number,
            issuedAt = sale.sale.createdAt,
            type = sale.sale.type,
            taxpayerName = project.taxpayerName.orNullIfBlank(),
            address = project.address.orNullIfBlank(),
            phone = project.phone.orNullIfBlank(),
            clientName = clientName.orNullIfBlank(),
            lines = sale.items.map { ReceiptLine(it.productNameSnapshot, it.quantity, it.unitPriceHalire) },
            totalHalire = sale.sale.totalHalire,
            layout = ReceiptLayout.fromJson(project.receiptLayout),
        )

    fun typeLabel(type: TransactionType): String = when (type) {
        TransactionType.PAYMENT -> "Platba"
        TransactionType.DEBT -> "Na dluh"
        TransactionType.REPAYMENT -> "Splacení dluhu"
    }

    private fun String?.orNullIfBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
