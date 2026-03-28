package com.pc.fash_android_mobile.ui.post

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CreateListingStep3Screen(
    modifier: Modifier = Modifier,
    viewModel: PostViewModel,
    onCloseRequest: () -> Unit,
    onSubmitSuccess: () -> Unit,
) {
    val draft by viewModel.draft.collectAsState()
    val meProfile by viewModel.meProfile.collectAsState()
    val aestheticTags by viewModel.aestheticTags.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val context = LocalContext.current
    val uriResolver: (Uri) -> Pair<ByteArray, String>? = { uri ->
        val mimeType = context.contentResolver.getType(uri)?.takeIf { !it.contains('*') } ?: "image/jpeg"
        context.contentResolver.openInputStream(uri)?.use { stream ->
            Pair(stream.readBytes(), mimeType)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadStep3Data()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        CreateListingFlowHeader(
            step = 3,
            totalSteps = 3,
            onBackClick = { viewModel.prevStep() },
            onCloseClick = onCloseRequest,
            primaryLabelRes = R.string.create_listing_post_for_sale,
            onPrimaryClick = {
                viewModel.submitListing(uriResolver) { onSubmitSuccess() }
            },
            primaryEnabled = !isSubmitting,
            primaryLoading = isSubmitting,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FashTheme.spacing.editorialStart),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.create_listing_step3_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))

            ListingPreviewCard(
                draft = draft,
                meProfile = meProfile,
                aestheticTags = aestheticTags,
            )
        }

        Step3LegalFooter()
    }
}

@Composable
private fun ListingPreviewCard(
    draft: CreateListingDraft,
    meProfile: ProfileInfo?,
    aestheticTags: List<com.pc.fash_android_mobile.data.user.AestheticTag>,
) {
    val scheme = MaterialTheme.colorScheme
    val coverImageUrl = draft.imageUrls.firstOrNull()
    val coverImageUri = draft.imageUris.firstOrNull()
    val firstTagDisplayName = draft.aestheticTags.firstOrNull()?.let { tagId ->
        aestheticTags.find { it.id == tagId }?.let { tag ->
            tag.displayName.ifBlank { tag.name }
        } ?: tagId
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FashTheme.spacing.spacing3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(FashColors.Primary.copy(alpha = 0.2f)),
                ) {
                    meProfile?.avatarUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        val fullUrl = resolveImageUrl(url)
                        FashAsyncImage(
                            model = fullUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "@${meProfile?.username ?: "user"}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                        color = scheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.create_listing_seller_just_active),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                firstTagDisplayName?.let { tag ->
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(
                                scheme.surfaceContainerHigh,
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .padding(horizontal = FashTheme.spacing.spacing3),
            ) {
                val imageModel = when {
                    coverImageUrl?.isNotBlank() == true -> resolveImageUrl(coverImageUrl)
                    coverImageUri != null -> coverImageUri
                    else -> null
                }
                if (imageModel != null) {
                    FashAsyncImage(
                        model = imageModel,
                        contentDescription = draft.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.no_image),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = formatConditionDisplay(draft.condition),
                        style = MaterialTheme.typography.labelSmall,
                        color = FashColors.Primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = formatPrice(draft.priceVnd),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = androidx.compose.ui.graphics.Color.White,
                    )
                }
            }

            Text(
                text = draft.title.ifBlank { "—" },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = scheme.onSurface,
                modifier = Modifier.padding(
                    horizontal = FashTheme.spacing.spacing3,
                    vertical = FashTheme.spacing.spacing2,
                ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = FashTheme.spacing.spacing3,
                        vertical = FashTheme.spacing.spacing2,
                    ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = scheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.create_listing_location_default),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = scheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.create_listing_just_now),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Step3LegalFooter() {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = stringResource(R.string.create_listing_legal_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = scheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(top = 8.dp, bottom = FashTheme.spacing.spacing4),
    )
}

private fun formatConditionDisplay(condition: String): String = when (condition.lowercase()) {
    "new" -> "Mới"
    "like_new", "like new" -> "Như mới"
    "good" -> "Tốt"
    "fair" -> "Khá"
    else -> condition.ifBlank { "—" }
}

private fun formatPrice(vnd: Long): String =
    "₫ ${NumberFormat.getIntegerInstance(Locale.getDefault()).format(vnd).replace(',', '.')}"

private fun resolveImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val base = AppEnvironment.apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path".takeIf { path.isNotBlank() } ?: ""
}
