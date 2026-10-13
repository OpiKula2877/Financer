package cz.financer.domain

import java.text.Normalizer

/** Vyhledávání bez ohledu na velikost písmen a diakritiku: „mydlo“ najde „Mýdlo“. */
object TextSearch {

    fun matches(text: String, query: String): Boolean {
        val needle = fold(query)
        return needle.isEmpty() || fold(text).contains(needle)
    }

    private fun fold(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
}
