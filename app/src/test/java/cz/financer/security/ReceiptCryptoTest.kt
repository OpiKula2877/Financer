package cz.financer.security

import javax.crypto.AEADBadTagException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptCryptoTest {

    private val key = ReceiptCrypto.generateKey()
    private val json = """{"number":278,"totalHalire":2200}"""

    @Test
    fun generatedKeyHasFiftyAllowedCharacters() {
        repeat(20) {
            val generated = ReceiptCrypto.generateKey()
            assertEquals(50, generated.length)
            assertTrue(ReceiptCrypto.isValidKey(generated))
        }
    }

    @Test
    fun keysWithWrongLengthOrCharactersAreRejected() {
        assertFalse(ReceiptCrypto.isValidKey("kratky"))
        assertFalse(ReceiptCrypto.isValidKey("ř".repeat(ReceiptCrypto.KEY_LENGTH)))
    }

    @Test
    fun bkpHasFortyFourCharactersInFiveGroupsOfEight() {
        val bkp = ReceiptCrypto.bkp(key, json)
        assertEquals(44, bkp.length)
        assertTrue(ReceiptCrypto.isValidBkp(bkp))
        assertEquals(listOf(8, 8, 8, 8, 8), bkp.split("-").map { it.length })
    }

    @Test
    fun bkpIsStableForSameKeyAndContent() {
        assertEquals(ReceiptCrypto.bkp(key, json), ReceiptCrypto.bkp(key, json))
    }

    @Test
    fun bkpChangesWhenContentChanges() {
        assertNotEquals(ReceiptCrypto.bkp(key, json), ReceiptCrypto.bkp(key, json.replace("2200", "2300")))
    }

    @Test
    fun bkpChangesWithKey() {
        assertNotEquals(ReceiptCrypto.bkp(key, json), ReceiptCrypto.bkp(ReceiptCrypto.generateKey(), json))
    }

    @Test
    fun encryptedPayloadDecryptsToOriginal() {
        assertEquals(json, ReceiptCrypto.decrypt(key, ReceiptCrypto.encrypt(key, json)))
    }

    @Test
    fun payloadIsDifferentEachTime() {
        assertNotEquals(ReceiptCrypto.encrypt(key, json), ReceiptCrypto.encrypt(key, json))
    }

    @Test(expected = AEADBadTagException::class)
    fun wrongKeyCannotDecrypt() {
        ReceiptCrypto.decrypt(ReceiptCrypto.generateKey(), ReceiptCrypto.encrypt(key, json))
    }

    @Test(expected = AEADBadTagException::class)
    fun tamperedPayloadIsRejected() {
        val token = ReceiptCrypto.encrypt(key, json)
        val index = token.length / 2
        val replacement = if (token[index] == 'A') "B" else "A"
        ReceiptCrypto.decrypt(key, token.replaceRange(index, index + 1, replacement))
    }
}
