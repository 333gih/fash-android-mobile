package com.pc.fash_android_mobile.ui.entitlements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.entitlements.FeatureUsageSummary
import com.pc.fash_android_mobile.data.entitlements.UserEntitlementRepository
import com.pc.fash_android_mobile.data.entitlements.UserEntitlementSummary
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.FashFilledTextField
import com.pc.fash_android_mobile.ui.components.FashPrimaryButton
import com.pc.fash_android_mobile.ui.components.FashSecondaryButton
import com.pc.fash_android_mobile.ui.components.FashSnackbarHost
import com.pc.fash_android_mobile.ui.feed.resolveListingImageUrl
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val FEATURE_GROUP_ORDER = listOf("verification", "visibility", "social_promo")

private data class ToolFeatureEntry(
    val key: String,
    val feature: FeatureUsageSummary,
)

private fun groupedToolFeatures(summary: UserEntitlementSummary?): List<Pair<String, List<ToolFeatureEntry>>> {
    val feats = summary?.features.orEmpty()
    if (feats.isEmpty()) return emptyList()
    val grouped = feats.entries
        .map { (key, feature) -> ToolFeatureEntry(key, feature) }
        .groupBy { it.feature.featureGroup.ifBlank { "other" } }
    val orderedGroups = FEATURE_GROUP_ORDER.filter { grouped.containsKey(it) } +
        grouped.keys.filter { it !in FEATURE_GROUP_ORDER }.sorted()
    return orderedGroups.map { group -> group to grouped[group].orEmpty() }
}

