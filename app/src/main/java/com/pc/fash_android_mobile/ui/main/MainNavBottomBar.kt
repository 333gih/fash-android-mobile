package com.pc.fash_android_mobile.ui.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.onboarding.FeatureTourAnchor
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn

private val NavIconSize = 26.dp
private val NavFabSize = 52.dp
private val NavIconLabelGap = 4.dp
private val NavSlotMinHeight = 56.dp

/**
 * Divider + nav row + vertical padding — matches [MainNavBottomBar] layout; excludes system navigation bar.
 * Use for fullscreen overlays (e.g. global dialogs) so content clears the tab bar.
 */
val MainNavBottomBarOverlayInset = 1.dp + 72.dp + 20.dp

/**
 * Approximate space to reserve above the system nav for [com.pc.fash_android_mobile.ui.chat.ChatDetailScreen]
 * composer row (divider + optional typing strip + input bar). Excludes system navigation bar insets.
 * Used so global message dialogs do not cover the typing field when chat is open.
 */
val ChatComposerBarOverlayInset = 100.dp

/**
 * Bottom bar with a centered Post action (FAB). Slots share equal width; icons and labels align
 * on one row without a raised FAB that breaks the bar's top edge.
 */
@Composable
fun MainNavBottomBar(
    selectedTab: Int,
    chatUnreadCount: Int,
    onTabChange: (Int) -> Unit,
    /**
     * Tap the already-selected tab — host should reload that tab and drive [isTabNavLoading].
     */
    onTabReselected: (MainTab) -> Unit = {},
    /** When true, the tab icon shows a spinner instead of its glyph (re-tap reload in progress). */
    isTabNavLoading: (MainTab) -> Boolean = { false },
    modifier: Modifier = Modifier,
    /** When true, reports anchor bounds for the first-launch feature tour overlay. */
    tourAnchorsEnabled: Boolean = false,
    onTourAnchorPositioned: (FeatureTourAnchor, LayoutCoordinates?) -> Unit = { _, _ -> },
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = scheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 1.dp,
                color = scheme.outlineVariant.copy(alpha = 0.72f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MainNavSideItem(
                    tab = MainTab.Home,
                    selected = selectedTab == MainTab.Home.ordinal,
                    showLoading = isTabNavLoading(MainTab.Home),
                    chatUnreadCount = 0,
                    onClick = {
                        if (selectedTab == MainTab.Home.ordinal) {
                            onTabReselected(MainTab.Home)
                        } else {
                            onTabChange(MainTab.Home.ordinal)
                        }
                    },
                    tourAnchorsEnabled = tourAnchorsEnabled,
                    tourAnchor = FeatureTourAnchor.BottomHome,
                    onTourAnchorPositioned = onTourAnchorPositioned,
                )
                MainNavSideItem(
                    tab = MainTab.Orders,
                    selected = selectedTab == MainTab.Orders.ordinal,
                    showLoading = isTabNavLoading(MainTab.Orders),
                    chatUnreadCount = 0,
                    onClick = {
                        if (selectedTab == MainTab.Orders.ordinal) {
                            onTabReselected(MainTab.Orders)
                        } else {
                            onTabChange(MainTab.Orders.ordinal)
                        }
                    },
                    tourAnchorsEnabled = tourAnchorsEnabled,
                    tourAnchor = FeatureTourAnchor.BottomOrders,
                    onTourAnchorPositioned = onTourAnchorPositioned,
                )
                MainNavPostFab(
                    selected = selectedTab == MainTab.Post.ordinal,
                    showLoading = isTabNavLoading(MainTab.Post),
                    onClick = {
                        if (selectedTab == MainTab.Post.ordinal) {
                            onTabReselected(MainTab.Post)
                        } else {
                            onTabChange(MainTab.Post.ordinal)
                        }
                    },
                    tourAnchorsEnabled = tourAnchorsEnabled,
                    onTourAnchorPositioned = onTourAnchorPositioned,
                )
                MainNavSideItem(
                    tab = MainTab.Chat,
                    selected = selectedTab == MainTab.Chat.ordinal,
                    showLoading = isTabNavLoading(MainTab.Chat),
                    chatUnreadCount = chatUnreadCount,
                    onClick = {
                        if (selectedTab == MainTab.Chat.ordinal) {
                            onTabReselected(MainTab.Chat)
                        } else {
                            onTabChange(MainTab.Chat.ordinal)
                        }
                    },
                    tourAnchorsEnabled = tourAnchorsEnabled,
                    tourAnchor = FeatureTourAnchor.BottomChat,
                    onTourAnchorPositioned = onTourAnchorPositioned,
                )
                MainNavSideItem(
                    tab = MainTab.Profile,
                    selected = selectedTab == MainTab.Profile.ordinal,
                    showLoading = isTabNavLoading(MainTab.Profile),
                    chatUnreadCount = 0,
                    onClick = {
                        if (selectedTab == MainTab.Profile.ordinal) {
                            onTabReselected(MainTab.Profile)
                        } else {
                            onTabChange(MainTab.Profile.ordinal)
                        }
                    },
                    tourAnchorsEnabled = tourAnchorsEnabled,
                    tourAnchor = FeatureTourAnchor.BottomProfile,
                    onTourAnchorPositioned = onTourAnchorPositioned,
                )
            }
        }
    }
}

