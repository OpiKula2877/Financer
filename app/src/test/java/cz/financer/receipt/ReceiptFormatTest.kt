package cz.financer.receipt

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptFormatTest {

    @Test
    fun receiptNumberIsPaddedToSevenDigits() {
        assertEquals("0000278", ReceiptFormat.number(278))
        assertEquals("0000001", ReceiptFormat.number(1))
    }
}
