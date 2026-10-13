package cz.financer.domain

import cz.financer.data.SaleWithItems
import cz.financer.data.TransactionType

/** Jeden řádek košíku ve formuláři platby. */
data class CartLine(
    val productId: Long,
    val productName: String,
    val unitPriceHalire: Long,
    val quantity: Int,
)

/**
 * Všechny součty se počítají z historie záznamů, takže se nikde neukládají
 * a vždy odpovídají datům v databázi.
 */
object SaleCalculator {

    /** Celková cena košíku, např. 5 × 2 Kč + 4 × 3 Kč = 22 Kč. */
    fun cartTotal(lines: List<CartLine>): Long =
        lines.sumOf { it.unitPriceHalire * it.quantity }

    /** Dluh klienta = dluhy − splacení. */
    fun debtOf(clientId: Long, sales: List<SaleWithItems>): Long {
        val forClient = sales.filter { it.sale.clientId == clientId }
        val debts = forClient.filter { it.sale.type == TransactionType.DEBT }.sumOf { it.sale.totalHalire }
        val repaid = forClient.filter { it.sale.type == TransactionType.REPAYMENT }.sumOf { it.sale.totalHalire }
        return debts - repaid
    }

    /** Vydělal = platby + dluhy (celková hodnota prodeje klientovi). */
    fun earnedFrom(clientId: Long, sales: List<SaleWithItems>): Long =
        sales
            .filter { it.sale.clientId == clientId && it.sale.type != TransactionType.REPAYMENT }
            .sumOf { it.sale.totalHalire }

    /** Počet prodaných kusů produktu. Splacení se nepočítá. */
    fun soldQuantity(productId: Long, sales: List<SaleWithItems>): Int =
        sales
            .filter { it.sale.type != TransactionType.REPAYMENT }
            .flatMap { it.items }
            .filter { it.productId == productId }
            .sumOf { it.quantity }

    /** Dluhy jen u klientů, kteří dluží (dluh > 0). */
    fun debtorBalances(clientIds: Collection<Long>, sales: List<SaleWithItems>): Map<Long, Long> =
        clientIds
            .associateWith { debtOf(it, sales) }
            .filterValues { it > 0 }

    /** Podíl každého dlužníka na celkovém dluhu v procentech (součet je 100). */
    fun debtShares(balances: Map<Long, Long>): Map<Long, Double> {
        val total = balances.values.sum()
        if (total <= 0) return emptyMap()
        return balances.mapValues { (_, debt) -> debt * 100.0 / total }
    }
}
