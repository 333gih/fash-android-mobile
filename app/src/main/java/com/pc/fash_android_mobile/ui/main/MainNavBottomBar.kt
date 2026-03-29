package com.pc.fash_android_mobile.ui.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn

private val NavIconSize = 26.dp
private val NavFabSize = 52.dp
private val NavIconLabelGap = 4.dp
private val NavSlotMinHeight = 56.dp

/**
 * Bottom bar with a centered Post action (FAB). Slots share equal width; icons and labels align
 * on one row without a raised FAB that breaks the bar’s top edge.
 */
@Composable
fun MainNavBottomBar(
    selectedTab: Int,
    chatUnreadCount: Int,
    onTabChange: (Int) -> Unit,
    /** Tap Explore while Explore is already selected (e.g. scroll Explore to top). */
    onExploreReselected: () -> Unit = {},
    modifier: Modifier = Modifier,
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
                color = scheme.outlineVariant.copy(alpha = 0.45f),
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
                    chatUnreadCount = 0,
                    onClick = { onTabChange(MainTab.Home.ordinal) },
                )
                MainNavSideItem(
                    tab = MainTab.Explore,
                    selected = selectedTab == MainTab.Explore.ordinal,
                    chatUnreadCount = 0,
                    onClick = {
                        if (selectedTab == MainTab.Explore.ordinal) {
                            onExploreReselected()
                        } else {
                            onTabChange(MainTab.Explore.ordinal)
                        }
                    },
                )
                MainNavPostFab(
                    selected = selectedTab == MainTab.Post.ordinal,
                    onClick = { onTabChange(MainTab.Post.ordinal) },
                )
                MainNavSideItem(
                    tab = MainTab.Chat,
                    selected = selectedTab == MainTab.Chat.ordinal,
                    chatUnreadCount = chatUnreadCount,
                    onClick = { onTabChange(MainTab.Chat.ordinal) },
                )
                MainNavSideItem(
                    tab = MainTab.Profile,
                    selected = selectedTab == MainTab.Profile.ordinal,
                    chatUnreadCount = 0,
                    onClick = { onTabChange(MainTab.Profile.ordinal) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.MainNavSideItem(
    tab: MainTab,
    selected: Boolean,
    chatUnreadCount: Int,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = NavSlotMinHeight)
            .clickable(onClick = onClick)
            .semantics { role = Role.Tab }
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (tab == MainTab.Chat && chatUnreadCount > 0) {
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
                    tint = if (selected) FashColors.Primary else scheme.onSurfaceVariant,
                    modifier = Modifier.size(NavIconSize),
                )
            }
        } else {
            Icon(
                imageVector = tab.icon,
                contentDescription = stringResource(tab.labelRes),
                tint = if (selected) FashColors.Primary else scheme.onSurfaceVariant,
                modifier = Modifier.size(NavIconSize),
            )
        }
        Spacer(Modifier.height(NavIconLabelGap))
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) FashColors.Primary else scheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.MainNavPostFab(
    selected: Boolean,
    onClick: () -> Unit,
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
            .semantics { role = Role.Tab },
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
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.nav_post_fab_cd),
                modifier = Modifier.size(26.dp),
            )
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
                .clickable(onClick = onClick)
                .padding(vertical = 2.dp),
        )
    }
}
