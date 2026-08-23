package ai.blueview.weather.util

import ai.blueview.weather.data.preferences.ClockFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Time helpers for the CITY being viewed, not the phone's own timezone. The forecast is
 * requested with timezone=auto, so Open-Meteo returns both an IANA zone id and a raw
 * offset for that city.
 */
object CityClock {

    private val H12 = DateTimeFormatter.ofPattern("h:mm a")
    private val H24 = DateTimeFormatter.ofPattern("HH:mm")
    private val H12_HOUR = DateTimeFormatter.ofPattern("h a")
    private val H24_HOUR = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Prefer the IANA id ("America/Denver") over the raw offset: the offset Open-Meteo
     * returns is the one in force right now, so using it alone would render times in the
     * wrong hour across a DST boundary. Falls back to the offset when the id is missing
     * or unrecognised by this device's tz database.
     */
    fun zoneOf(timezone: String, utcOffsetSeconds: Int): ZoneId =
        runCatching { ZoneId.of(timezone) }
            .getOrElse { ZoneOffset.ofTotalSeconds(utcOffsetSeconds) }

    /** Current wall-clock time in that city. */
    fun nowIn(zone: ZoneId, format: ClockFormat): String =
        Instant.now().atZone(zone).format(if (format == ClockFormat.H12) H12 else H24)

    /**
     * Formats an Open-Meteo local timestamp ("2026-08-10T14:00"). These are already in
     * the city's local time, so they are formatted as-is — never converted again.
     */
    fun hourLabel(localIso: String, format: ClockFormat): String =
        runCatching {
            LocalDateTime.parse(localIso).format(
                if (format == ClockFormat.H12) H12_HOUR else H24_HOUR
            )
        }.getOrElse { localIso.takeLast(5) }
}
