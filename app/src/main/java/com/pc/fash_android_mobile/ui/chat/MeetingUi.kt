package com.pc.fash_android_mobile.ui.chat

import android.net.Uri
import android.text.format.DateFormat
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.chat.ChatMessage
import com.pc.fash_android_mobile.data.chat.MeetingAppointmentPayload
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import com.pc.fash_android_mobile.ui.components.FashPillFilterChip
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import com.pc.fash_android_mobile.ui.theme.FashShapes
import com.pc.fash_android_mobile.ui.theme.editorialHorizontalPadding
import com.pc.fash_android_mobile.ui.theme.fashReadableOn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import com.pc.fash_android_mobile.data.common.CommonAddressDto
import com.pc.fash_android_mobile.data.common.SafeMeetupZoneDto
import com.pc.fash_android_mobile.data.locale.AppLocale
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MeetingLocationPanelHeight = 300.dp

private const val PROVINCE_ID_HANOI = "4ef77064-7ee8-59cb-800f-de9af3395329"
private const val PROVINCE_ID_HCM = "74fab9c4-a31b-5147-be55-4f4e7990ef05"

private fun safeZoneTypeEmoji(zoneType: String): String = when (zoneType.lowercase()) {
    "cafe" -> "☕"
    "mall" -> "🛍️"
    "convenience" -> "🏪"
    else -> "📍"
}

private fun safeZoneTypeLabelRes(zoneType: String): Int = when (zoneType.lowercase()) {
    "cafe" -> R.string.safe_zone_type_cafe
    "mall" -> R.string.safe_zone_type_mall
    "convenience" -> R.string.safe_zone_type_convenience
    else -> R.string.safe_zone_type_other
}

private fun meetingAppLocale(preferVi: Boolean): Locale =
    if (preferVi) Locale.forLanguageTag(AppLocale.TAG_VI) else Locale.forLanguageTag(AppLocale.TAG_EN)

/** Round up to the next 15-minute slot (e.g. 14:07 → 14:15). */
private fun roundUpToNextMeetingSlot(now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())): LocalTime {
    var slot = now.withSecond(0).withNano(0)
    val remainder = slot.minute % 15
    if (remainder != 0 || now.second > 0 || now.nano > 0) {
        slot = slot.plusMinutes((15 - remainder).toLong())
    }
    return slot.toLocalTime()
}

private fun clampTimeForDate(dateMillis: Long?, hour: Int, minute: Int): Pair<Int, Int> {
    if (dateMillis == null) return hour to minute
    val zone = ZoneId.systemDefault()
    val selectedDate = Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    if (selectedDate.isAfter(today)) return hour to minute
    val minTime = roundUpToNextMeetingSlot(ZonedDateTime.now(zone))
    val proposed = LocalTime.of(hour, minute)
    return if (proposed.isBefore(minTime)) minTime.hour to minTime.minute else hour to minute
}

private fun isScheduleInFuture(dateMillis: Long?, hour: Int, minute: Int): Boolean {
    if (dateMillis == null) return false
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate()
    val scheduled = ZonedDateTime.of(date, LocalTime.of(hour, minute), zone)
    return scheduled.isAfter(ZonedDateTime.now(zone))
}

private fun isSelectedDateToday(dateMillis: Long?): Boolean {
    if (dateMillis == null) return false
    val zone = ZoneId.systemDefault()
    return Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate() == LocalDate.now(zone)
}

