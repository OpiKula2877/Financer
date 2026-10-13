package cz.financer.domain

import cz.financer.data.SaleItem
import cz.financer.data.SaleTransaction
import cz.financer.data.SaleWithItems
import cz.financer.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class SaleCalculatorTest {

    private fun sale(
        id: Long,
        clientId: Long,
        type: TransactionType,
        total: Long,
        items: List<SaleItem> = emptyList(),
    ) = SaleWithItems(
        sale = SaleTransaction(id = id, projectId = 1, clientId = clientId, type = type, createdAt = id, totalHalire = total),
        items = items,
    )

    private fun item(transactionId: Long, productId: Long, quantity: Int) = SaleItem(
        transactionId = transactionId,
        productId = productId,
        productNameSnapshot = "p$productId",
        unitPriceHalire = 0,
        quantity = quantity,
    )

    @Test
    fun cartTotal_fiveSoapsAndFourTowels_is22Kc() {
        val lines = listOf(
            CartLine(productId = 1, productName = "Mýdlo", unitPriceHalire = 200, quantity = 5),
            CartLine(productId = 2, productName = "Ručník", unitPriceHalire = 300, quantity = 4),
        )
        assertEquals(2200L, SaleCalculator.cartTotal(lines))
        assertEquals("22 CZK", Money.format(SaleCalculator.cartTotal(lines)))
    }

    @Test
    fun debtOf_isDebtsMinusRepayments() {
        val sales = listOf(
            sale(1, clientId = 7, type = TransactionType.DEBT, total = 2500),
            sale(2, clientId = 7, type = TransactionType.REPAYMENT, total = 1000),
            sale(3, clientId = 8, type = TransactionType.DEBT, total = 900),
        )
        assertEquals(1500L, SaleCalculator.debtOf(7, sales))
    }

    @Test
    fun earnedFrom_countsPaymentsAndDebtsButNotRepayments() {
        val sales = listOf(
            sale(1, clientId = 7, type = TransactionType.PAYMENT, total = 2200),
            sale(2, clientId = 7, type = TransactionType.DEBT, total = 500),
            sale(3, clientId = 7, type = TransactionType.REPAYMENT, total = 1000),
        )
        assertEquals(2700L, SaleCalculator.earnedFrom(7, sales))
    }

    @Test
    fun debtShares_twentyFiveAndSeventyFive_giveTwentyFivePercentAndSeventyFivePercent() {
        val shares = SaleCalculator.debtShares(mapOf(1L to 2500L, 2L to 7500L))
        assertEquals(25.0, shares.getValue(1L), 0.0001)
        assertEquals(75.0, shares.getValue(2L), 0.0001)
    }

    @Test
    fun debtorBalances_leavesOutClientsWithoutDebt() {
        val sales = listOf(
            sale(1, clientId = 1, type = TransactionType.DEBT, total = 2500),
            sale(2, clientId = 2, type = TransactionType.DEBT, total = 1000),
            sale(3, clientId = 2, type = TransactionType.REPAYMENT, total = 1000),
        )
        assertEquals(mapOf(1L to 2500L), SaleCalculator.debtorBalances(listOf(1L, 2L, 3L), sales))
    }

    @Test
    fun soldQuantity_ignoresRepayments() {
        val sales = listOf(
            sale(1, clientId = 1, type = TransactionType.PAYMENT, total = 0, items = listOf(item(1, productId = 5, quantity = 3))),
            sale(2, clientId = 1, type = TransactionType.DEBT, total = 0, items = listOf(item(2, productId = 5, quantity = 2))),
            sale(3, clientId = 1, type = TransactionType.REPAYMENT, total = 100, items = listOf(item(3, productId = 5, quantity = 9))),
        )
        assertEquals(5, SaleCalculator.soldQuantity(5, sales))
    }

    @Test
    fun money_parseAndFormat() {
        assertEquals(250L, Money.parse("2,5"))
        assertEquals(null, Money.parse("abc"))
        assertEquals("1 250 CZK", Money.format(125000))
        assertEquals("2,50 CZK", Money.format(250))
    }
}
