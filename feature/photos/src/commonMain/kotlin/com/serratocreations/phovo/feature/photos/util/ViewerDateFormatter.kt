package com.serratocreations.phovo.feature.photos.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

// TODO: English and 12-hour only, like the feed's month headers. Localizing needs a formatter per
//  platform, because kotlinx-datetime has no locale support.

/**
 * The day a photo was taken, as the viewer's title: "May 2", or "May 2, 2024" when the year is not
 * [currentYear].
 */
fun LocalDateTime.toViewerDateString(currentYear: Int): String =
    if (year == currentYear) DayFormat.format(this) else DayWithYearFormat.format(this)

/** The time of day a photo was taken, as the viewer's subtitle: "10:31 AM". */
fun LocalDateTime.toViewerTimeString(): String = TimeFormat.format(this)

private val DayFormat = LocalDateTime.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    day(padding = Padding.NONE)
}

private val DayWithYearFormat = LocalDateTime.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    day(padding = Padding.NONE)
    chars(", ")
    year()
}

private val TimeFormat = LocalDateTime.Format {
    amPmHour(padding = Padding.NONE)
    char(':')
    minute()
    char(' ')
    amPmMarker(am = "AM", pm = "PM")
}