@OptIn(ExperimentalMaterial3Api::class)
private val meetingTodaySelectableDates = object : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val zone = ZoneId.systemDefault()
        val todayStart = LocalDate.now(zone)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        return utcTimeMillis >= todayStart
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MeetingProposalBottomSheet(
    isLoading: Boolean,
    /** When set with [onViewOrder], shows a link to open order detail (e.g. escrow + meetup). */
    linkedOrderId: String? = null,
    onViewOrder: ((String) -> Unit)? = null,
    /** Viewer browse location from profile — seeds safe-zone lookup. */
    browseProvinceId: String? = null,
    browseDistrictId: String? = null,
    loadSafeZones: suspend (provinceId: String?, districtId: String?) -> Result<List<SafeMeetupZoneDto>>,
    loadProvinces: suspend () -> Result<List<CommonAddressDto>>,
    onDismiss: () -> Unit,
    onSubmit: (
        locationUrl: String,
        scheduledAtIso: String,
        reminderEnabled: Boolean,
        reminderOffsetMinutes: Int,
        safeZoneId: String?,
        safeZoneName: String?,
    ) -> Unit,
) {
    val context = LocalContext.current
    val localeRev by AppLocale.localeRevisionFlow.collectAsState()
    val preferVi = AppLocale.currentTag(context) != AppLocale.TAG_EN
    val cityHanoi = stringResource(R.string.safe_zone_city_hanoi)
    val cityHcm = stringResource(R.string.safe_zone_city_hcm)
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var locationTab by remember { mutableIntStateOf(0) } // 0 = safe zone, 1 = manual
    var locationUrl by remember { mutableStateOf("") }
    var selectedSafeZoneId by remember { mutableStateOf<String?>(null) }
    var selectedSafeZoneName by remember { mutableStateOf("") }
    var allSafeZones by remember { mutableStateOf<List<SafeMeetupZoneDto>>(emptyList()) }
    var zonesLoading by remember { mutableStateOf(true) }
    var zonesLoadError by remember { mutableStateOf(false) }
    var zonesReloadKey by remember { mutableIntStateOf(0) }
    var provinces by remember { mutableStateOf<List<CommonAddressDto>>(emptyList()) }
    var pickerProvinceId by remember(browseProvinceId) { mutableStateOf<String?>(browseProvinceId) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val defaultTimeSlot = remember { roundUpToNextMeetingSlot() }
    var pickedHour by remember { mutableIntStateOf(defaultTimeSlot.hour) }
    var pickedMinute by remember { mutableIntStateOf(defaultTimeSlot.minute) }
    var reminderEnabled by remember { mutableStateOf(true) }
    var offset by remember { mutableIntStateOf(60) }

    val offsets = listOf(
        15 to R.string.chat_meeting_reminder_15,
        30 to R.string.chat_meeting_reminder_30,
        60 to R.string.chat_meeting_reminder_60,
        1440 to R.string.chat_meeting_reminder_1440,
    )

    LaunchedEffect(zonesReloadKey, browseProvinceId, localeRev) {
        zonesLoading = true
        zonesLoadError = false
        val zonesResult = withContext(Dispatchers.IO) {
            loadSafeZones(null, null)
        }
        var loadedZones = emptyList<SafeMeetupZoneDto>()
        zonesResult.fold(
            onSuccess = { loadedZones = it },
            onFailure = { e ->
                android.util.Log.w("MeetingProposalSheet", "loadSafeZones failed", e)
                zonesLoadError = true
            },
        )
        allSafeZones = loadedZones
        if (provinces.isEmpty()) {
            provinces = withContext(Dispatchers.IO) {
                loadProvinces().getOrElse { emptyList() }
            }
        }
        if (pickerProvinceId.isNullOrBlank()) {
            val preferred = listOfNotNull(
                browseProvinceId?.trim()?.takeIf { it.isNotEmpty() },
            ).firstOrNull { pid -> loadedZones.any { it.provinceId.equals(pid, ignoreCase = true) } }
                ?: loadedZones.firstOrNull()?.provinceId?.takeIf { it.isNotBlank() }
            pickerProvinceId = preferred
        }
        zonesLoadError = zonesLoadError && loadedZones.isEmpty()
        zonesLoading = false
    }

    val citiesWithZones = remember(allSafeZones, provinces, cityHanoi, cityHcm) {
        fun localizedProvinceName(provinceId: String, catalogName: String): String = when (provinceId.lowercase()) {
            PROVINCE_ID_HANOI -> cityHanoi
            PROVINCE_ID_HCM -> cityHcm
            else -> catalogName.ifBlank { provinceId.take(8) }
        }
        val ids = allSafeZones.map { it.provinceId }.distinct()
        ids.mapNotNull { pid ->
            val catalog = provinces.find { it.id.equals(pid, ignoreCase = true) }
            CommonAddressDto(
                id = pid,
                name = localizedProvinceName(pid, catalog?.name.orEmpty()),
                code = catalog?.code.orEmpty(),
                parentId = catalog?.parentId,
                level = catalog?.level ?: 1,
                status = catalog?.status ?: "active",
                effectiveFrom = catalog?.effectiveFrom,
                effectiveTo = catalog?.effectiveTo,
            )
        }
    }

    val visibleZones = remember(allSafeZones, pickerProvinceId) {
        val pid = pickerProvinceId?.trim()?.takeIf { it.isNotEmpty() }
        if (pid == null) {
            allSafeZones
        } else {
            allSafeZones.filter { it.provinceId.equals(pid, ignoreCase = true) }
        }
    }

    LaunchedEffect(selectedDateMillis) {
        selectedDateMillis?.let {
            val (h, m) = clampTimeForDate(it, pickedHour, pickedMinute)
            pickedHour = h
            pickedMinute = m
        }
    }

    val scheduledAtIso = remember(selectedDateMillis, pickedHour, pickedMinute) {
        selectedDateMillis?.let { buildScheduledAtRfc3339(it, pickedHour, pickedMinute) }
    }
    val dateTimeSummary = remember(selectedDateMillis, pickedHour, pickedMinute, preferVi) {
        selectedDateMillis?.let { formatSelectedDateTime(it, pickedHour, pickedMinute, preferVi) }
            ?: ""
    }
    val timeFromNowHint = remember(selectedDateMillis, pickedHour, pickedMinute, preferVi, localeRev) {
        if (!isSelectedDateToday(selectedDateMillis)) return@remember null
        val minTime = roundUpToNextMeetingSlot()
        formatTimeOnly(minTime.hour, minTime.minute, preferVi)
    }
    val locationReady = when (locationTab) {
        0 -> selectedSafeZoneId != null
        else -> locationUrl.isNotBlank()
    }
    val canSubmit = locationReady &&
        selectedDateMillis != null &&
        scheduledAtIso != null &&
        isScheduleInFuture(selectedDateMillis, pickedHour, pickedMinute)
    val sheetSubtitle = if (locationTab == 0) {
        stringResource(R.string.chat_meeting_sheet_subtitle_safe)
    } else {
        stringResource(R.string.chat_meeting_sheet_subtitle_manual)
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis ?: System.currentTimeMillis(),
            selectableDates = meetingTodaySelectableDates,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDateMillis = millis
                            val (h, m) = clampTimeForDate(millis, pickedHour, pickedMinute)
                            pickedHour = h
                            pickedMinute = m
                        }
                        showDatePicker = false
                    },
                ) { Text(stringResource(R.string.chat_meeting_picker_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.chat_meeting_picker_cancel))
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = stringResource(R.string.chat_meeting_date_picker_title),
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                    )
                },
            )
        }
    }

    if (showTimePicker) {
        val is24Hour = DateFormat.is24HourFormat(context)
        val (initHour, initMinute) = clampTimeForDate(selectedDateMillis, pickedHour, pickedMinute)
        key(selectedDateMillis, initHour, initMinute, localeRev) {
            val timePickerState = rememberTimePickerState(
                initialHour = initHour,
                initialMinute = initMinute,
                is24Hour = is24Hour,
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text(stringResource(R.string.chat_meeting_time_picker_title)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val (h, m) = clampTimeForDate(
                                selectedDateMillis,
                                timePickerState.hour,
                                timePickerState.minute,
                            )
                            pickedHour = h
                            pickedMinute = m
                            showTimePicker = false
                        },
                    ) {
                        Text(stringResource(R.string.chat_meeting_picker_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text(stringResource(R.string.chat_meeting_picker_cancel))
                    }
                },
                text = { TimePicker(state = timePickerState) },
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isLoading) {
                keyboard?.hide()
                focusManager.clearFocus(force = true)
                onDismiss()
            }
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        val scheme = MaterialTheme.colorScheme
        val spacing = FashTheme.spacing
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(spacing.editorialHorizontalPadding())
                .padding(top = spacing.spacing2, bottom = spacing.spacing6)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing4),
        ) {
            MeetingSheetHeader(
                title = stringResource(R.string.chat_meeting_sheet_title),
                subtitle = sheetSubtitle,
            )

            Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing2)) {
                MeetingPillTabRow(
                    selectedTab = locationTab,
                    onTabSelected = { tab ->
                        locationTab = tab
                        if (tab == 1) {
                            selectedSafeZoneId = null
                            selectedSafeZoneName = ""
                            locationUrl = ""
                        }
                    },
                    tab0Label = stringResource(R.string.safe_zone_tab_picker),
                    tab1Label = stringResource(R.string.safe_zone_tab_manual),
                    enabled = !isLoading,
                )

                MeetingLocationPanel(
                locationTab = locationTab,
                isLoading = isLoading,
                isVi = preferVi,
                citiesWithZones = citiesWithZones,
                pickerProvinceId = pickerProvinceId,
                onProvinceSelected = { pid ->
                    pickerProvinceId = pid
                    selectedSafeZoneId = null
                    selectedSafeZoneName = ""
                    locationUrl = ""
                },
                visibleZones = visibleZones,
                zonesLoading = zonesLoading,
                zonesLoadError = zonesLoadError,
                selectedSafeZoneId = selectedSafeZoneId,
                onZoneSelected = { zone, label ->
                    selectedSafeZoneId = zone.id
                    selectedSafeZoneName = label
                    locationUrl = zone.locationUrl
                },
                onRetryZones = { zonesReloadKey++ },
                onSwitchToManual = { locationTab = 1 },
                locationUrl = locationUrl,
                onLocationUrlChange = {
                    locationUrl = it
                    selectedSafeZoneId = null
                    selectedSafeZoneName = ""
                },
                )
            }

            MeetingSheetSection(title = stringResource(R.string.chat_meeting_datetime_label)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing3),
                ) {
                    MeetingDateTimePickCard(
                        icon = Icons.Outlined.Event,
                        label = stringResource(R.string.chat_meeting_pick_date),
                        value = selectedDateMillis?.let { formatDateOnly(it, preferVi) },
                        placeholder = stringResource(R.string.chat_meeting_pick_date),
                        onClick = { showDatePicker = true },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                    )
                    MeetingDateTimePickCard(
                        icon = Icons.Outlined.Schedule,
                        label = stringResource(R.string.chat_meeting_pick_time),
                        value = formatTimeOnly(pickedHour, pickedMinute, preferVi),
                        placeholder = stringResource(R.string.chat_meeting_pick_time),
                        onClick = { showTimePicker = true },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (dateTimeSummary.isNotBlank()) {
                    Text(
                        text = dateTimeSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                if (timeFromNowHint != null) {
                    Text(
                        text = stringResource(R.string.chat_meeting_time_from_now_hint, timeFromNowHint),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            MeetingSheetSection(title = stringResource(R.string.chat_meeting_reminder_label)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
                ) {
                    offsets.forEach { (minutes, labelRes) ->
                        MeetingEditorialChip(
                            label = stringResource(labelRes),
                            selected = offset == minutes,
                            onClick = { offset = minutes },
                            enabled = !isLoading,
                        )
                    }
                }
                Spacer(Modifier.height(spacing.spacing1))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.chat_meeting_reminder_toggle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface,
                    )
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        enabled = !isLoading,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FashColors.Primary.fashReadableOn(),
                            checkedTrackColor = FashColors.Primary,
                            uncheckedTrackColor = scheme.surfaceContainerHighest,
                        ),
                    )
                }
            }

            val oid = linkedOrderId?.trim()?.takeIf { it.isNotEmpty() }
            if (oid != null && onViewOrder != null) {
                TextButton(
                    onClick = { onViewOrder(oid) },
                    enabled = !isLoading,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = stringResource(R.string.chat_deal_banner_view_order),
                        color = FashColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Button(
                onClick = {
                    val iso = scheduledAtIso ?: return@Button
                    onSubmit(
                        locationUrl.trim(),
                        iso,
                        reminderEnabled,
                        offset,
                        selectedSafeZoneId?.trim()?.takeIf { it.isNotEmpty() },
                        selectedSafeZoneName.trim().takeIf { it.isNotEmpty() },
                    )
                },
                enabled = canSubmit && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(spacing.buttonHeight),
                shape = FashShapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FashColors.Primary,
                    contentColor = FashColors.Primary.fashReadableOn(),
                    disabledContainerColor = scheme.surfaceContainerHighest,
                    disabledContentColor = scheme.onSurfaceVariant.copy(alpha = 0.55f),
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = FashColors.Primary.fashReadableOn(),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.chat_meeting_sheet_submit),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}

@Composable
private fun MeetingSheetHeader(
    title: String,
    subtitle: String,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun MeetingPillTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tab0Label: String,
    tab1Label: String,
    enabled: Boolean,
) {
    val spacing = FashTheme.spacing
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = spacing.chipShape(),
        color = scheme.surfaceContainerLow,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MeetingSegmentTab(
                label = tab0Label,
                selected = selectedTab == 0,
                onClick = { if (enabled) onTabSelected(0) },
                modifier = Modifier.weight(1f),
            )
            MeetingSegmentTab(
                label = tab1Label,
                selected = selectedTab == 1,
                onClick = { if (enabled) onTabSelected(1) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MeetingSegmentTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = FashTheme.spacing.chipShape()
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = if (selected) FashColors.Primary.fashReadableOn() else scheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(shape)
            .background(if (selected) FashColors.Primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
    )
}

@Composable
private fun MeetingSheetSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val spacing = FashTheme.spacing
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = FashShapes.large,
            color = scheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.42f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing3),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun MeetingDateTimePickCard(
    icon: ImageVector,
    label: String,
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = FashTheme.spacing
    val hasValue = !value.isNullOrBlank()
    Surface(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = FashShapes.large,
        color = if (hasValue) {
            FashColors.Primary.copy(alpha = 0.08f)
        } else {
            scheme.surfaceContainerLow
        },
        border = BorderStroke(
            1.dp,
            if (hasValue) FashColors.Primary.copy(alpha = 0.45f) else scheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.spacing3),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (hasValue) FashColors.Primary else scheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
            Text(
                text = value?.takeIf { it.isNotBlank() } ?: placeholder,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (hasValue) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = if (hasValue) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MeetingEditorialChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    FashPillFilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        enabled = enabled,
    )
}

private fun formatDateOnly(selectedDateMillis: Long, preferVi: Boolean): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(selectedDateMillis).atZone(zone)
    val pattern = if (preferVi) "dd/MM/yyyy" else "MMM d, yyyy"
    return date.format(DateTimeFormatter.ofPattern(pattern, meetingAppLocale(preferVi)))
}

private fun formatTimeOnly(hour: Int, minute: Int, preferVi: Boolean): String =
    String.format(meetingAppLocale(preferVi), "%02d:%02d", hour, minute)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeetingLocationPanel(
    locationTab: Int,
    isLoading: Boolean,
    isVi: Boolean,
    citiesWithZones: List<CommonAddressDto>,
    pickerProvinceId: String?,
    onProvinceSelected: (String) -> Unit,
    visibleZones: List<SafeMeetupZoneDto>,
    zonesLoading: Boolean,
    zonesLoadError: Boolean,
    selectedSafeZoneId: String?,
    onZoneSelected: (SafeMeetupZoneDto, String) -> Unit,
    onRetryZones: () -> Unit,
    onSwitchToManual: () -> Unit,
    locationUrl: String,
    onLocationUrlChange: (String) -> Unit,
) {
    val spacing = FashTheme.spacing
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(MeetingLocationPanelHeight),
        shape = FashShapes.large,
        color = scheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.42f)),
    ) {
        if (locationTab == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.spacing3),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.safe_zone_hub_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (zonesLoadError) {
                        TextButton(onClick = onRetryZones, enabled = !isLoading) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.safe_zone_retry))
                        }
                    }
                }
                if (citiesWithZones.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
                    ) {
                        citiesWithZones.forEach { city ->
                            FashPillFilterChip(
                                selected = pickerProvinceId.equals(city.id, ignoreCase = true),
                                onClick = { onProvinceSelected(city.id) },
                                label = city.name,
                                enabled = !isLoading,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    when {
                        zonesLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center).size(28.dp),
                                strokeWidth = 2.dp,
                                color = FashColors.Primary,
                            )
                        }
                        zonesLoadError -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.safe_zone_load_error),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.error,
                                )
                                OutlinedButton(onClick = onRetryZones, enabled = !isLoading) {
                                    Text(stringResource(R.string.safe_zone_retry))
                                }
                            }
                        }
                        visibleZones.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.safe_zone_empty_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant,
                                )
                                TextButton(onClick = onSwitchToManual, enabled = !isLoading) {
                                    Text(stringResource(R.string.safe_zone_switch_manual))
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(visibleZones, key = { it.id }) { zone ->
                                    val label = zone.displayLabel(isVi)
                                    SafeZonePickRow(
                                        label = label,
                                        typeLabel = stringResource(safeZoneTypeLabelRes(zone.zoneType)),
                                        addressLine = zone.addressLine,
                                        emoji = safeZoneTypeEmoji(zone.zoneType),
                                        selected = selectedSafeZoneId.equals(zone.id, ignoreCase = true),
                                        enabled = !isLoading,
                                        onClick = { onZoneSelected(zone, label) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.spacing3),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
            ) {
                Text(
                    text = stringResource(R.string.safe_zone_manual_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                )
                OutlinedTextField(
                    value = locationUrl,
                    onValueChange = onLocationUrlChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = { Text(stringResource(R.string.chat_meeting_field_maps_hint)) },
                    singleLine = false,
                    minLines = 4,
                    maxLines = 6,
                    enabled = !isLoading,
                    shape = FashShapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FashColors.Primary,
                        focusedLabelColor = FashColors.Primary,
                        unfocusedContainerColor = scheme.surfaceContainerLow,
                        focusedContainerColor = scheme.surfaceContainerLow,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SafeZonePickRow(
    label: String,
    typeLabel: String,
    addressLine: String,
    emoji: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = FashTheme.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = FashShapes.large,
        color = if (selected) {
            FashColors.Primary.copy(alpha = 0.1f)
        } else {
            scheme.surfaceContainerLow
        },
        border = BorderStroke(
            1.dp,
            if (selected) {
                FashColors.Primary.copy(alpha = 0.5f)
            } else {
                scheme.outlineVariant.copy(alpha = 0.35f)
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.spacing3, vertical = spacing.spacing2 + 2.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (selected) FashColors.Primary.copy(alpha = 0.15f) else scheme.surfaceContainerHighest,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, style = MaterialTheme.typography.titleMedium)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
                if (addressLine.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = scheme.onSurfaceVariant,
                        )
                        Text(
                            text = addressLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        color = if (selected) FashColors.Primary else Color.Transparent,
                        shape = CircleShape,
                    )
                    .border(
                        width = if (selected) 0.dp else 1.5.dp,
                        color = scheme.outlineVariant.copy(alpha = 0.65f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = FashColors.Primary.fashReadableOn(),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MeetingMutationButtonContent(
    loading: Boolean,
    label: String,
    progressColor: Color,
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = progressColor,
        )
    } else {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MeetingProposalMessageCard(
    message: ChatMessage,
    meeting: MeetingAppointmentPayload,
    /** Current user is buyer in this thread (drives check-in column + copy). */
    isViewerBuyer: Boolean,
    /** Escrow / linked order — show handoff / confirm next-step hints (check-in ≠ order done). */
    hasLinkedEscrowOrder: Boolean,
    formatTime: (String) -> String,
    mutationInFlight: Boolean,
    onConfirm: () -> Unit,
    /** Recipient rejects or proposer withdraws — maps to POST …/cancel. */
    onWithdrawOrReject: () -> Unit,
    /** C2C: after both sides confirmed, optional entry to `POST /deals`. */
    onRecordOfflineDeal: (() -> Unit)? = null,
    /** Confirmed meetup: `POST …/check-in` within `scheduled_at ± 30m`; does not change appointment.status. */
    onCheckIn: (() -> Unit)? = null,
    /** Confirmed meetup within window: `POST …/on-my-way`. */
    onOnMyWay: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val whenText = remember(meeting.scheduledAt) { formatMeetingWhen(meeting.scheduledAt) }
    val st = meeting.status.lowercase()
    val showConfirm = st == "pending" && !meeting.isProposerMe
    val showCancel = st == "pending" && meeting.isProposerMe
    val myCheckInAt = if (isViewerBuyer) meeting.buyerCheckInAt else meeting.sellerCheckInAt
    val otherCheckInAt = if (isViewerBuyer) meeting.sellerCheckInAt else meeting.buyerCheckInAt
    val myOnMyWayAt = if (isViewerBuyer) meeting.buyerOnMyWayAt else meeting.sellerOnMyWayAt
    val otherOnMyWayAt = if (isViewerBuyer) meeting.sellerOnMyWayAt else meeting.buyerOnMyWayAt
    val withinMeetupWindow = isWithinMeetingActionWindow(meeting.scheduledAt)
    val showOnMyWayCta = st == "confirmed" &&
        onOnMyWay != null &&
        myOnMyWayAt.isBlank() &&
        myCheckInAt.isBlank()
    val showCheckInCta = st == "confirmed" &&
        onCheckIn != null &&
        myCheckInAt.isBlank() &&
        myOnMyWayAt.isNotBlank() &&
        withinMeetupWindow

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(scheme.surfaceContainerLow, RoundedCornerShape(16.dp))
                .border(1.dp, scheme.outlineVariant.copy(alpha = 0.68f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Event,
                    contentDescription = null,
                    tint = FashColors.Primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.chat_meeting_card_title),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = FashColors.Primary,
                )
            }

            Surface(
                color = meetingStatusChipBackground(st),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = stringResource(meetingStatusLabel(st)),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = meetingStatusChipOnColor(st),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }

            Text(
                text = stringResource(meetingStateDescriptionRes(st)),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                lineHeight = 18.sp,
            )

            if (whenText.isNotBlank()) {
                Text(
                    text = whenText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                )
            }

            if (meeting.safeZoneName.isNotBlank()) {
                Text(
                    text = meeting.safeZoneName,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = scheme.onSurface,
                )
            }

            if (meeting.locationUrl.isNotBlank()) {
                OutlinedButton(
                    onClick = { runCatching { uriHandler.openUri(meeting.locationUrl) } },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, FashColors.Primary.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.chat_meeting_open_maps))
                }
            } else if (st == "confirmed") {
                Text(
                    text = stringResource(R.string.chat_meeting_confirmed_no_maps_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.9f),
                    lineHeight = 18.sp,
                )
            }

            if (st == "confirmed" && onRecordOfflineDeal != null) {
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))
                OutlinedButton(
                    onClick = onRecordOfflineDeal,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, FashColors.Primary.copy(alpha = 0.45f)),
                ) {
                    Text(stringResource(R.string.chat_meeting_record_deal))
                }
            }

            if (st == "confirmed" && myOnMyWayAt.isNotBlank()) {
                Text(
                    text = stringResource(R.string.meeting_self_on_my_way),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = FashColors.Primary,
                    lineHeight = 18.sp,
                )
            }
            if (st == "confirmed" && otherOnMyWayAt.isNotBlank()) {
                Text(
                    text = stringResource(R.string.meeting_other_on_my_way),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = FashColors.Primary,
                    lineHeight = 18.sp,
                )
            }

            if (st == "confirmed" && (otherCheckInAt.isNotBlank() || myCheckInAt.isNotBlank())) {
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))
                if (otherCheckInAt.isNotBlank()) {
                    Text(
                        text = stringResource(
                            R.string.chat_meeting_other_checked_in_at,
                            formatTime(otherCheckInAt),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                    )
                }
                if (myCheckInAt.isNotBlank()) {
                    Text(
                        text = stringResource(
                            R.string.chat_meeting_you_checked_in_at,
                            formatTime(myCheckInAt),
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = scheme.onSurface,
                        lineHeight = 18.sp,
                    )
                }
            }

            if (st == "confirmed" && hasLinkedEscrowOrder) {
                Text(
                    text = stringResource(R.string.chat_meeting_check_in_not_order_complete),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                )
                Text(
                    text = stringResource(
                        if (isViewerBuyer) {
                            R.string.chat_meeting_next_steps_buyer_after_check_in
                        } else {
                            R.string.chat_meeting_next_steps_seller_after_check_in
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                )
            }

            if (showOnMyWayCta) {
                OutlinedButton(
                    onClick = { onOnMyWay?.invoke() },
                    enabled = !mutationInFlight,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, FashColors.Primary.copy(alpha = 0.45f)),
                ) {
                    MeetingMutationButtonContent(
                        loading = mutationInFlight,
                        label = stringResource(R.string.meeting_on_my_way),
                        progressColor = FashColors.Primary,
                    )
                }
            }

            if (st == "confirmed" && showCheckInCta) {
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))
                Text(
                    text = stringResource(R.string.chat_meeting_check_in_window_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.85f),
                )
                Button(
                    onClick = { onCheckIn?.invoke() },
                    enabled = !mutationInFlight,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
                ) {
                    MeetingMutationButtonContent(
                        loading = mutationInFlight,
                        label = stringResource(R.string.chat_meeting_check_in_cta),
                        progressColor = FashColors.Primary.fashReadableOn(),
                    )
                }
            }

            if (showConfirm || showCancel) {
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))
                when {
                    showConfirm -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedButton(
                                onClick = onWithdrawOrReject,
                                enabled = !mutationInFlight,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                MeetingMutationButtonContent(
                                    loading = mutationInFlight,
                                    label = stringResource(R.string.chat_meeting_reject),
                                    progressColor = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Button(
                                onClick = onConfirm,
                                enabled = !mutationInFlight,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
                            ) {
                                MeetingMutationButtonContent(
                                    loading = mutationInFlight,
                                    label = stringResource(R.string.chat_meeting_confirm),
                                    progressColor = FashColors.Primary.fashReadableOn(),
                                )
                            }
                        }
                    }
                    showCancel -> {
                        OutlinedButton(
                            onClick = onWithdrawOrReject,
                            enabled = !mutationInFlight,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            MeetingMutationButtonContent(
                                loading = mutationInFlight,
                                label = stringResource(R.string.chat_meeting_withdraw),
                                progressColor = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            Text(
                text = formatTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
fun MeetingIdentityReverifyDialog(
    visible: Boolean,
    /** When non-blank, shows “Open verification” opening this URL in Custom Tabs. */
    openVerificationUrl: String?,
    isAckInFlight: Boolean,
    onDismiss: () -> Unit,
    onAckCompleted: () -> Unit,
) {
    if (!visible) return
    val context = LocalContext.current
    val url = openVerificationUrl?.trim()?.takeIf { it.isNotEmpty() }
    AlertDialog(
        onDismissRequest = { if (!isAckInFlight) onDismiss() },
        title = { Text(stringResource(R.string.meeting_identity_reverify_dialog_title)) },
        text = { Text(stringResource(R.string.meeting_identity_reverify_dialog_body)) },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (url != null) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                CustomTabsIntent.Builder()
                                    .setShowTitle(true)
                                    .build()
                                    .launchUrl(context, Uri.parse(url))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAckInFlight,
                    ) {
                        Text(stringResource(R.string.meeting_identity_reverify_open_link))
                    }
                }
                Button(
                    onClick = onAckCompleted,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isAckInFlight,
                ) {
                    if (isAckInFlight) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.meeting_identity_reverify_ack_done))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isAckInFlight,
            ) {
                Text(stringResource(R.string.meeting_identity_reverify_close))
            }
        },
    )
}

internal fun isWithinMeetingActionWindow(scheduledAtIso: String, windowMinutes: Long = 30): Boolean {
    if (scheduledAtIso.isBlank()) return false
    return runCatching {
        val instant = Instant.parse(scheduledAtIso.replace(" ", "T"))
        val now = Instant.now()
        val start = instant.minusSeconds(windowMinutes * 60)
        val end = instant.plusSeconds(windowMinutes * 60)
        !now.isBefore(start) && !now.isAfter(end)
    }.getOrElse { false }
}

private fun buildScheduledAtRfc3339(
    selectedDateMillis: Long,
    hour: Int,
    minute: Int,
): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(selectedDateMillis).atZone(zone).toLocalDate()
    val zdt = ZonedDateTime.of(date, LocalTime.of(hour, minute, 0), zone)
    return zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

private fun formatSelectedDateTime(
    selectedDateMillis: Long,
    hour: Int,
    minute: Int,
    preferVi: Boolean,
): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(selectedDateMillis).atZone(zone).toLocalDate()
    val zdt = ZonedDateTime.of(date, LocalTime.of(hour, minute, 0), zone)
    val pattern = if (preferVi) "dd/MM/yyyy · HH:mm" else "MMM d, yyyy · HH:mm"
    return zdt.format(DateTimeFormatter.ofPattern(pattern, meetingAppLocale(preferVi)))
}

private fun meetingStatusLabel(status: String): Int = when (status.lowercase()) {
    "confirmed" -> R.string.chat_meeting_status_confirmed
    "cancelled" -> R.string.chat_meeting_status_cancelled
    else -> R.string.chat_meeting_status_pending
}

private fun meetingStateDescriptionRes(status: String): Int = when (status.lowercase()) {
    "confirmed" -> R.string.chat_meeting_state_desc_confirmed
    "cancelled" -> R.string.chat_meeting_state_desc_cancelled
    else -> R.string.chat_meeting_state_desc_pending
}

private fun meetingStatusChipBackground(status: String): Color = when (status.lowercase()) {
    "confirmed" -> Color(0xFFE8F5E9)
    "cancelled" -> Color(0xFFFFEBEE)
    else -> Color(0xFFFFF8E1)
}

private fun meetingStatusChipOnColor(status: String): Color = when (status.lowercase()) {
    "confirmed" -> Color(0xFF1B5E20)
    "cancelled" -> Color(0xFFB71C1C)
    else -> Color(0xFF856404)
}

internal fun formatMeetingWhen(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        val instant = Instant.parse(iso.replace(" ", "T"))
        val z = instant.atZone(ZoneId.systemDefault())
        z.format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm"))
    }.getOrElse { iso }
}
