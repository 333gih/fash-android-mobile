package com.pc.fash_android_mobile.ui.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import androidx.compose.foundation.text.KeyboardOptions

private val CONDITION_PAIRS = listOf(
    "new" to R.string.condition_new,
    "like_new" to R.string.condition_like_new,
    "good" to R.string.condition_good,
    "fair" to R.string.condition_fair,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditListingScreen(
    modifier: Modifier = Modifier,
    listingId: String,
    viewModel: EditListingViewModel,
    onBack: () -> Unit,
) {
    val detail by viewModel.detail.collectAsState()
    val form by viewModel.form.collectAsState()
    val catalogTags by viewModel.catalogTags.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val baselineTagIds by viewModel.baselineTagIds.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(listingId) {
        viewModel.load(listingId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.edit_listing_screen_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.orders_back),
                            tint = FashColors.Primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        when {
            isLoading && detail == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = FashColors.Primary)
                }
            }
            loadError != null && detail == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = loadError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.load(listingId) }) {
                        Text(stringResource(R.string.feed_retry))
                    }
                }
            }
            detail != null -> {
                val d = detail!!
                val editable = d.status.equals("active", ignoreCase = true)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = FashTheme.spacing.editorialStart)
                        .padding(bottom = 32.dp),
                ) {
                    if (!editable) {
                        Text(
                            text = stringResource(R.string.edit_listing_readonly_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    FashColors.Primary.copy(alpha = 0.12f),
                                    RoundedCornerShape(12.dp),
                                )
                                .padding(16.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    ListingImageStrip(detail = d)

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.edit_listing_category_locked),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = d.category ?: stringResource(R.string.create_listing_select),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = form.title,
                        onValueChange = {
                            viewModel.updateForm { copy(title = it.take(60)) }
                        },
                        label = { Text(stringResource(R.string.create_listing_title_label)) },
                        enabled = editable && !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FashColors.Primary,
                            focusedLabelColor = FashColors.Primary,
                            cursorColor = FashColors.Primary,
                        ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = form.priceText,
                        onValueChange = { v ->
                            if (v.all { it.isDigit() } && v.length <= 12) {
                                viewModel.updateForm { copy(priceText = v) }
                            }
                        },
                        label = { Text(stringResource(R.string.create_listing_price_label)) },
                        enabled = editable && !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FashColors.Primary,
                            focusedLabelColor = FashColors.Primary,
                            cursorColor = FashColors.Primary,
                        ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.create_listing_condition_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CONDITION_PAIRS.forEach { (value, labelRes) ->
                            val selected = form.condition == value
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (editable && !isSaving) {
                                        viewModel.updateForm { copy(condition = value) }
                                    }
                                },
                                enabled = editable && !isSaving,
                                label = { Text(stringResource(labelRes)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FashColors.Primary.copy(alpha = 0.2f),
                                    selectedLabelColor = FashColors.Primary,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = form.size,
                            onValueChange = {
                                viewModel.updateForm { copy(size = it.take(20)) }
                            },
                            label = { Text(stringResource(R.string.create_listing_size_label)) },
                            enabled = editable && !isSaving,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FashColors.Primary,
                                focusedLabelColor = FashColors.Primary,
                                cursorColor = FashColors.Primary,
                            ),
                        )
                        OutlinedTextField(
                            value = form.brand,
                            onValueChange = {
                                viewModel.updateForm { copy(brand = it.take(50)) }
                            },
                            label = { Text(stringResource(R.string.create_listing_brand_label)) },
                            enabled = editable && !isSaving,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FashColors.Primary,
                                focusedLabelColor = FashColors.Primary,
                                cursorColor = FashColors.Primary,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = form.description,
                        onValueChange = {
                            viewModel.updateForm { copy(description = it.take(500)) }
                        },
                        label = { Text(stringResource(R.string.edit_listing_description_label)) },
                        enabled = editable && !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FashColors.Primary,
                            focusedLabelColor = FashColors.Primary,
                            cursorColor = FashColors.Primary,
                        ),
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.edit_listing_style_tags),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.edit_listing_tags_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        catalogTags.forEach { tag ->
                            val selected = tag.id in form.selectedTagIds
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (editable && !isSaving) viewModel.toggleTag(tag.id)
                                },
                                enabled = editable && !isSaving,
                                label = { Text(tag.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FashColors.Primary.copy(alpha = 0.2f),
                                    selectedLabelColor = FashColors.Primary,
                                ),
                            )
                        }
                    }
                    if (form.selectedTagIds != baselineTagIds && form.selectedTagIds.isEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.edit_listing_tags_cleared_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { viewModel.save() },
                        enabled = editable && viewModel.canSave() && !isSaving && !isDeleting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = FashColors.OnPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.edit_listing_save))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        enabled = !isSaving && !isDeleting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.edit_listing_delete))
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text(stringResource(R.string.edit_listing_delete_title)) },
            text = { Text(stringResource(R.string.edit_listing_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    },
                    enabled = !isDeleting,
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            stringResource(R.string.edit_listing_delete_confirm),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) {
                    Text(stringResource(R.string.create_listing_cancel))
                }
            },
        )
    }
}

@Composable
private fun ListingImageStrip(detail: ListingDetail) {
    val urls = detail.imageUrls.mapNotNull { u ->
        u.takeIf { it.isNotBlank() }?.let { resolveEditImageUrl(it) }
    }
    if (urls.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        urls.forEach { url ->
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                FashAsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

private fun resolveEditImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val base = AppEnvironment.apiBaseUrl.trimEnd('/')
    return "$base/${path.trimStart('/')}"
}
