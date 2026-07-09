package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.theme.FashShapes

/** Visual tone for transient bottom messages (API errors, confirmations, hints). */
enum class FashSnackbarKind {
    Error,
    Success,
    Info,
}

/**
 * Heuristic for API / VM snackbar strings — avoids touching every call site.
 */
fun inferFashSnackbarKind(message: String): FashSnackbarKind {
    val m = message.trim().lowercase()
    if (m.isEmpty()) return FashSnackbarKind.Info
    val errorHints = listOf(
        "http ",
        "failed",
        "failure",
        "error",
        "lỗi",
        "thất bại",
        "invalid",
        "denied",
        "required",
        "conflict",
        "not found",
        "không thể",
        "không thành công",
        "permission",
        "server error",
        "bad request",
        "authentication",
        "unauthorized",
        "forbidden",
        "timeout",
        "hết hạn",
        "từ chối",
    )
    if (errorHints.any { m.contains(it) }) return FashSnackbarKind.Error
    val successHints = listOf(
        "success",
        "thành công",
        "đã lưu",
        "đã thêm",
        "đã gỡ",
        "đã gửi",
        "đã cập nhật",
        "saved",
        "sent",
        "added",
        "removed",
        "available again",
        "có thể mua lại",
        "review sent",
        "đánh giá",
        "yêu thích",
        "mong muốn",
        "wishlist",
        "đã theo dõi",
        "following",
    )
    if (successHints.any { m.contains(it) }) return FashSnackbarKind.Success
    return FashSnackbarKind.Info
}

/**
 * Editorial snackbar palette — **one lifted surface** for every tone; only accent + icon tint vary.
 * Uses [MaterialTheme.colorScheme] (wired from [com.pc.fash_android_mobile.ui.theme.FashColorTokens]).
 */
private data class FashSnackbarColors(
    val container: Color,
    val content: Color,
    val contentSecondary: Color,
    val accent: Color,
    val iconTint: Color,
    val iconBg: Color,
    val action: Color,
    val border: Color,
)

@Composable
private fun colorsForKind(kind: FashSnackbarKind): FashSnackbarColors {
    val scheme = MaterialTheme.colorScheme
    // Shared editorial card — same as FashEditorialCard / FashGlobalDialogCard surfaces.
    val base = FashSnackbarColors(
        container = scheme.surfaceContainerHighest,
        content = scheme.onSurface,
        contentSecondary = scheme.onSurfaceVariant,
        accent = scheme.primary,
        iconTint = scheme.primary,
        iconBg = scheme.surfaceContainer,
        action = scheme.primary,
        border = scheme.outlineVariant.copy(alpha = 0.48f),
    )
    return when (kind) {
        // Warm terracotta accent — editorial caution, not alarm red/green.
        FashSnackbarKind.Error -> base.copy(
            accent = scheme.tertiary,
            iconTint = scheme.tertiary,
            iconBg = scheme.tertiary.copy(alpha = 0.12f),
            action = scheme.tertiary,
        )
        // Coral brand — positive feedback stays on-brand.
        FashSnackbarKind.Success -> base.copy(
            accent = scheme.primary,
            iconTint = scheme.primary,
            iconBg = scheme.primaryContainer.copy(alpha = 0.38f),
            action = scheme.primary,
        )
        // Warm brown secondary — neutral hints.
        FashSnackbarKind.Info -> base.copy(
            accent = scheme.secondary,
            iconTint = scheme.secondary,
            iconBg = scheme.secondaryContainer.copy(alpha = 0.45f),
            action = scheme.primary,
        )
    }
}

private fun iconForKind(kind: FashSnackbarKind): ImageVector = when (kind) {
    FashSnackbarKind.Error -> Icons.Default.ErrorOutline
    FashSnackbarKind.Success -> Icons.Default.CheckCircle
    FashSnackbarKind.Info -> Icons.Default.Info
}

/**
 * App-wide snackbar — warm editorial card aligned with [FashGlobalDialogCard] and feed surfaces.
 *
 * All tones share the same container; a 4dp accent stripe + icon tint signals error / success / info
 * without Material green/red fills that clash with the coral palette.
 */
@Composable
fun FashSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    additionalBottomInset: Dp = 0.dp,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = additionalBottomInset + 8.dp)
            .navigationBarsPadding()
            .imePadding(),
        snackbar = { data ->
            FashSnackbarCard(data = data)
        },
    )
}

@Composable
private fun FashSnackbarCard(
    data: SnackbarData,
    modifier: Modifier = Modifier,
) {
    val message = data.visuals.message
    val kind = inferFashSnackbarKind(message)
    val colors = colorsForKind(kind)
    val shape = FashShapes.large
    val actionLabel = data.visuals.actionLabel?.trim().orEmpty()
    val showDismiss = data.visuals.withDismissAction && actionLabel.isEmpty()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.14f),
            ),
        shape = shape,
        color = colors.container,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(52.dp)
                    .background(colors.accent),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconForKind(kind),
                        contentDescription = null,
                        tint = colors.iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = colors.content,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (actionLabel.isNotEmpty()) {
                    TextButton(
                        onClick = { data.performAction() },
                        modifier = Modifier.padding(start = 4.dp),
                    ) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = colors.action,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else if (showDismiss) {
                    IconButton(
                        onClick = { data.dismiss() },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = colors.contentSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