private fun featureGroupLabel(group: String): String = when (group) {
    "verification" -> "Xác minh"
    "visibility" -> "Hiển thị"
    "social_promo" -> "Quảng bá"
    else -> group.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

@Composable
private fun featureIcon(key: String, kind: String): ImageVector = when {
    kind == "boost" || key == "explore_boost" -> Icons.Outlined.AutoAwesome
    key.contains("social") -> Icons.Outlined.Share
    key.contains("fanpage") -> Icons.Outlined.Campaign
    key.contains("authenticity") || key.contains("verify") || key == "seller_real_badge" -> Icons.Outlined.VerifiedUser
    else -> Icons.Outlined.WorkspacePremium
}

@Composable
private fun featureTitle(key: String, feature: FeatureUsageSummary): String {
    if (feature.name.isNotBlank()) return feature.name
    return when (key) {
        "authenticity_verify" -> stringResource(R.string.seller_packages_feature_authenticity)
        "explore_boost" -> stringResource(R.string.seller_packages_feature_explore_boost)
        "fanpage_spotlight" -> stringResource(R.string.seller_packages_feature_fanpage)
        "social_tiktok_instagram" -> stringResource(R.string.seller_packages_feature_social)
        "seller_real_badge" -> stringResource(R.string.seller_packages_feature_real_badge)
        else -> key.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}

@Composable
private fun featureDescription(key: String, feature: FeatureUsageSummary): String {
    if (feature.description.isNotBlank()) return feature.description
    return when (key) {
        "authenticity_verify" -> stringResource(R.string.seller_packages_tools_desc_verify)
        "explore_boost" -> stringResource(R.string.seller_packages_tools_desc_boost)
        "fanpage_spotlight" -> stringResource(R.string.seller_packages_tools_desc_fanpage)
        "social_tiktok_instagram" -> stringResource(R.string.seller_packages_tools_desc_social)
        else -> ""
    }
}

@Composable
private fun featureCta(kind: String): String = when (kind) {
    "boost" -> stringResource(R.string.seller_packages_tools_boost)
    else -> stringResource(R.string.seller_packages_tools_submit)
}

private fun featureSuccessMessageRes(kind: String): Int = when (kind) {
    "boost" -> R.string.seller_packages_tools_success_boost
    else -> R.string.seller_packages_tools_success_request
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerPackageToolsScreen(
    repository: UserEntitlementRepository,
    listingRepository: ListingRepository,
    onBack: () -> Unit,
    onEntitlementsChanged: () -> Unit,
    onUpgrade: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var summary by remember { mutableStateOf(repository.peekCached()) }
    var listings by remember { mutableStateOf<List<ListingFeedItem>>(emptyList()) }
    var listingId by remember { mutableStateOf("") }
    var selectedListingTitle by remember { mutableStateOf("") }
    var showListingPicker by remember { mutableStateOf(false) }
    var showAdvancedListingId by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(summary == null) }
    var listingsLoading by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf<String?>(null) }

    fun reloadEntitlements() {
        scope.launch {
            loading = summary == null
            val result = withContext(Dispatchers.IO) { repository.fetchEntitlements() }
            loading = false
            result.onSuccess { summary = it }
        }
    }

    fun loadListings() {
        scope.launch {
            listingsLoading = true
            val result = withContext(Dispatchers.IO) {
                val active = listingRepository.getMyListings(status = "active", limit = 40, offset = 0)
                if (active.isSuccess && active.getOrNull().orEmpty().isNotEmpty()) {
                    active
                } else {
                    listingRepository.getMyListings(status = null, limit = 40, offset = 0)
                }
            }
            listingsLoading = false
            result.onSuccess { items ->
                listings = items
                if (listingId.isBlank() && items.isNotEmpty()) {
                    listingId = items.first().id
                    selectedListingTitle = items.first().title
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        reloadEntitlements()
        loadListings()
    }

    val selectedId = listingId.trim()
    val hasListing = selectedId.isNotEmpty()

    fun runTool(
        featureKey: String,
        executionKind: String,
        needsListing: Boolean,
        needsCaption: Boolean,
        action: () -> Result<Unit>,
    ) {
        if (submitting != null) return
        if (needsListing && !hasListing) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.seller_packages_tools_need_listing)) }
            return
        }
        if (needsCaption && caption.trim().isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.seller_packages_tools_need_caption)) }
            return
        }
        submitting = featureKey
        scope.launch {
            val result = withContext(Dispatchers.IO) { action() }
            submitting = null
            result.fold(
                onSuccess = {
                    onEntitlementsChanged()
                    reloadEntitlements()
                    val msgRes = when {
                        executionKind == "boost" -> R.string.seller_packages_tools_success_boost_affinity
                        else -> featureSuccessMessageRes(executionKind)
                    }
                    snackbarHostState.showSnackbar(context.getString(msgRes))
                },
                onFailure = { err ->
                    snackbarHostState.showSnackbar(
                        err.message ?: context.getString(R.string.seller_packages_load_error),
                    )
                },
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { FashSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.seller_packages_tools_title),
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
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = scheme.surfaceContainerLowest,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = FashTheme.spacing.editorialStart,
                        end = FashTheme.spacing.editorialEnd,
                        top = 12.dp,
                        bottom = 32.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.seller_packages_tools_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                PackageStatusCard(summary = summary, loading = loading, onUpgrade = { onUpgrade(null) })
                val anyNeedsListing = summary?.features?.values?.any { it.requiresListing } == true
                if (anyNeedsListing) {
                    ListingPickerSection(
                        selectedTitle = selectedListingTitle.ifBlank { selectedId.take(8) },
                        hasSelection = hasListing,
                        showAdvanced = showAdvancedListingId,
                        listingId = listingId,
                        onOpenPicker = { showListingPicker = true },
                        onToggleAdvanced = { showAdvancedListingId = !showAdvancedListingId },
                        onListingIdChange = { listingId = it },
                    )
                }
                SellerListingPickerSheet(
                    visible = showListingPicker,
                    listingRepository = listingRepository,
                    selectedId = selectedId,
                    onDismiss = { showListingPicker = false },
                    onSelect = { item ->
                        listingId = item.id
                        selectedListingTitle = item.title
                    },
                )
                groupedToolFeatures(summary).forEach { (group, entries) ->
                    Text(
                        text = featureGroupLabel(group),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                    )
                    entries.forEach { entry ->
                        val kind = entry.feature.executionKind.ifBlank { "request" }
                        val showsCaption = kind != "boost" && entry.key != "seller_real_badge"
                        val needsListing = entry.feature.requiresListing
                        ToolActionCard(
                            featureKey = entry.key,
                            icon = featureIcon(entry.key, kind),
                            title = featureTitle(entry.key, entry.feature),
                            description = featureDescription(entry.key, entry.feature),
                            feature = entry.feature,
                            cta = featureCta(kind),
                            busy = submitting == entry.key,
                            enabled = submitting == null,
                            caption = if (showsCaption) caption else null,
                            onCaptionChange = if (showsCaption) ({ caption = it }) else null,
                            onUpgrade = { onUpgrade(entry.key) },
                            onSubmit = {
                                val listingArg = if (needsListing) selectedId else ""
                                runTool(entry.key, kind, needsListing, showsCaption) {
                                    repository.invokeFeature(entry.key, listingArg, caption)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageStatusCard(
    summary: UserEntitlementSummary?,
    loading: Boolean,
    onUpgrade: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val name = summary?.packageName?.ifBlank { summary.packageCode }.orEmpty()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(FashColors.Primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WorkspacePremium,
                        contentDescription = null,
                        tint = FashColors.Primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.seller_packages_entitlement_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    when {
                        loading && name.isBlank() -> Text(
                            text = stringResource(R.string.explore_filter_loading),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        name.isBlank() -> Text(
                            text = stringResource(R.string.seller_packages_tools_empty_package),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                        else -> {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = scheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.seller_packages_entitlement_active),
                                style = MaterialTheme.typography.labelSmall,
                                color = FashColors.Primary,
                            )
                        }
                    }
                }
            }
            if (name.isBlank() && !loading) {
                FashSecondaryButton(
                    onClick = onUpgrade,
                    label = stringResource(R.string.seller_packages_entitlement_upgrade),
                )
            }
        }
    }
}

@Composable
private fun ListingPickerSection(
    selectedTitle: String,
    hasSelection: Boolean,
    showAdvanced: Boolean,
    listingId: String,
    onOpenPicker: () -> Unit,
    onToggleAdvanced: () -> Unit,
    onListingIdChange: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.seller_packages_tools_pick_listing),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenPicker),
            shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
            colors = CardDefaults.cardColors(containerColor = scheme.surface),
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (hasSelection) selectedTitle else stringResource(R.string.seller_packages_tools_tap_pick_listing),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (hasSelection) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (hasSelection) scheme.onSurface else scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.seller_packages_tools_change_listing),
                    style = MaterialTheme.typography.labelMedium,
                    color = FashColors.Primary,
                )
            }
        }
        Text(
            text = stringResource(R.string.seller_packages_tools_advanced_listing_id),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = FashColors.Primary,
            modifier = Modifier.clickable(onClick = onToggleAdvanced),
        )
        if (showAdvanced) {
            FashFilledTextField(
                value = listingId,
                onValueChange = onListingIdChange,
                label = { Text(stringResource(R.string.seller_packages_tools_listing_id)) },
                singleLine = true,
            )
        }
    }
}

