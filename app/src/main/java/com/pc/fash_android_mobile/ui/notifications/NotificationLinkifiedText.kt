package com.pc.fash_android_mobile.ui.notifications

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink

private val urlPattern = Regex("""(?i)(https?://[^\s<>"]+|www\.[^\s<>"]+)""")

@Composable
fun NotificationLinkifiedText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val linkStyle = remember(scheme.primary) {
        TextLinkStyles(
            style = SpanStyle(
                color = scheme.primary,
                textDecoration = TextDecoration.Underline,
            ),
        )
    }
    val annotated = remember(text, linkStyle) {
        buildAnnotatedString {
            var cursor = 0
            for (match in urlPattern.findAll(text)) {
                val start = match.range.first
                if (start > cursor) {
                    append(text.substring(cursor, start))
                }
                val raw = match.value.trimEnd('.', ',', ';', ')')
                val url = if (raw.startsWith("http", ignoreCase = true)) raw else "https://$raw"
                withLink(
                    LinkAnnotation.Url(
                        url = url,
                        styles = linkStyle,
                        linkInteractionListener = {
                            runCatching { uriHandler.openUri(url) }
                        },
                    ),
                ) {
                    append(raw)
                }
                cursor = match.range.last + 1
            }
            if (cursor < text.length) {
                append(text.substring(cursor))
            }
        }
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        color = scheme.onSurface,
    )
}
