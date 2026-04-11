package com.pc.fash_android_mobile.ui.chat

import android.net.Uri
import android.text.format.DateFormat
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.chat.ChatMessage
import com.pc.fash_android_mobile.data.chat.MeetingAppointmentPayload
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MeetingProposalBottomSheet(
    isLoading: Boolean,
    /** When set with [onViewOrder], shows a link to open order detail (e.g. escrow + meetup). */
    linkedOrderId: String? = null,
    onViewOrder: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSubmit: (
        locationUrl: String,
        scheduledAtIso: String,
        reminderEnabled: Boolean,
        reminderOffsetMinutes: Int,
    ) -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var locationUrl by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = 9,
        initialMinute = 0,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    var reminderEnabled by remember { mutableStateOf(true) }
    var offset by remember { mutableIntStateOf(60) }

    val offsets = listOf(15 to R.string.chat_meeting_reminder_15, 60 to R.string.chat_meeting_reminder_60, 1440 to R.string.chat_meeting_reminder_1440)

    val scheduledAtIso = remember(selectedDateMillis, timePickerState.hour, timePickerState.minute) {
        selectedDateMillis?.let { buildScheduledAtRfc3339(it, timePickerState.hour, timePickerState.minute) }
    }
    val dateTimeSummary = remember(selectedDateMillis, timePickerState.hour, timePickerState.minute) {
        selectedDateMillis?.let { formatSelectedDateTime(it, timePickerState.hour, timePickerState.minute) }
            ?: ""
    }
    val canSubmit = locationUrl.isNotBlank() && selectedDateMillis != null && scheduledAtIso != null

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
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
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.chat_meeting_time_picker_title)) },
            confirmButton = {
                TextButton(onClick = { showTimePicker = false }) {
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

    ModalBottomSheet(
        onDismissRequest = {
            if (!isLoading) {
                keyboard?.hide()
                focusManager.clearFocus(force = true)
                onDismiss()
            }
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_meeting_sheet_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.chat_meeting_sheet_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = locationUrl,
                onValueChange = { locationUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.chat_meeting_field_maps_url)) },
                placeholder = { Text(stringResource(R.string.chat_meeting_field_maps_hint)) },
                singleLine = false,
                minLines = 2,
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FashColors.Primary,
                    focusedLabelColor = FashColors.Primary,
                ),
            )

            Text(
                text = stringResource(R.string.chat_meeting_datetime_label),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                ) {
                    Icon(Icons.Outlined.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.chat_meeting_pick_date))
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                ) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.chat_meeting_pick_time))
                }
            }
            OutlinedTextField(
                value = dateTimeSummary,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.chat_meeting_datetime_placeholder)) },
                singleLine = true,
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FashColors.Primary,
                    focusedLabelColor = FashColors.Primary,
                ),
            )
            if (scheduledAtIso != null) {
                Text(
                    text = stringResource(R.string.chat_meeting_iso_preview_label) + ": " + scheduledAtIso,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = stringResource(R.string.chat_meeting_reminder_label),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                offsets.forEach { (minutes, labelRes) ->
                    FilterChip(
                        selected = offset == minutes,
                        onClick = { offset = minutes },
                        label = { Text(stringResource(labelRes)) },
                        enabled = !isLoading,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = reminderEnabled,
                    onClick = { reminderEnabled = !reminderEnabled },
                    label = { Text(stringResource(R.string.chat_meeting_reminder_toggle)) },
                    enabled = !isLoading,
                )
            }

            val oid = linkedOrderId?.trim()?.takeIf { it.isNotEmpty() }
            if (oid != null && onViewOrder != null) {
                TextButton(
                    onClick = { onViewOrder(oid) },
                    enabled = !isLoading,
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
                    )
                },
                enabled = canSubmit && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FashColors.Primary,
                    contentColor = FashColors.Primary.fashReadableOn(),
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
        )
    }
}

@Composable
fun MeetingProposalMessageCard(
    message: ChatMessage,
    meeting: MeetingAppointmentPayload,
    formatTime: (String) -> String,
    mutationInFlight: Boolean,
    onConfirm: () -> Unit,
    /** Recipient rejects or proposer withdraws — maps to POST …/cancel. */
    onWithdrawOrReject: () -> Unit,
    /** C2C: after both sides confirmed, optional entry to `POST /deals`. */
    onRecordOfflineDeal: (() -> Unit)? = null,
    /** Confirmed meetup: `POST …/check-in` (±30m of scheduled time). */
    onCheckIn: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val whenText = remember(meeting.scheduledAt) { formatMeetingWhen(meeting.scheduledAt) }
    val st = meeting.status.lowercase()
    val showConfirm = st == "pending" && !meeting.isProposerMe
    val showCancel = st == "pending" && meeting.isProposerMe

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

            Text(
                text = stringResource(meetingStatusLabel(st)),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = scheme.onSurface,
            )

            if (whenText.isNotBlank()) {
                Text(
                    text = whenText,
                    style = MaterialTheme.typography.bodyMedium,
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

            if (st == "confirmed" && onCheckIn != null) {
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))
                Button(
                    onClick = onCheckIn,
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
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = onWithdrawOrReject,
                                enabled = !mutationInFlight,
                                modifier = Modifier.weight(1f),
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
                                modifier = Modifier.weight(1f),
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
): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(selectedDateMillis).atZone(zone).toLocalDate()
    val zdt = ZonedDateTime.of(date, LocalTime.of(hour, minute, 0), zone)
    return zdt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm"))
}

private fun meetingStatusLabel(status: String): Int = when (status) {
    "confirmed" -> R.string.chat_meeting_status_confirmed
    "cancelled" -> R.string.chat_meeting_status_cancelled
    else -> R.string.chat_meeting_status_pending
}

private fun formatMeetingWhen(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        val instant = Instant.parse(iso.replace(" ", "T"))
        val z = instant.atZone(ZoneId.systemDefault())
        z.format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm"))
    }.getOrElse { iso }
}
