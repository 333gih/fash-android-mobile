package com.pc.fash_android_mobile.ui.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.order.OrderCancelReasonOption
import com.pc.fash_android_mobile.data.order.OrderCancelReasons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Buyer cancel: pick reason → POST cancel. Server records reason and posts `order_cancelled` chat card.
 * No in-app star rating after cancel (reason is enough for tracing).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderCancelFlowHost(
    orderId: String?,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit,
) {
    if (orderId.isNullOrBlank()) return
    val app = LocalContext.current.applicationContext as FashApplication
    val orderRepository = remember { app.orderRepository }
    val scope = rememberCoroutineScope()

    var selectedReason by remember { mutableStateOf<OrderCancelReasonOption?>(null) }
    var reasonNote by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.order_cancel_reason_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                stringResource(R.string.order_cancel_reason_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OrderCancelReasons.options.forEach { opt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !busy) { selectedReason = opt },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedReason?.code == opt.code,
                        onClick = { selectedReason = opt },
                        enabled = !busy,
                    )
                    Text(
                        stringResource(opt.labelRes),
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (selectedReason?.requiresNote == true) {
                OutlinedTextField(
                    value = reasonNote,
                    onValueChange = { reasonNote = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.order_cancel_reason_note_label)) },
                    placeholder = { Text(stringResource(R.string.order_cancel_reason_note_hint)) },
                    minLines = 2,
                    enabled = !busy,
                )
            }
            errorText?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    val reason = selectedReason ?: return@Button
                    if (reason.requiresNote && reasonNote.trim().length < 3) {
                        errorText = app.getString(R.string.order_cancel_reason_note_required)
                        return@Button
                    }
                    busy = true
                    errorText = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            orderRepository.cancelOrder(orderId, reason.code, reasonNote)
                        }
                        busy = false
                        result.fold(
                            onSuccess = {
                                onSuccess(orderId)
                                onDismiss()
                            },
                            onFailure = { e ->
                                errorText = e.message ?: app.getString(R.string.order_cancel_failed)
                            },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy && selectedReason != null,
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                } else {
                    Text(stringResource(R.string.order_cancel_confirm_action))
                }
            }
        }
    }
}
