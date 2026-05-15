package com.pc.fash_android_mobile.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.channels.Channel

/**
 * Runs all snackbar operations **one after another** on [snackbarHostState] so rapid API / VM events
 * never overlap visually (Material’s host still queues, but concurrent [showSnackbar] calls from many
 * collectors can interleave badly).
 */
typealias SerialSnackbarOp = suspend SnackbarHostState.() -> Unit

@Composable
fun rememberSerialSnackbarChannel(snackbarHostState: SnackbarHostState): (SerialSnackbarOp) -> Unit {
    val channel = remember { Channel<SerialSnackbarOp>(Channel.UNLIMITED) }
    LaunchedEffect(snackbarHostState) {
        for (op in channel) {
            op(snackbarHostState)
        }
    }
    return remember(channel) {
        { op -> channel.trySend(op).isSuccess }
    }
}
