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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.FashSnackbarHost
import com.pc.fash_android_mobile.ui.components.FashPrimaryButton
import com.pc.fash_android_mobile.ui.onboarding.ProfileSetupSizingSection
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import com.pc.fash_android_mobile.ui.theme.fashReadableOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val AvatarSize = 88.dp
private val CoverBannerHeight = 132.dp
/** Vertical gap between major form blocks (display name, bio, style, sizing). */
private val FormSectionSpacing = 12.dp
private val LabelToFieldGap = 4.dp
private val InputCorner = RoundedCornerShape(12.dp)
private val ChipCorner = RoundedCornerShape(20.dp)

private const val DISPLAY_NAME_MAX = 50

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: EditProfileViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val profile by viewModel.profile.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val username by viewModel.username.collectAsState()
    val bio by viewModel.bio.collectAsState()
    val selectedTagIds by viewModel.selectedTagIds.collectAsState()
    val avatarUrl by viewModel.avatarUrl.collectAsState()
    val coverImageUrl by viewModel.coverImageUrl.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val referenceSize by viewModel.referenceSize.collectAsState()
    val measurementUnit by viewModel.measurementUnit.collectAsState()
    val measurementHem by viewModel.measurementHem.collectAsState()
    val measurementChest by viewModel.measurementChest.collectAsState()
    val measurementLength by viewModel.measurementLength.collectAsState()
    val measurementShoulders by viewModel.measurementShoulders.collectAsState()
    val measurementSleeve by viewModel.measurementSleeve.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val usernameAvailable by viewModel.usernameAvailable.collectAsState()
    val isCheckingUsername by viewModel.isCheckingUsername.collectAsState()
    val scheme = MaterialTheme.colorScheme

    var showStyleSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { u ->
            scope.launch {
                val pair = withContext(Dispatchers.IO) {
                    val mimeType = context.contentResolver.getType(u)
                        ?.takeIf { !it.contains('*') } ?: "image/jpeg"
                    context.contentResolver.openInputStream(u)?.use { stream ->
                        Pair(stream.readBytes(), mimeType)
                    }
                }
                pair?.let { (bytes, mime) ->
                    if (bytes.isNotEmpty()) viewModel.setAvatarFromBytes(bytes, mime)
                }
            }
        }
    }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { u ->
            scope.launch {
                val pair = withContext(Dispatchers.IO) {
                    val mimeType = context.contentResolver.getType(u)
                        ?.takeIf { !it.contains('*') } ?: "image/jpeg"
                    context.contentResolver.openInputStream(u)?.use { stream ->
                        Pair(stream.readBytes(), mimeType)
                    }
                }
                pair?.let { (bytes, mime) ->
                    if (bytes.isNotEmpty()) viewModel.setCoverFromBytes(bytes, mime)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = scheme.surfaceContainerLow,
        snackbarHost = { FashSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.edit_profile_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.surface,
                ),
            )
        },
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(scheme.surfaceContainerLow),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = FashColors.Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(scheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .background(scheme.surfaceContainerLow),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CoverBannerHeight),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxSize()
                                .background(scheme.surfaceVariant.copy(alpha = 0.55f)),
                        ) {
                            val cover = coverImageUrl ?: profile?.coverImageUrl?.takeIf { it.isNotBlank() }
                            if (cover != null) {
                                FashAsyncImage(
                                    model = cover,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.edit_profile_cover_placeholder),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = scheme.onSurfaceVariant.copy(alpha = 0.72f),
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
                                    .size(44.dp)
                                    .background(scheme.surfaceContainerHighest, CircleShape)
                                    .border(1.dp, scheme.outlineVariant.copy(alpha = 0.45f), CircleShape),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = stringResource(R.string.edit_profile_change_photo_cd),
                                    modifier = Modifier.size(22.dp),
                                    tint = FashColors.Primary,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = 12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(AvatarSize)
                                    .clip(CircleShape)
                                    .background(scheme.surfaceContainerHighest)
                                    .border(2.dp, scheme.outlineVariant.copy(alpha = 0.4f), CircleShape),
                            ) {
                                val avatar = avatarUrl ?: profile?.avatarUrl?.takeIf { it.isNotBlank() }
                                if (avatar != null) {
                                    FashAsyncImage(
                                        model = avatar,
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
                                        contentDescription = stringResource(R.string.edit_profile_change_photo_cd),
                                        modifier = Modifier.size(16.dp),
                                        tint = FashColors.Primary.fashReadableOn(),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = FashTheme.spacing.editorialStart)
                            .padding(top = 4.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(FormSectionSpacing),
                    ) {
                        DisplayNameInput(
                            label = stringResource(R.string.edit_profile_display_name_label),
                            value = displayName,
                            onValueChange = viewModel::onDisplayNameChange,
                            placeholder = stringResource(R.string.edit_profile_display_name_placeholder),
                            maxLength = DISPLAY_NAME_MAX,
                        )

                        UsernameInput(
                            value = username,
                            onValueChange = viewModel::onUsernameChange,
                            isValid = viewModel.isUsernameValid() && (usernameAvailable == true || username == profile?.username?.trim()),
                            isChecking = isCheckingUsername,
                        )

                        BioInput(
                            value = bio,
                            onValueChange = viewModel::onBioChange,
                            maxLength = 150,
                            placeholder = stringResource(R.string.edit_profile_bio_placeholder),
                        )

                        AestheticStylesSection(
                            selectedIds = selectedTagIds,
                            tagCount = tags.size,
                            onOpenPicker = { showStyleSheet = true },
                            onRemove = viewModel::removeTag,
                            resolveLabel = viewModel::resolveTagLabel,
                        )

                        ProfileSetupSizingSection(
                            referenceSize = referenceSize,
                            onReferenceSizeChange = viewModel::onReferenceSizeChange,
                            measurementUnit = measurementUnit,
                            onMeasurementUnitChange = viewModel::onMeasurementUnitChange,
                            hem = measurementHem,
                            onHemChange = viewModel::onMeasurementHemChange,
                            chest = measurementChest,
                            onChestChange = viewModel::onMeasurementChestChange,
                            length = measurementLength,
                            onLengthChange = viewModel::onMeasurementLengthChange,
                            shoulders = measurementShoulders,
                            onShouldersChange = viewModel::onMeasurementShouldersChange,
                            sleeve = measurementSleeve,
                            onSleeveChange = viewModel::onMeasurementSleeveChange,
                            supportedMeasurementUnits = listOf("cm", "in", "st"),
                            compactDensity = true,
                        )
                    }
                }

                FashPrimaryButton(
                    onClick = {
                        if (viewModel.canSave() && !isSubmitting) {
                            viewModel.save(onSaved)
                        }
                    },
                    enabled = viewModel.canSave() && !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = FashColors.Primary.fashReadableOn(),
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

    if (showStyleSheet) {
        AestheticTagsPickerSheet(
            tags = tags,
            selectedIds = selectedTagIds,
            onToggle = viewModel::toggleTag,
            onClearAll = viewModel::clearAllStyles,
            onDismiss = { showStyleSheet = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AestheticStylesSection(
    selectedIds: Set<String>,
    tagCount: Int,
    onOpenPicker: () -> Unit,
    onRemove: (String) -> Unit,
    resolveLabel: (String) -> String,
) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Text(
            text = stringResource(R.string.edit_profile_style_label),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.edit_profile_style_section_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant.copy(alpha = 0.85f),
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.edit_profile_style_selected_count, selectedIds.size),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurface,
            )
            TextButton(
                onClick = onOpenPicker,
                enabled = tagCount > 0,
            ) {
                Text(
                    text = stringResource(R.string.edit_profile_style_edit_button),
                    color = FashColors.Primary,
                )
            }
        }

        if (selectedIds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selectedIds.sorted().forEach { id ->
                    Surface(
                        shape = ChipCorner,
                        color = FashColors.Primary.copy(alpha = 0.12f),
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = resolveLabel(id),
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurface,
                                maxLines = 1,
                            )
                            IconButton(
                                onClick = { onRemove(id) },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = scheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.edit_profile_style_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AestheticTagsPickerSheet(
    tags: List<CommonAestheticTagDto>,
    selectedIds: Set<String>,
    onToggle: (CommonAestheticTagDto) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(tags, searchQuery) {
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        if (q.isEmpty()) {
            tags
        } else {
            tags.filter { t ->
                t.displayName.lowercase(Locale.getDefault()).contains(q) ||
                    t.name.lowercase(Locale.getDefault()).contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = scheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onClearAll,
                    enabled = selectedIds.isNotEmpty(),
                ) {
                    Text(
                        text = stringResource(R.string.edit_profile_style_clear_all),
                        color = if (selectedIds.isNotEmpty()) FashColors.Primary else scheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
                Text(
                    text = stringResource(R.string.edit_profile_style_label),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.edit_profile_style_done),
                        color = FashColors.Primary,
                    )
                }
            }
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.65f))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 12.dp),
                placeholder = {
                    Text(stringResource(R.string.edit_profile_style_search_placeholder))
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = scheme.surfaceContainerHighest,
                    unfocusedContainerColor = scheme.surfaceContainerHighest,
                ),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .padding(horizontal = FashTheme.spacing.editorialStart),
            ) {
                items(filtered, key = { it.id }) { tag ->
                    val selected = tag.id in selectedIds
                    Surface(
                        shape = ChipCorner,
                        color = if (selected) FashColors.Primary else scheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(tag) },
                    ) {
                        Text(
                            text = tag.displayName.ifBlank { tag.name },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) FashColors.Primary.fashReadableOn() else scheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisplayNameInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLength: Int,
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
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.edit_profile_display_name_counter, value.length, maxLength),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(LabelToFieldGap))
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
        Spacer(modifier = Modifier.height(LabelToFieldGap))
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
                        color = FashColors.Primary,
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
        Spacer(modifier = Modifier.height(LabelToFieldGap))
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
