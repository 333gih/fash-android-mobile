package com.pc.fash_android_mobile.ui.post

import android.net.Uri
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

@Composable
fun CreateListingStep1Screen(
    modifier: Modifier = Modifier,
    viewModel: PostViewModel,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val draft by viewModel.draft.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val relaxPostSteps = BuildConfig.POST_STEPS_RELAX_VALIDATION
    val uriResolver: (Uri) -> Pair<ByteArray, String>? = { uri ->
        val mimeType = context.contentResolver.getType(uri)?.takeIf { !it.contains('*') } ?: "image/jpeg"
        context.contentResolver.openInputStream(uri)?.use { stream ->
            Pair(stream.readBytes(), mimeType)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris: List<Uri> ->
        viewModel.setImageUris(uris)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        CreateListingFlowHeader(
            step = 1,
            totalSteps = 3,
            onBackClick = null,
            onCloseClick = onClose,
            primaryLabelRes = R.string.create_listing_next,
            onPrimaryClick = {
                if (isUploading) return@CreateListingFlowHeader
                scope.launch {
                    val ok = when {
                        relaxPostSteps && draft.imageUris.isEmpty() -> true
                        else -> viewModel.uploadImages(uriResolver)
                    }
                    if (ok) viewModel.nextStep()
                }
            },
            primaryEnabled = draft.canProceedFromStep1() || relaxPostSteps,
            primaryLoading = isUploading,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FashTheme.spacing.editorialStart),
        ) {
            Text(
                text = stringResource(R.string.create_listing_add_photos),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(24.dp))

            AddPhotoBox(
                onClick = { imagePicker.launch("image/*") },
            )

            if (draft.imageUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                ImagePreviewRow(
                    imageUris = draft.imageUris,
                    onRemove = { viewModel.removeImage(it) },
                    onAddMore = { imagePicker.launch("image/*") },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Step1AdvertisingPanel()
    }
}

@Composable
private fun Step1AdvertisingPanel() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(bottom = FashTheme.spacing.spacing3),
    ) {
        TipBox()
    }
}

@Composable
private fun AddPhotoBox(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.create_listing_add_photo),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.create_listing_photo_tip),
                style = MaterialTheme.typography.bodySmall,
                color = FashColors.Primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ImagePreviewRow(
    imageUris: List<Uri>,
    onRemove: (Int) -> Unit,
    onAddMore: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        imageUris.forEachIndexed { index, uri ->
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                FashAsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                if (index == 0) {
                    Text(
                        text = stringResource(R.string.create_listing_cover_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(FashColors.Primary)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
                IconButton(
                    onClick = { onRemove(index) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        if (imageUris.size < 6) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable(onClick = onAddMore),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TipBox() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = FashColors.Primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Lightbulb,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = FashColors.Primary,
        )
        Column {
            Text(
                text = stringResource(R.string.create_listing_tip_title),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.create_listing_tip_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
