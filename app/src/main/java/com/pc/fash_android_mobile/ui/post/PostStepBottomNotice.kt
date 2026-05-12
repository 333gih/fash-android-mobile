package com.pc.fash_android_mobile.ui.post

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/**
 * Bottom notice card aligned with [com.pc.fash_android_mobile.ui.login.OtpHelpBottomCard]:
 * tonal surface, primary accent bar, info icon, title, and bullet lines (split on `\n`).
 */
@Composable
fun PostFlowNoticeCard(
    text: String,
    horizontalPadding: Dp,
    title: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusCard)
    val lines = remember(text) {
        text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf(text.trim()) }
    }
    val titleText = title ?: stringResource(R.string.post_notice_title)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(top = 8.dp, bottom = 12.dp),
        shape = shape,
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FashTheme.spacing.spacing4),
            horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing3),
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(68.dp)
                    .background(
                        FashColors.Primary.copy(alpha = 0.58f),
                        RoundedCornerShape(2.dp),
                    ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = FashColors.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                    )
                }
                lines.forEach { line ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "\u2022",
                            style = MaterialTheme.typography.bodySmall,
                            color = FashColors.Primary.copy(alpha = 0.72f),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Same chrome as [PostFlowNoticeCard], with inline links for Terms and Privacy (portal publish pages).
 */
@Composable
fun PostFlowLegalNoticeCard(
    horizontalPadding: Dp,
    title: String? = null,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusCard)
    val titleText = title ?: stringResource(R.string.post_notice_title)
    val linkColor = scheme.primary
    val linkStyle = TextLinkStyles(
        style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
    )
    val annotated = buildAnnotatedString {
        append(stringResource(R.string.create_listing_legal_prefix))
        withLink(
            LinkAnnotation.Clickable(
                tag = "terms",
                styles = linkStyle,
                linkInteractionListener = { onTermsClick() },
            ),
        ) {
            append(stringResource(R.string.create_listing_legal_terms_link))
        }
        append(stringResource(R.string.create_listing_legal_mid))
        withLink(
            LinkAnnotation.Clickable(
                tag = "privacy",
                styles = linkStyle,
                linkInteractionListener = { onPrivacyClick() },
            ),
        ) {
            append(stringResource(R.string.create_listing_legal_privacy_link))
        }
        append(stringResource(R.string.create_listing_legal_suffix))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(top = 8.dp, bottom = 12.dp),
        shape = shape,
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FashTheme.spacing.spacing4),
            horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing3),
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(68.dp)
                    .background(
                        FashColors.Primary.copy(alpha = 0.58f),
                        RoundedCornerShape(2.dp),
                    ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = FashColors.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "\u2022",
                        style = MaterialTheme.typography.bodySmall,
                        color = FashColors.Primary.copy(alpha = 0.72f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = scheme.onSurfaceVariant,
                            lineHeight = 20.sp,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun PostStepNoticeText(text: String, horizontalPadding: Dp) {
    PostFlowNoticeCard(text = text, horizontalPadding = horizontalPadding)
}

/**
 * Scrollable step body with an optional hint at the bottom (OTP-style card, always visible when set).
 */
@Composable
fun PostStepScrollWithBottomNotice(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp,
    bottomNotice: String?,
    scrollState: ScrollState,
    bottomExtra: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hasBottomText = !bottomNotice.isNullOrBlank()
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = horizontalPadding),
            ) {
                content()
            }
        }
        if (hasBottomText) {
            PostStepNoticeText(text = bottomNotice!!, horizontalPadding = horizontalPadding)
        }
        bottomExtra?.invoke()
    }
}

/**
 * Lazy list step body with an optional hint at the bottom (OTP-style card, always visible when set).
 */
@Composable
fun PostStepLazyListWithBottomNotice(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp,
    bottomNotice: String?,
    listState: LazyListState,
    header: @Composable ColumnScope.() -> Unit,
    lazyContent: LazyListScope.() -> Unit,
) {
    val hasBottomText = !bottomNotice.isNullOrBlank()
    Column(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
        ) {
            header()
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
                contentPadding = PaddingValues(horizontal = horizontalPadding),
            ) {
                lazyContent()
            }
        }
        if (hasBottomText) {
            PostStepNoticeText(text = bottomNotice!!, horizontalPadding = horizontalPadding)
        }
    }
}
