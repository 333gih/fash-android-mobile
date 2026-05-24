package com.pc.fash_android_mobile.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.ui.UiDialogMessage

/**
 * Global success / error / info dialog aligned with Fash editorial surfaces and Material 3.
 *
 * @param bottomOverlayInset Space reserved above the gesture/nav bar so tab bar and/or chat composer stay visible.
 * @param onDismissAll Called when the user taps the backdrop or back — clears the entire queue.
 *   Falls back to [onDismiss] when not provided for backwards compatibility.
 */
@Composable
fun FashGlobalDialogHost(
    message: UiDialogMessage?,
    onDismiss: () -> Unit,
    bottomOverlayInset: Dp = 0.dp,
    onDismissAll: (() -> Unit)? = null,
) {
    if (message == null) return
    val dismissAll = onDismissAll ?: onDismiss
    val scheme = MaterialTheme.colorScheme
    Dialog(
        onDismissRequest = dismissAll,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // Let the activity show through in the reserved bottom region (nav bar + composer).
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            // Allow system-level outside-tap dismissal as a safety fallback in addition to
            // the manual backdrop click below, so the dialog can never get stuck.
            dismissOnClickOutside = true,
        ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Transparent),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                // Backdrop — tap anywhere outside the card to clear all queued messages
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(scheme.scrim.copy(alpha = 0.48f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = dismissAll,
                        ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = message,
                        transitionSpec = {
                            (
                                fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                    scaleIn(
                                        initialScale = 0.94f,
                                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                                    )
                                ) togetherWith (
                                fadeOut(animationSpec = tween(180)) +
                                    scaleOut(
                                        targetScale = 0.96f,
                                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                                    )
                                )
                        },
                        label = "fashGlobalDialogMessage",
                    ) { msg ->
                        FashGlobalDialogCard(
                            message = msg,
                            onDismiss = onDismiss,
                        )
                    }
                }
            }
            if (bottomOverlayInset > 0.dp) {
                Spacer(Modifier.height(bottomOverlayInset))
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun FashGlobalDialogCard(
    message: UiDialogMessage,
    onDismiss: () -> Unit,
) {
    val resolvedTitle = message.title ?: when (message) {
        is UiDialogMessage.Success -> stringResource(R.string.dialog_title_success)
        is UiDialogMessage.Error -> stringResource(R.string.dialog_title_error)
        is UiDialogMessage.Info -> stringResource(R.string.dialog_title_info)
    }
    val scheme = MaterialTheme.colorScheme
    val (iconVector, iconTint, iconCircleBg) = when (message) {
        is UiDialogMessage.Success -> Triple(
            Icons.Default.CheckCircle,
            scheme.primary,
            scheme.primaryContainer.copy(alpha = 0.38f),
        )
        is UiDialogMessage.Error -> Triple(
            Icons.Default.ErrorOutline,
            scheme.tertiary,
            scheme.tertiary.copy(alpha = 0.12f),
        )
        is UiDialogMessage.Info -> Triple(
            Icons.Default.Info,
            scheme.secondary,
            scheme.secondaryContainer.copy(alpha = 0.45f),
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            // Cap height so the card never extends off-screen; the message body scrolls inside
            .heightIn(max = 480.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* consume */ },
            ),
        shape = RoundedCornerShape(22.dp),
        color = scheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(
            1.dp,
            scheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Scrollable area: icon + title + message body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 22.dp, bottom = 4.dp)
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(iconCircleBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = resolvedTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            // OK button always pinned at the bottom so it's never hidden
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = scheme.primary,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.dialog_ok),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
