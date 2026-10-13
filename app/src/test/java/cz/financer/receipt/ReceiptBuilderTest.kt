package cz.financer.receipt

import cz.financer.data.Project
import cz.financer.data.SaleItem
import cz.financer.data.SaleTransaction
import cz.financer.data.SaleWithItems
import cz.financer.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptBuilderTest {

    private val project = Project(
        id = 1,
        name = "Projekt",
        ownerFirstName = "Jan",
        ownerLastName = "Novák",
        logoPath = null,
        createdAt = 0,
        taxpayerName = "   ",
        address = "Ulice 1",
        phone = null,
    )

    private val sale = SaleWithItems(
        sale = SaleTransaction(id = 7, projectId = 1, clientId = 3, type = TransactionType.DEBT, createdAt = 123, totalHalire = 2200),
        items = listOf(
            SaleItem(id = 1, transactionId = 7, productId = 1, productNameSnapshot = "Mýdlo", unitPriceHalire = 200, quantity = 5),
            SaleItem(id = 2, transactionId = 7, productId = 2, productNameSnapshot = "Ručník", unitPriceHalire = 300, quantity = 4),
        ),
    )

    @Test
    fun blankFieldsAreLeftOutOfTheReceipt() {
        val data = ReceiptBuilder.build(project, sale, clientName = "  Ana ", number = 278)

        assertNull(data.taxpayerName)
        assertEquals("Ulice 1", data.address)
        assertNull(data.phone)
        assertEquals("Ana", data.clientName)
        assertEquals(278L, data.number)
        assertEquals(123L, data.issuedAt)
    }

    @Test
    fun receiptLinesMatchTheStoredTotal() {
        val data = ReceiptBuilder.build(project, sale, clientName = null, number = 1)

        assertEquals(sale.sale.totalHalire, data.lines.sumOf { it.totalHalire })
    }

    @Test
    fun typeLabelsMatchTheReceiptTypes() {
        assertEquals("Platba", ReceiptBuilder.typeLabel(TransactionType.PAYMENT))
        assertEquals("Na dluh", ReceiptBuilder.typeLabel(TransactionType.DEBT))
        assertEquals("Splacení dluhu", ReceiptBuilder.typeLabel(TransactionType.REPAYMENT))
    }
}
