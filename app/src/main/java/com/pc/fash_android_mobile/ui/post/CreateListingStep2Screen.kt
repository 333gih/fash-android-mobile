package com.pc.fash_android_mobile.ui.post

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.Category
import com.pc.fash_android_mobile.data.user.AestheticTag
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

private val TITLE_MAX_LENGTH = 60
private val PRICE_MIN = 1_000L
private val PRICE_MAX = 100_000_000L
private val MAX_STYLE_TAGS = 5

private val SIZES = listOf(
    "XXS", "XS", "S", "M", "L", "XL", "XXL",
    "Free size", "34", "36", "38", "40", "42",
)

private val BRANDS = listOf(
    "Zara", "H&M", "Uniqlo", "Mango", "Bershka", "Pull&Bear",
    "Nike", "Adidas", "Gucci", "Louis Vuitton", "Chanel",
    "Khác",
)

private val CONDITION_VALUES = listOf(
    "new" to R.string.condition_new,
    "like_new" to R.string.condition_like_new,
    "good" to R.string.condition_good,
    "fair" to R.string.condition_fair,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateListingStep2Screen(
    modifier: Modifier = Modifier,
    viewModel: PostViewModel,
    onCloseRequest: () -> Unit,
) {
    val draft by viewModel.draft.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val aestheticTags by viewModel.aestheticTags.collectAsState()

    var showCategorySheet by rememberSaveable { mutableStateOf(false) }
    var showSizeSheet by rememberSaveable { mutableStateOf(false) }
    var showBrandSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadStep2Data()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        CreateListingFlowHeader(
            step = 2,
            totalSteps = 3,
            onBackClick = { viewModel.prevStep() },
            onCloseClick = onCloseRequest,
            primaryLabelRes = R.string.create_listing_next,
            onPrimaryClick = {
                if (draft.canProceedFromStep2()) {
                    viewModel.nextStep()
                }
            },
            primaryEnabled = draft.canProceedFromStep2(),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FashTheme.spacing.editorialStart),
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            TitleField(
                value = draft.title,
                onValueChange = { viewModel.updateDraft { withTitle(it.take(TITLE_MAX_LENGTH)) } },
            )

            Spacer(modifier = Modifier.height(16.dp))

            PriceField(
                value = draft.priceVnd,
                onValueChange = { viewModel.updateDraft { withPriceVnd(it) } },
            )

            Spacer(modifier = Modifier.height(16.dp))

            ConditionChips(
                selected = draft.condition,
                onSelect = { viewModel.updateDraft { withCondition(it) } },
            )

            Spacer(modifier = Modifier.height(16.dp))

            CategoryField(
                categories = categories,
                selectedId = draft.categoryId,
                selectedName = categories.find { it.id == draft.categoryId }?.name ?: "",
                onSelect = { cat -> viewModel.updateDraft { withCategoryId(cat.id) } },
                onOpenSheet = { showCategorySheet = true },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SelectField(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.create_listing_size_label),
                    value = draft.size,
                    placeholder = stringResource(R.string.create_listing_select),
                    onOpenSheet = { showSizeSheet = true },
                )
                SelectField(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.create_listing_brand_label),
                    value = draft.brand,
                    placeholder = stringResource(R.string.create_listing_select),
                    onOpenSheet = { showBrandSheet = true },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            StyleChips(
                tags = aestheticTags,
                selectedIds = draft.aestheticTags.toSet(),
                onToggle = { tag ->
                    val current = draft.aestheticTags.toMutableList()
                    if (tag.id in current) {
                        current.remove(tag.id)
                    } else if (current.size < MAX_STYLE_TAGS) {
                        current.add(tag.id)
                    }
                    viewModel.updateDraft { withAestheticTags(current) }
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Step2IncompleteHint(keys = draft.step2MissingRequirementKeys())

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCategorySheet) {
        CategoryBottomSheet(
            categories = categories,
            onSelect = { cat ->
                viewModel.updateDraft { withCategoryId(cat.id) }
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false },
        )
    }
    if (showSizeSheet) {
        OptionBottomSheet(
            title = stringResource(R.string.create_listing_size_label),
            options = SIZES,
            onSelect = {
                viewModel.updateDraft { withSize(it) }
                showSizeSheet = false
            },
            onDismiss = { showSizeSheet = false },
        )
    }
    if (showBrandSheet) {
        OptionBottomSheet(
            title = stringResource(R.string.create_listing_brand_label),
            options = BRANDS,
            onSelect = {
                viewModel.updateDraft { withBrand(if (it == "Khác") "" else it) }
                showBrandSheet = false
            },
            onDismiss = { showBrandSheet = false },
        )
    }
}

@Composable
private fun TitleField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.create_listing_title_label),
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    stringResource(R.string.create_listing_title_placeholder),
                    color = scheme.onSurfaceVariant,
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = scheme.outline,
                unfocusedBorderColor = scheme.outlineVariant,
                focusedContainerColor = scheme.surfaceContainerLow,
                unfocusedContainerColor = scheme.surfaceContainerLow,
                cursorColor = FashColors.Primary,
                focusedTextColor = scheme.onSurface,
                unfocusedTextColor = scheme.onSurface,
            ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${value.length}/$TITLE_MAX_LENGTH",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@Composable
private fun PriceField(
    value: Long,
    onValueChange: (Long) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var text by remember { mutableStateOf(if (value > 0) value.toString() else "") }
    LaunchedEffect(value) {
        text = if (value > 0) value.toString() else ""
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.create_listing_price_label),
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { s ->
                val digits = s.filter { it.isDigit() }
                text = digits
                val parsed = digits.toLongOrNull() ?: 0L
                onValueChange(parsed)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    stringResource(R.string.create_listing_price_placeholder),
                    color = scheme.onSurfaceVariant,
                )
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = {
                Text(
                    text = "₫",
                    style = MaterialTheme.typography.bodyLarge,
                    color = FashColors.Primary,
                    modifier = Modifier.padding(end = 16.dp),
                )
            },
            shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = scheme.outline,
                unfocusedBorderColor = scheme.outlineVariant,
                focusedContainerColor = scheme.surfaceContainerLow,
                unfocusedContainerColor = scheme.surfaceContainerLow,
                cursorColor = FashColors.Primary,
                focusedTextColor = scheme.onSurface,
                unfocusedTextColor = scheme.onSurface,
            ),
        )
    }
}

