package cz.financer.receipt

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Formáty údajů na účtence v časové zóně telefonu. */
object ReceiptFormat {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val dateFormat = DateTimeFormatter.ofPattern("d. M. yyyy")
    private val timeFormat = DateTimeFormatter.ofPattern("H:mm:ss")

    /** Číslo účtenky s nulami vepředu, např. 0000278. */
    fun number(value: Long): String = value.toString().padStart(7, '0')

    fun date(millis: Long): String = Instant.ofEpochMilli(millis).atZone(zone).format(dateFormat)

    fun time(millis: Long): String = Instant.ofEpochMilli(millis).atZone(zone).format(timeFormat)
}
