package cz.financer.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptQrPayloadTest {

    private val bkp = "3F9A1C0B-2D7E4A11-8B6C5D4E-9A0F1B2C-7E8D9C0B"

    @Test
    fun contentWithPayloadParsesBackToBkpAndPayload() {
        val parsed = ReceiptQrPayload.parse(ReceiptQrPayload.content(bkp, "abc-_DEF"))

        assertEquals(ReceiptQrPayload.Parsed(bkp, "abc-_DEF"), parsed)
    }

    @Test
    fun contentWithoutPayloadHasNoPayload() {
        val parsed = ReceiptQrPayload.parse(ReceiptQrPayload.content(bkp, null))

        assertEquals(ReceiptQrPayload.Parsed(bkp, null), parsed)
    }

    @Test
    fun textThatIsNotAReceiptIsIgnored() {
        assertNull(ReceiptQrPayload.parse("https://example.cz"))
        assertNull(ReceiptQrPayload.parse("FIN1:krátké:abc"))
    }
}
