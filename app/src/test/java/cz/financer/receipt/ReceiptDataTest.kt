package cz.financer.receipt

import cz.financer.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptDataTest {

    private val data = ReceiptData(
        number = 278,
        issuedAt = 1_700_000_000_000,
        type = TransactionType.DEBT,
        taxpayerName = "Jana Nováková",
        address = null,
        phone = "123 456 789",
        clientName = "Ana",
        lines = listOf(ReceiptLine("Mýdlo", 5, 200), ReceiptLine("Ručník", 4, 300)),
        totalHalire = 2200,
        layout = ReceiptLayout(showClient = true),
    )

    @Test
    fun jsonRoundTripKeepsEveryField() {
        assertEquals(data, ReceiptData.fromJson(data.toJson()))
    }

    @Test
    fun lineTotalsAddUpToReceiptTotal() {
        assertEquals(data.totalHalire, data.lines.sumOf { it.totalHalire })
    }

    @Test
    fun layoutFallsBackToDefaultsForMissingOrBrokenJson() {
        assertEquals(ReceiptLayout(), ReceiptLayout.fromJson(null))
        assertEquals(ReceiptLayout(), ReceiptLayout.fromJson("není json"))
        assertEquals(ReceiptLayout(), ReceiptLayout.fromJson(""))
    }

    @Test
    fun layoutRoundTripsAllSwitches() {
        val layout = ReceiptLayout(showHeader = false, showQr = false, showClient = true)
        assertEquals(layout, ReceiptLayout.fromJson(layout.toJson()))
        assertFalse(layout.showHeader)
        assertTrue(layout.showClient)
    }
}
