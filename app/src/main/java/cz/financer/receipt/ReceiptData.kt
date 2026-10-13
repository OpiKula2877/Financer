package cz.financer.receipt

import cz.financer.data.TransactionType
import org.json.JSONArray
import org.json.JSONObject

data class ReceiptLine(val name: String, val quantity: Int, val unitPriceHalire: Long) {
    val totalHalire: Long get() = unitPriceHalire * quantity
}

/**
 * Obsah účtenky v okamžiku vystavení. Tento JSON text se podepisuje (BKP) a šifruje,
 * proto se nikdy nepřepisuje ani nepřepočítává.
 */
data class ReceiptData(
    val number: Long,
    val issuedAt: Long,
    val type: TransactionType,
    val taxpayerName: String?,
    val address: String?,
    val phone: String?,
    val clientName: String?,
    val lines: List<ReceiptLine>,
    val totalHalire: Long,
    val layout: ReceiptLayout,
) {
    fun toJson(): String = JSONObject()
        .put("number", number)
        .put("issuedAt", issuedAt)
        .put("type", type.name)
        .put("taxpayerName", taxpayerName ?: JSONObject.NULL)
        .put("address", address ?: JSONObject.NULL)
        .put("phone", phone ?: JSONObject.NULL)
        .put("clientName", clientName ?: JSONObject.NULL)
        .put("lines", JSONArray().apply {
            lines.forEach { line ->
                put(
                    JSONObject()
                        .put("name", line.name)
                        .put("quantity", line.quantity)
                        .put("unitPriceHalire", line.unitPriceHalire),
                )
            }
        })
        .put("totalHalire", totalHalire)
        .put("layout", layout.toJsonObject())
        .toString()

    companion object {
        fun fromJson(json: String): ReceiptData {
            val o = JSONObject(json)
            val lines = o.getJSONArray("lines")
            return ReceiptData(
                number = o.getLong("number"),
                issuedAt = o.getLong("issuedAt"),
                type = TransactionType.valueOf(o.getString("type")),
                taxpayerName = o.nullableString("taxpayerName"),
                address = o.nullableString("address"),
                phone = o.nullableString("phone"),
                clientName = o.nullableString("clientName"),
                lines = (0 until lines.length()).map { i ->
                    val line = lines.getJSONObject(i)
                    ReceiptLine(line.getString("name"), line.getInt("quantity"), line.getLong("unitPriceHalire"))
                },
                totalHalire = o.getLong("totalHalire"),
                layout = ReceiptLayout.fromJsonObject(o.optJSONObject("layout")),
            )
        }

        private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
    }
}
