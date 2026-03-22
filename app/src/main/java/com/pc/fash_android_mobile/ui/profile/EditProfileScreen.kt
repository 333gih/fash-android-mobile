package com.pc.fash_android_mobile.ui.profile

import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.user.AestheticTag
import com.pc.fash_android_mobile.ui.components.FashPrimaryButton
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

private val AvatarSize = 96.dp
private val InputCorner = RoundedCornerShape(12.dp)
private val ChipCorner = RoundedCornerShape(20.dp)

@Composable
fun EditProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: EditProfileViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val username by viewModel.username.collectAsState()
    val bio by viewModel.bio.collectAsState()
    val selectedTagNames by viewModel.selectedTagNames.collectAsState()
    val avatarUrl by viewModel.avatarUrl.collectAsState()
    val coverImageUrl by viewModel.coverImageUrl.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val usernameAvailable by viewModel.usernameAvailable.collectAsState()
    val isCheckingUsername by viewModel.isCheckingUsername.collectAsState()
    val scheme = MaterialTheme.colorScheme

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.isNotEmpty()) {
                    viewModel.setAvatarFromBytes(bytes)
                }
            }
        }
    }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.isNotEmpty()) {
                    viewModel.setCoverFromBytes(bytes)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = scheme.surface,
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = FashColors.Primary)
            }
            return@Surface
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Header: X | Title | Lưu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = scheme.onSurface,
                    )
                }
                Text(
                    text = stringResource(R.string.edit_profile_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                val canSave = viewModel.canSave()
                Text(
                    text = stringResource(R.string.edit_profile_save),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (canSave && !isSubmitting) FashColors.Primary else scheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = canSave && !isSubmitting) {
                            viewModel.save(onSaved)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Cover + Avatar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                ) {
                    // Cover
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxSize()
                            .background(scheme.surfaceContainerHighest),
                    ) {
                        val cover = coverImageUrl ?: profile?.coverImageUrl?.takeIf { it.isNotBlank() }
                        if (cover != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(cover).crossfade(true).build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(
                                text = "COVER IMAGE",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(8.dp),
                            )
                        }
                        IconButton(
                            onClick = { coverPicker.launch("image/*") },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .size(40.dp)
                                .background(scheme.surface, CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = scheme.onSurface,
                            )
                        }
                    }

                    // Avatar overlapping bottom center
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 24.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(AvatarSize)
                                .clip(CircleShape)
                                .background(scheme.surfaceContainerHigh)
                                .border(3.dp, scheme.surface, CircleShape),
                        ) {
                            val avatar = avatarUrl ?: profile?.avatarUrl?.takeIf { it.isNotBlank() }
                            if (avatar != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(avatar).crossfade(true).build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                            IconButton(
                                onClick = { avatarPicker.launch("image/*") },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 4.dp, y = 4.dp)
                                    .size(32.dp)
                                    .background(FashColors.Primary, CircleShape),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = FashColors.OnPrimary,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // Display name
                    LabeledInput(
                        label = stringResource(R.string.edit_profile_display_name_label),
                        value = displayName,
                        onValueChange = viewModel::onDisplayNameChange,
                        placeholder = stringResource(R.string.edit_profile_display_name_placeholder),
                    )

                    // Username with checkmark
                    UsernameInput(
                        value = username,
                        onValueChange = viewModel::onUsernameChange,
                        isValid = viewModel.isUsernameValid() && (usernameAvailable == true || username == profile?.username?.trim()),
                        isChecking = isCheckingUsername,
                    )

                    // Bio with counter
                    BioInput(
                        value = bio,
                        onValueChange = viewModel::onBioChange,
                        maxLength = 150,
                        placeholder = stringResource(R.string.edit_profile_bio_placeholder),
                    )

                    // Style chips
                    StyleChipsSection(
                        label = stringResource(R.string.edit_profile_style_label),
                        tags = tags,
                        selectedNames = selectedTagNames,
                        onToggle = viewModel::toggleTag,
                    )
                }
            }

            // Bottom save button
            FashPrimaryButton(
                onClick = {
                    if (viewModel.canSave() && !isSubmitting) {
                        viewModel.save(onSaved)
                    }
                },
                enabled = viewModel.canSave() && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = FashColors.OnPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.edit_profile_save_changes),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun LabeledInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            shape = InputCorner,
            color = scheme.surfaceContainerHighest,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                singleLine = true,
                cursorBrush = SolidColor(FashColors.Primary),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun UsernameInput(
    value: String,
    onValueChange: (String) -> Unit,
    isValid: Boolean,
    isChecking: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.edit_profile_username_label),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            shape = InputCorner,
            color = scheme.surfaceContainerHighest,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isValid && value.isNotBlank()) FashColors.Success.copy(alpha = 0.5f)
                else scheme.outlineVariant.copy(alpha = 0.5f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "@",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = FashColors.Primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                    singleLine = true,
                    cursorBrush = SolidColor(FashColors.Primary),
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.edit_profile_username_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                            inner()
                        }
                    },
                )
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = scheme.primary,
                    )
                } else if (isValid && value.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = FashColors.Success,
                    )
                }
            }
        }
    }
}

@Composable
private fun BioInput(
    value: String,
    onValueChange: (String) -> Unit,
    maxLength: Int,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.edit_profile_bio_label),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.edit_profile_bio_counter, value.length, maxLength),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            shape = InputCorner,
            color = scheme.surfaceContainerHighest,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .height(100.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                maxLines = 4,
                cursorBrush = SolidColor(FashColors.Primary),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun StyleChipsSection(
    label: String,
    tags: List<AestheticTag>,
    selectedNames: Set<String>,
    onToggle: (AestheticTag) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tags.forEach { tag ->
                val selected = selectedNames.contains(tag.name)
                Surface(
                    shape = ChipCorner,
                    color = if (selected) FashColors.Primary else scheme.surfaceContainerHighest,
                    modifier = Modifier.clickable { onToggle(tag) },
                ) {
                    Text(
                        text = tag.displayName.ifBlank { tag.name },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) FashColors.OnPrimary else scheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
