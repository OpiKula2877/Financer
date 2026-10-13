package cz.financer.domain

import cz.financer.data.SaleWithItems
import cz.financer.data.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** Souhrn prodeje jednoho produktu pro statistiky a graf. */
data class ProductSales(
    val productName: String,
    val quantity: Int,
    val revenueHalire: Long,
)

/** Tržby za měsíc pro čárový graf. */
data class MonthlySales(
    val month: YearMonth,
    val totalHalire: Long,
)

/** Výpočty pro záložku Účtárna. Vše se počítá z historie záznamů. */
object Statistics {

    /** Tržby = platby + dluhy (splacení se nepočítá). */
    fun totalSales(sales: List<SaleWithItems>): Long =
        sales.filter { it.sale.type != TransactionType.REPAYMENT }.sumOf { it.sale.totalHalire }

    /** Celkový dluh všech klientů = dluhy − splacení. */
    fun totalDebt(sales: List<SaleWithItems>): Long {
        val debts = sales.filter { it.sale.type == TransactionType.DEBT }.sumOf { it.sale.totalHalire }
        val repaid = sales.filter { it.sale.type == TransactionType.REPAYMENT }.sumOf { it.sale.totalHalire }
        return debts - repaid
    }

    /** Produkty seřazené podle počtu prodaných kusů, od nejprodávanějšího. */
    fun productSales(sales: List<SaleWithItems>): List<ProductSales> =
        sales
            .filter { it.sale.type != TransactionType.REPAYMENT }
            .flatMap { it.items }
            .groupBy { (it.productId ?: -1L) to it.productNameSnapshot }
            .map { (key, items) ->
                ProductSales(
                    productName = key.second,
                    quantity = items.sumOf { it.quantity },
                    revenueHalire = items.sumOf { it.unitPriceHalire * it.quantity },
                )
            }
            .sortedByDescending { it.quantity }

    /** Tržby po měsících, od nejstaršího. Měsíc se počítá v místním časovém pásmu. */
    fun monthlySales(sales: List<SaleWithItems>, zone: ZoneId = ZoneId.systemDefault()): List<MonthlySales> =
        sales
            .filter { it.sale.type != TransactionType.REPAYMENT }
            .groupBy { YearMonth.from(Instant.ofEpochMilli(it.sale.createdAt).atZone(zone)) }
            .map { (month, list) -> MonthlySales(month, list.sumOf { it.sale.totalHalire }) }
            .sortedBy { it.month }
}