@Composable
private fun ConditionChips(
    selected: String,
    onSelect: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = stringResource(R.string.create_listing_condition_label),
        style = MaterialTheme.typography.labelLarge,
        color = scheme.onSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for ((value, resId) in CONDITION_VALUES) {
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(FashTheme.spacing.radiusPill))
                    .background(
                        if (isSelected) FashColors.Primary
                        else scheme.surfaceContainerHigh
                    )
                    .clickable { onSelect(value) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = stringResource(resId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) FashColors.OnPrimary else scheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun CategoryField(
    categories: List<Category>,
    selectedId: String,
    selectedName: String,
    onSelect: (Category) -> Unit,
    onOpenSheet: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = stringResource(R.string.create_listing_category_label),
        style = MaterialTheme.typography.labelLarge,
        color = scheme.onSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FashTheme.spacing.radiusSoftMin))
            .border(
                width = 1.dp,
                color = scheme.outlineVariant,
                shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin),
            )
            .background(scheme.surfaceContainerLow)
            .clickable(onClick = onOpenSheet)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = selectedName.ifBlank { stringResource(R.string.create_listing_select_category) },
                style = MaterialTheme.typography.bodyLarge,
                color = if (selectedName.isBlank()) scheme.onSurfaceVariant else scheme.onSurface,
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryBottomSheet(
    categories: List<Category>,
    onSelect: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FashTheme.spacing.editorialStart)
                .padding(bottom = 24.dp),
        ) {
            categories.forEach { cat ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(cat) }
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectField(
    modifier: Modifier,
    label: String,
    value: String,
    placeholder: String,
    onOpenSheet: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(FashTheme.spacing.radiusSoftMin))
                .border(
                    width = 1.dp,
                    color = scheme.outlineVariant,
                    shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin),
                )
                .background(scheme.surfaceContainerLow)
                .clickable(onClick = onOpenSheet)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = value.ifBlank { placeholder },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value.isBlank()) scheme.onSurfaceVariant else scheme.onSurface,
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionBottomSheet(
    title: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FashTheme.spacing.editorialStart)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            options.forEach { option ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option) }
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleChips(
    tags: List<AestheticTag>,
    selectedIds: Set<String>,
    onToggle: (AestheticTag) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = stringResource(R.string.create_listing_style_label),
        style = MaterialTheme.typography.labelLarge,
        color = scheme.onSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            val isSelected = tag.id in selectedIds
            val atLimit = selectedIds.size >= MAX_STYLE_TAGS && !isSelected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(FashTheme.spacing.radiusPill))
                    .border(
                        width = 1.dp,
                        color = if (isSelected) FashColors.Primary else scheme.outlineVariant,
                        shape = RoundedCornerShape(FashTheme.spacing.radiusPill),
                    )
                    .background(if (isSelected) FashColors.Primary.copy(alpha = 0.15f) else scheme.surfaceContainerLow)
                    .clickable(enabled = !atLimit) { onToggle(tag) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = tag.displayName.ifBlank { tag.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) FashColors.Primary else scheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun Step2IncompleteHint(keys: List<String>) {
    if (keys.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(bottom = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.create_listing_step2_incomplete_header),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.error,
        )
        keys.forEach { key ->
            val msg = when (key) {
                "title" -> stringResource(R.string.create_listing_step2_need_title)
                "price" -> stringResource(R.string.create_listing_step2_need_price)
                "condition" -> stringResource(R.string.create_listing_step2_need_condition)
                "category" -> stringResource(R.string.create_listing_step2_need_category)
                else -> ""
            }
            if (msg.isNotBlank()) {
                Text(
                    text = "• $msg",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}
