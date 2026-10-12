package cc.xdan.tempo.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import cc.xdan.tempo.model.displayDate
import java.time.LocalDate

@Composable
fun friendlyDate(date: LocalDate): String = displayDate(date, LocalConfiguration.current.locales[0])

@Composable
fun friendlyDate(iso: String): String = validDate(iso)?.let { friendlyDate(it) } ?: iso