@Composable
private fun RowScope.MainNavSideItem(
    tab: MainTab,
    selected: Boolean,
    showLoading: Boolean,
    chatUnreadCount: Int,
    onClick: () -> Unit,
    tourAnchorsEnabled: Boolean,
    tourAnchor: FeatureTourAnchor,
    onTourAnchorPositioned: (FeatureTourAnchor, LayoutCoordinates?) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val iconTint = if (selected) FashColors.Primary else scheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = NavSlotMinHeight)
            .clickable(enabled = !showLoading, onClick = onClick)
            .semantics { role = Role.Tab }
            .then(
                if (tourAnchorsEnabled) {
                    Modifier.onGloballyPositioned { coords ->
                        onTourAnchorPositioned(tourAnchor, coords.takeIf { it.isAttached })
                    }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(NavIconSize),
            contentAlignment = Alignment.Center,
        ) {
            if (showLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = FashColors.Primary,
                    strokeWidth = 2.dp,
                )
            } else if (tab == MainTab.Chat && chatUnreadCount > 0) {
                BadgedBox(
                    badge = {
                        androidx.compose.material3.Badge(containerColor = FashColors.Primary) {
                            Text(
                                text = if (chatUnreadCount > 99) "99+" else chatUnreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = FashColors.Primary.fashReadableOn(),
                            )
                        }
                    },
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = stringResource(tab.labelRes),
                        tint = iconTint,
                        modifier = Modifier.size(NavIconSize),
                    )
                }
            } else {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = stringResource(tab.labelRes),
                    tint = iconTint,
                    modifier = Modifier.size(NavIconSize),
                )
            }
        }
        Spacer(Modifier.height(NavIconLabelGap))
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = iconTint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.MainNavPostFab(
    selected: Boolean,
    showLoading: Boolean,
    onClick: () -> Unit,
    tourAnchorsEnabled: Boolean,
    onTourAnchorPositioned: (FeatureTourAnchor, LayoutCoordinates?) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.98f,
        label = "postFabScale",
    )
    val elevation by animateDpAsState(
        targetValue = if (selected) 8.dp else 5.dp,
        label = "postFabElevation",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = NavSlotMinHeight)
            .padding(horizontal = 2.dp)
            .semantics { role = Role.Tab }
            .then(
                if (tourAnchorsEnabled) {
                    Modifier.onGloballyPositioned { coords ->
                        onTourAnchorPositioned(
                            FeatureTourAnchor.BottomPostFab,
                            coords.takeIf { it.isAttached },
                        )
                    }
                } else {
                    Modifier
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .size(NavFabSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            shape = CircleShape,
            containerColor = FashColors.Primary,
            contentColor = FashColors.Primary.fashReadableOn(),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = elevation,
                pressedElevation = elevation + 2.dp,
                focusedElevation = elevation,
                hoveredElevation = elevation + 1.dp,
            ),
        ) {
            if (showLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = FashColors.Primary.fashReadableOn(),
                    strokeWidth = 2.5.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.nav_post_fab_cd),
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Spacer(Modifier.height(NavIconLabelGap))
        Text(
            text = stringResource(R.string.nav_post_fab_label),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.4.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = if (selected) FashColors.Primary else scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .clickable(enabled = !showLoading, onClick = onClick)
                .padding(vertical = 2.dp),
        )
    }
}
