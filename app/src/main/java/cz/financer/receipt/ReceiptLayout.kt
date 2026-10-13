package cz.financer.receipt

import org.json.JSONObject

/** Přepínače, které určují, co se na účtence vykreslí. Hlavička se zobrazí jen když je v nastavení vyplněná. */
data class ReceiptLayout(
    val showHeader: Boolean = true,
    val showDateTime: Boolean = true,
    val showNumber: Boolean = true,
    val showType: Boolean = true,
    val showClient: Boolean = false,
    val showItems: Boolean = true,
    val showQuantities: Boolean = true,
    val showTotal: Boolean = true,
    val showBkp: Boolean = true,
    val showQr: Boolean = true,
) {
    fun toJsonObject(): JSONObject = JSONObject()
        .put("header", showHeader)
        .put("dateTime", showDateTime)
        .put("number", showNumber)
        .put("type", showType)
        .put("client", showClient)
        .put("items", showItems)
        .put("quantities", showQuantities)
        .put("total", showTotal)
        .put("bkp", showBkp)
        .put("qr", showQr)

    fun toJson(): String = toJsonObject().toString()

    companion object {
        private val defaults = ReceiptLayout()

        /** Chybějící nebo poškozený zápis dostane výchozí nastavení. */
        fun fromJson(json: String?): ReceiptLayout =
            if (json.isNullOrBlank()) defaults else fromJsonObject(runCatching { JSONObject(json) }.getOrNull())

        fun fromJsonObject(o: JSONObject?): ReceiptLayout {
            if (o == null) return defaults
            return ReceiptLayout(
                showHeader = o.optBoolean("header", defaults.showHeader),
                showDateTime = o.optBoolean("dateTime", defaults.showDateTime),
                showNumber = o.optBoolean("number", defaults.showNumber),
                showType = o.optBoolean("type", defaults.showType),
                showClient = o.optBoolean("client", defaults.showClient),
                showItems = o.optBoolean("items", defaults.showItems),
                showQuantities = o.optBoolean("quantities", defaults.showQuantities),
                showTotal = o.optBoolean("total", defaults.showTotal),
                showBkp = o.optBoolean("bkp", defaults.showBkp),
                showQr = o.optBoolean("qr", defaults.showQr),
            )
        }
    }
}
