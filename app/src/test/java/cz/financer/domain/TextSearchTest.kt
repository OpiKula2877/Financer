package cz.financer.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSearchTest {

    @Test
    fun emptyQuery_matchesEverything() {
        assertTrue(TextSearch.matches("Mýdlo", ""))
        assertTrue(TextSearch.matches("Mýdlo", "   "))
    }

    @Test
    fun ignoresCaseAndDiacritics() {
        assertTrue(TextSearch.matches("Mýdlo levandulové", "mydlo"))
        assertTrue(TextSearch.matches("Ručník", "RUCN"))
        assertTrue(TextSearch.matches("Paní Nováková", "novak"))
    }

    @Test
    fun missingText_doesNotMatch() {
        assertFalse(TextSearch.matches("Ručník", "mýdlo"))
    }
}