@Composable
private fun ToolActionCard(
    featureKey: String,
    icon: ImageVector,
    title: String,
    description: String,
    feature: FeatureUsageSummary?,
    cta: String,
    busy: Boolean,
    enabled: Boolean,
    onUpgrade: () -> Unit,
    onSubmit: () -> Unit,
    caption: String? = null,
    onCaptionChange: ((String) -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val canUse = feature.canUse()
    val locked = feature == null || !feature.enabled
    val disclaimer = feature?.disclaimerText?.takeIf { it.isNotBlank() }
    val boostHint = feature?.boostAffinityHint?.takeIf { it.isNotBlank() && featureKey == "explore_boost" }
    val showResearchResult = featureKey == "authenticity_verify" &&
        feature?.latestRequestStatus == "fulfilled" &&
        feature.latestConfidencePct != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(FashColors.Primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = FashColors.Primary, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                QuotaChip(feature = feature)
            }
            disclaimer?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(scheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                )
            }
            boostHint?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = FashColors.Primary,
                )
            }
            if (feature?.latestRequestStatus == "pending") {
                Text(
                    text = stringResource(R.string.seller_packages_tools_status_pending),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (showResearchResult) {
                Text(
                    text = stringResource(
                        R.string.seller_packages_tools_result_confidence,
                        feature.latestConfidencePct ?: 0,
                        feature.latestResultVerdict.ifBlank { "—" },
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = FashColors.Primary,
                )
            }
            if (featureKey == "seller_real_badge" && feature?.latestRequestStatus == "fulfilled") {
                Text(
                    text = stringResource(R.string.seller_packages_tools_badge_granted),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = FashColors.Primary,
                )
            }
            if (onCaptionChange != null && caption != null && canUse) {
                FashFilledTextField(
                    value = caption,
                    onValueChange = onCaptionChange,
                    label = { Text(stringResource(R.string.seller_packages_tools_caption)) },
                    singleLine = false,
                )
                Text(
                    text = stringResource(R.string.seller_packages_tools_caption_helper),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (locked) {
                Text(
                    text = stringResource(R.string.seller_packages_tools_locked_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                FashSecondaryButton(
                    onClick = onUpgrade,
                    label = stringResource(R.string.seller_packages_entitlement_upgrade),
                    enabled = enabled,
                )
            } else {
                FashPrimaryButton(
                    onClick = onSubmit,
                    enabled = enabled && canUse,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = LocalContentColor.current,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = cta,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun QuotaChip(feature: FeatureUsageSummary?) {
    val scheme = MaterialTheme.colorScheme
    val (label, selected) = when {
        feature == null || !feature.enabled -> stringResource(R.string.seller_packages_tools_quota_locked) to false
        feature.unlimited -> stringResource(R.string.seller_packages_tools_quota_unlimited) to true
        feature.remaining != null && feature.remaining <= 0L -> stringResource(R.string.seller_packages_tools_quota_exhausted) to false
        feature.remaining != null -> stringResource(R.string.seller_packages_tools_quota_remaining, feature.remaining) to true
        else -> stringResource(R.string.seller_packages_tools_quota_unlimited) to true
    }
    Surface(
        shape = RoundedCornerShape(FashTheme.spacing.radiusPill),
        color = if (selected) FashColors.Primary.copy(alpha = 0.14f) else scheme.surfaceVariant,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) FashColors.Primary else scheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

internal fun FeatureUsageSummary?.canUse(): Boolean {
    if (this == null || !enabled) return false
    if (unlimited) return true
    remaining?.let { return it > 0L }
    return true
}
