package com.pc.fash_android_mobile.ui.invite

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.user.UserRepository
import com.pc.fash_android_mobile.deeplink.InviteDeepLinks
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteFriendsScreen(
    modifier: Modifier = Modifier,
    referrerUsername: String?,
    userRepository: UserRepository,
    onBack: () -> Unit,
    onUserMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var referralToken by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(referrerUsername, userRepository) {
        referralToken = withContext(Dispatchers.IO) {
            userRepository.getReferralInviteTokenOrNull()
        }
    }
    val playUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
    val httpsInviteUrl = InviteDeepLinks.publicInviteHttpsUrl(
        referrerUsername?.takeIf { it.isNotBlank() },
        referralToken,
    )
    val shareBody = stringResource(R.string.invite_share_body_format, httpsInviteUrl, playUrl)
    val shareSubject = stringResource(R.string.invite_share_subject)

    val copyLabel = stringResource(R.string.invite_copy_clipboard_label)
    val onCopy: () -> Unit = {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(copyLabel, shareBody))
        onUserMessage(context.getString(R.string.invite_copied))
    }

    val onShare: () -> Unit = {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, shareSubject)
            putExtra(Intent.EXTRA_TEXT, shareBody)
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(send, context.getString(R.string.invite_share_chooser_title)),
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.invite_screen_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = FashColors.Primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                color = Color.Transparent,
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    FashColors.Primary.copy(alpha = 0.22f),
                                    scheme.secondaryContainer.copy(alpha = 0.55f),
                                ),
                            ),
                        )
                        .padding(20.dp),
                ) {
                    Text(
                        text = stringResource(R.string.invite_screen_hero_kicker),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.invite_screen_hero_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.invite_screen_hero_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface.copy(alpha = 0.92f),
                    )
                }
            }

            Text(
                text = stringResource(R.string.invite_benefits_section_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
            )

            InviteBenefitRow(
                icon = Icons.Outlined.Star,
                title = stringResource(R.string.invite_benefit_priority_title),
                body = stringResource(R.string.invite_benefit_priority_body),
            )
            InviteBenefitRow(
                icon = Icons.Outlined.CardGiftcard,
                title = stringResource(R.string.invite_benefit_promos_title),
                body = stringResource(R.string.invite_benefit_promos_body),
            )
            InviteBenefitRow(
                icon = Icons.Outlined.Groups,
                title = stringResource(R.string.invite_benefit_circle_title),
                body = stringResource(R.string.invite_benefit_circle_body),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onShare,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(stringResource(R.string.invite_cta_share))
            }

            OutlinedButton(
                onClick = onCopy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(stringResource(R.string.invite_cta_copy))
            }

            Text(
                text = stringResource(R.string.invite_fine_print),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun InviteBenefitRow(
    icon: ImageVector,
    title: String,
    body: String,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FashColors.Primary,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}
