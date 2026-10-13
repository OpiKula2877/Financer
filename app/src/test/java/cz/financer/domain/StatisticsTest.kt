package cz.financer.domain

import cz.financer.data.SaleItem
import cz.financer.data.SaleTransaction
import cz.financer.data.SaleWithItems
import cz.financer.data.TransactionType
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsTest {

    private fun sale(
        id: Long,
        type: TransactionType,
        total: Long,
        createdAt: Long,
        items: List<SaleItem> = emptyList(),
    ) = SaleWithItems(
        sale = SaleTransaction(id = id, projectId = 1, clientId = 1, type = type, createdAt = createdAt, totalHalire = total),
        items = items,
    )

    private fun item(transactionId: Long, productId: Long, name: String, quantity: Int, price: Long) = SaleItem(
        transactionId = transactionId,
        productId = productId,
        productNameSnapshot = name,
        unitPriceHalire = price,
        quantity = quantity,
    )

    @Test
    fun totalDebt_isDebtsMinusRepayments() {
        val sales = listOf(
            sale(1, TransactionType.DEBT, 10000, createdAt = 1),
            sale(2, TransactionType.REPAYMENT, 4000, createdAt = 2),
            sale(3, TransactionType.PAYMENT, 2200, createdAt = 3),
        )
        assertEquals(6000L, Statistics.totalDebt(sales))
        assertEquals(12200L, Statistics.totalSales(sales))
    }

    @Test
    fun productSales_sortsByQuantityDescending() {
        val sales = listOf(
            sale(1, TransactionType.PAYMENT, 2200, createdAt = 1, items = listOf(
                item(1, productId = 1, name = "Mýdlo", quantity = 5, price = 200),
                item(1, productId = 2, name = "Ručník", quantity = 4, price = 300),
            )),
            sale(2, TransactionType.DEBT, 600, createdAt = 2, items = listOf(
                item(2, productId = 2, name = "Ručník", quantity = 2, price = 300),
            )),
        )
        val result = Statistics.productSales(sales)
        assertEquals(listOf("Ručník", "Mýdlo"), result.map { it.productName })
        assertEquals(6, result.first().quantity)
        assertEquals(1800L, result.first().revenueHalire)
    }

    @Test
    fun monthlySales_groupsByMonthInOrder() {
        val jan = 1_704_067_200_000L // 1. 1. 2024 UTC
        val feb = jan + 40L * 24 * 3600 * 1000
        val sales = listOf(
            sale(1, TransactionType.PAYMENT, 100, createdAt = feb),
            sale(2, TransactionType.PAYMENT, 200, createdAt = jan),
            sale(3, TransactionType.REPAYMENT, 999, createdAt = jan),
        )
        val result = Statistics.monthlySales(sales, ZoneOffset.UTC)
        assertEquals(listOf(200L, 100L), result.map { it.totalHalire })
        assertEquals(listOf("2024-01", "2024-02"), result.map { it.month.toString() })
    }
}
