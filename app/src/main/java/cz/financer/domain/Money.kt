package cz.financer.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** Peníze se ukládají v haléřích (Long), aby nevznikaly zaokrouhlovací chyby. */
object Money {

    /** Formát pro zobrazení, např. "22 CZK", "1 250 CZK" nebo "2,50 CZK". */
    fun format(halire: Long): String {
        val sign = if (halire < 0) "-" else ""
        val absolute = kotlin.math.abs(halire)
        val kc = (absolute / 100).toString().reversed().chunked(3).joinToString(" ").reversed()
        val haler = absolute % 100
        return if (haler == 0L) {
            "$sign$kc CZK"
        } else {
            "$sign$kc,${haler.toString().padStart(2, '0')} CZK"
        }
    }

    /** Text pro pole ve formuláři bez měny, např. "2" nebo "2,50". */
    fun toEditable(halire: Long): String {
        val kc = halire / 100
        val haler = halire % 100
        return if (haler == 0L) kc.toString() else "$kc,${haler.toString().padStart(2, '0')}"
    }

    /**
     * Převede text z formuláře (např. "2", "2,5", "2.50") na haléře.
     * Vrací null, pokud text není platné nezáporné číslo.
     */
    fun parse(text: String): Long? {
        val normalized = text.trim().replace(',', '.').replace(" ", "")
        if (normalized.isEmpty()) return null
        return try {
            val amount = BigDecimal(normalized)
            if (amount.signum() < 0) return null
            amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (_: NumberFormatException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }
}
