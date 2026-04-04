@file:OptIn(ExperimentalLayoutApi::class)

package com.pc.fash_android_mobile.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.common.CategoryTreeNode
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlinx.coroutines.delay

@Composable
fun CreateListingPostStep1(
    viewModel: PostViewModel,
    onCloseRequest: () -> Unit,
) {
    val draft by viewModel.draft.collectAsState()
    val tree by viewModel.categoryTree.collectAsState()
    val catalogLoading by viewModel.catalogLoading.collectAsState()
    val canNext = draft.canProceedFromStep(1)

    LaunchedEffect(Unit) {
        viewModel.loadCatalogIfNeeded()
    }

    var categoryQuery by remember { mutableStateOf("") }
    val categoryMatches = remember(categoryQuery, tree) {
        buildCategorySearchMatches(tree, categoryQuery)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PostListingColors.stepCanvas())
            .navigationBarsPadding(),
    ) {
        CreateListingFlowHeader(
            step = 1,
            totalSteps = TotalPostSteps,
            onBackClick = null,
            onCloseClick = onCloseRequest,
            primaryLabelRes = R.string.create_listing_next,
            onPrimaryClick = { viewModel.nextStep() },
            primaryEnabled = canNext,
            nextDisabledReasonRes = draft.nextStepBlockedReasonRes(1),
        )
        PostStepScrollWithBottomNotice(
            modifier = Modifier.weight(1f),
            horizontalPadding = FashTheme.spacing.editorialStart,
            bottomNotice = stringResource(R.string.post_hint_category_step),
            scrollState = scrollState,
        ) {
            Text(
                text = stringResource(R.string.post_step_category_only),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.post_step1_category_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(14.dp))
            PostStep1SectionCard {
                Text(
                    text = stringResource(R.string.post_step1_category_section_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.post_step1_category_section_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!catalogLoading || tree.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PostListingSearchField(
                        value = categoryQuery,
                        onValueChange = { categoryQuery = it },
                        label = { Text(stringResource(R.string.post_search_category)) },
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (catalogLoading && tree.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = FashColors.Primary)
                    }
                } else if (categoryQuery.isBlank()) {
                    CategoryTreeSection(
                        roots = tree,
                        selectedId = draft.categoryId,
                        onSelectLeaf = { leaf ->
                            viewModel.updateDraft {
                                withLeafCategory(tree, leaf)
                            }
                        },
                    )
                } else if (categoryMatches.isEmpty()) {
                    Text(
                        text = stringResource(R.string.post_category_no_matches),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categoryMatches.forEach { match ->
                            val subtitle =
                                match.pathSegments.dropLast(1).joinToString(" · ").takeIf { it.isNotBlank() }
                            PostSelectableListRow(
                                text = match.leaf.name,
                                subtitle = subtitle,
                                selected = draft.categoryId == match.leaf.id,
                                onClick = {
                                    viewModel.updateDraft {
                                        withLeafCategory(tree, match.leaf)
                                    }
                                    categoryQuery = ""
                                },
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PostStep1SectionCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
fun CreateListingPostStep2(
    viewModel: PostViewModel,
    onCloseRequest: () -> Unit,
) {
    val draft by viewModel.draft.collectAsState()
    val tags by viewModel.aestheticTags.collectAsState()
    val canNext = draft.canProceedFromStep(2)

    LaunchedEffect(Unit) {
        viewModel.loadCatalogIfNeeded()
    }

    var tagQuery by remember { mutableStateOf("") }
    val filteredTags = remember(tagQuery, tags) {
        tags.filter { it.matchesTagQuery(tagQuery) }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PostListingColors.stepCanvas())
            .navigationBarsPadding(),
    ) {
        CreateListingFlowHeader(
            step = 2,
            totalSteps = TotalPostSteps,
            onBackClick = { viewModel.prevStep() },
            onCloseClick = onCloseRequest,
            primaryLabelRes = R.string.create_listing_next,
            onPrimaryClick = { viewModel.nextStep() },
            primaryEnabled = canNext,
            nextDisabledReasonRes = draft.nextStepBlockedReasonRes(2),
        )
        PostStepScrollWithBottomNotice(
            modifier = Modifier.weight(1f),
            horizontalPadding = FashTheme.spacing.editorialStart,
            bottomNotice = stringResource(R.string.post_hint_style_tags_step),
            scrollState = scrollState,
        ) {
            Text(
                text = stringResource(R.string.post_step_style_only),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.post_step2_style_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(14.dp))
            PostListingSearchField(
                value = tagQuery,
                onValueChange = { tagQuery = it },
                label = { Text(stringResource(R.string.post_search_style_tags)) },
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (filteredTags.isEmpty()) {
                Text(
                    text = stringResource(R.string.post_style_tags_no_matches),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    filteredTags.forEach { tag ->
                        val selected = draft.selectedAestheticTagIds.contains(tag.id)
                        PostSelectablePill(
                            text = tag.displayName.ifBlank { tag.name },
                            selected = selected,
                            onClick = { viewModel.updateDraft { toggleAestheticTag(tag.id) } },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class CategorySearchMatch(
    val leaf: CategoryTreeNode,
    val pathSegments: List<String>,
)

/** Leaf categories whose full path (all segment names) matches the trimmed query (case-insensitive). */
private fun buildCategorySearchMatches(
    roots: List<CategoryTreeNode>,
    query: String,
): List<CategorySearchMatch> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()
    val ql = q.lowercase()
    val out = mutableListOf<CategorySearchMatch>()
    fun walk(node: CategoryTreeNode, ancestors: List<String>) {
        val segments = ancestors + node.name
        if (node.children.isEmpty()) {
            val searchable = segments.joinToString(" ").lowercase()
            if (searchable.contains(ql)) {
                out.add(CategorySearchMatch(leaf = node, pathSegments = segments))
            }
        } else {
            node.children.forEach { walk(it, segments) }
        }
    }
    roots.forEach { walk(it, emptyList()) }
    return out
        .distinctBy { it.leaf.id }
        .sortedWith(
            compareBy<CategorySearchMatch> { it.pathSegments.size }
                .thenBy { it.pathSegments.joinToString(" · ") },
        )
}

@Composable
private fun CategoryTreeSection(
    roots: List<CategoryTreeNode>,
    selectedId: String,
    onSelectLeaf: (CategoryTreeNode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        roots.forEach { root ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin),
                color = PostListingColors.fieldSurface(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CategoryNodeBlock(
                        node = root,
                        selectedId = selectedId,
                        onSelectLeaf = onSelectLeaf,
                        depth = 0,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryNodeBlock(
    node: CategoryTreeNode,
    selectedId: String,
    onSelectLeaf: (CategoryTreeNode) -> Unit,
    depth: Int = 0,
) {
    if (node.children.isEmpty()) {
        PostSelectablePill(
            text = node.name,
            selected = selectedId == node.id,
            onClick = { onSelectLeaf(node) },
        )
    } else {
        val indent = (depth * 10).coerceAtMost(28).dp
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = indent),
        ) {
            Text(
                text = node.name,
                style = if (depth == 0) {
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                } else {
                    MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                },
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                node.children.forEach { child ->
                    CategoryNodeBlock(child, selectedId, onSelectLeaf, depth + 1)
                }
            }
        }
    }
}

private val conditionValues = listOf("New", "Like new", "Good", "Fair", "Worn")

@Composable
fun CreateListingPostStep3(viewModel: PostViewModel, onCloseRequest: () -> Unit) {
    val draft by viewModel.draft.collectAsState()
    val featured by viewModel.brandsFeatured.collectAsState()
    val search by viewModel.brandsSearch.collectAsState()
    var query by remember { mutableStateOf("") }
    val canNext = draft.canProceedFromStep(3)

    LaunchedEffect(query) {
        delay(320)
        viewModel.searchBrands(query)
    }

    val listState = rememberLazyListState()
    val list = if (query.isBlank()) featured else search

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PostListingColors.stepCanvas())
            .navigationBarsPadding(),
    ) {
        CreateListingFlowHeader(
            step = 3,
            totalSteps = TotalPostSteps,
            onBackClick = { viewModel.prevStep() },
            onCloseClick = onCloseRequest,
            primaryLabelRes = R.string.create_listing_next,
            onPrimaryClick = { viewModel.nextStep() },
            primaryEnabled = canNext,
            nextDisabledReasonRes = draft.nextStepBlockedReasonRes(3),
        )
        PostStepLazyListWithBottomNotice(
            modifier = Modifier.weight(1f),
            horizontalPadding = FashTheme.spacing.editorialStart,
            bottomNotice = stringResource(R.string.post_hint_brand),
            listState = listState,
            header = {
                Text(
                    text = stringResource(R.string.post_step_brand),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.height(8.dp))
                PostListingSearchField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.post_search_brand)) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (draft.brandName.isNotBlank()) {
                    Text(
                        text = "${stringResource(R.string.create_listing_brand_label)}: ${draft.brandName}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            viewModel.updateDraft { copy(brandId = null, brandName = "") }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = FashColors.Primary),
                    ) {
                        Text(stringResource(R.string.post_clear_brand))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            },
            lazyContent = {
                if (query.isBlank() && featured.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.post_brand_featured),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                items(list, key = { it.id }) { brand ->
                    PostSelectableListRow(
                        text = brand.name,
                        selected = draft.brandId == brand.id,
                        onClick = {
                            viewModel.updateDraft {
                                copy(brandId = brand.id, brandName = brand.name)
                            }
                        },
                    )
                }
            },
        )
    }
}

@Composable
fun CreateListingPostStep4(viewModel: PostViewModel, onCloseRequest: () -> Unit) {
    val draft by viewModel.draft.collectAsState()
    val countries by viewModel.countries.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, countries) {
        countries.filter { it.matchesQuery(query) }
    }
    val canNext = draft.canProceedFromStep(4)
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PostListingColors.stepCanvas())
            .navigationBarsPadding(),
    ) {
        CreateListingFlowHeader(
            step = 4,
            totalSteps = TotalPostSteps,
            onBackClick = { viewModel.prevStep() },
            onCloseClick = onCloseRequest,
            primaryLabelRes = R.string.create_listing_next,
            onPrimaryClick = { viewModel.nextStep() },
            primaryEnabled = canNext,
            nextDisabledReasonRes = draft.nextStepBlockedReasonRes(4),
        )
        PostStepLazyListWithBottomNotice(
            modifier = Modifier.weight(1f),
            horizontalPadding = FashTheme.spacing.editorialStart,
            bottomNotice = stringResource(R.string.post_hint_country),
            listState = listState,
            header = {
                Text(
                    text = stringResource(R.string.post_step_country),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.height(8.dp))
                PostListingSearchField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.post_search_country)) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (draft.countryName.isNotBlank()) {
                    Text(
                        text = "${draft.countryName} (${draft.countryIso2})",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            },
            lazyContent = {
                items(filtered, key = { it.id }) { c ->
                    PostSelectableListRow(
                        text = c.name,
                        selected = draft.countryId == c.id,
                        onClick = {
                            viewModel.updateDraft {
                                copy(
                                    countryId = c.id,
                                    countryIso2 = c.iso2,
                                    countryName = c.name,
                                )
                            }
                        },
                        leadingEmoji = c.emoji,
                    )
                }
            },
        )
    }
}

@Composable
fun CreateListingPostStep5(viewModel: PostViewModel, onCloseRequest: () -> Unit) {
    val draft by viewModel.draft.collectAsState()
    val canNext = draft.canProceedFromStep(5)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PostListingColors.stepCanvas())
            .navigationBarsPadding(),
    ) {
        CreateListingFlowHeader(
            step = 5,
            totalSteps = TotalPostSteps,
            onBackClick = { viewModel.prevStep() },
            onCloseClick = onCloseRequest,
            primaryLabelRes = R.string.create_listing_next,
            onPrimaryClick = { viewModel.nextStep() },
            primaryEnabled = canNext,
            nextDisabledReasonRes = draft.nextStepBlockedReasonRes(5),
        )
        PostStepScrollWithBottomNotice(
            modifier = Modifier.weight(1f),
            horizontalPadding = FashTheme.spacing.editorialStart,
            bottomNotice = stringResource(R.string.post_hint_listing_details_step),
            scrollState = scrollState,
        ) {
            Text(
                text = stringResource(R.string.post_step_listing_details),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.post_listing_details_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.post_step_condition_short),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                conditionValues.forEach { cond ->
                    PostSelectablePill(
                        text = cond,
                        selected = draft.condition == cond,
                        onClick = { viewModel.updateDraft { copy(condition = cond) } },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            PostListingOutlinedTextField(
                value = draft.title,
                onValueChange = { viewModel.updateDraft { copy(title = it.take(MaxListingTitleLength)) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.create_listing_title_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Text(
                text = "${draft.title.length}/$MaxListingTitleLength",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            PostListingOutlinedTextField(
                value = draft.description,
                onValueChange = { viewModel.updateDraft { copy(description = it.take(MaxListingDescriptionLength)) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                label = { Text(stringResource(R.string.post_description_label)) },
                singleLine = false,
                minLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Text(
                text = "${draft.description.length}/$MaxListingDescriptionLength",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
