package com.pc.fash_android_mobile.ui.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors

/**
 * Bottom bar with a raised circular Post action (FAB) in the center — matches editorial “ĐĂNG TIN” pattern.
 * Other tabs use icon + label in equal slots.
 */
@Composable
fun MainNavBottomBar(
    selectedTab: Int,
    chatUnreadCount: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp),
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
                onClick = { onTabChange(MainTab.Explore.ordinal) },
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
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
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
                            color = FashColors.OnPrimary,
                        )
                    }
                },
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = stringResource(tab.labelRes),
                    tint = if (selected) FashColors.Primary else scheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            Icon(
                imageVector = tab.icon,
                contentDescription = stringResource(tab.labelRes),
                tint = if (selected) FashColors.Primary else scheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) FashColors.Primary else scheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
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
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        label = "postFabScale",
    )
    val fabLift by animateDpAsState(
        targetValue = if (selected) 10.dp else 8.dp,
        label = "postFabLift",
    )
    val elevation by animateDpAsState(
        targetValue = if (selected) 10.dp else 7.dp,
        label = "postFabElevation",
    )

    Column(
        modifier = Modifier
            .weight(1.2f)
            .padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.offset(y = -fabLift),
        ) {
            FloatingActionButton(
                onClick = onClick,
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                shape = CircleShape,
                containerColor = FashColors.Primary,
                contentColor = FashColors.OnPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = elevation,
                    pressedElevation = elevation + 3.dp,
                    focusedElevation = elevation,
                    hoveredElevation = elevation + 1.dp,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.nav_post_fab_cd),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.nav_post_fab_label),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.6.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = if (selected) FashColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
