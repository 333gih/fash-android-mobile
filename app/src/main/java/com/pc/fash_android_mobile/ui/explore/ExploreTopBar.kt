package com.pc.fash_android_mobile.ui.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.components.FashBrandMarkText
import com.pc.fash_android_mobile.ui.components.FashInboxNotificationIconButton
import com.pc.fash_android_mobile.ui.guest.GuestTopBarSignInAction
import com.pc.fash_android_mobile.ui.main.MainTab
import com.pc.fash_android_mobile.ui.theme.FashBrandTypography
import com.pc.fash_android_mobile.ui.theme.FashColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreTopBar(
    viewModel: ExploreViewModel,
    /** Server total unread inbox rows ([NotificationsViewModel.unreadCount]). */
    inboxUnreadCount: Int,
    onOrdersClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    showGuestSignIn: Boolean = false,
    onGuestSignInClick: () -> Unit = {},
) {
    val searchBarExpanded by viewModel.searchBarExpanded.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val explorePrimarySection by viewModel.primarySection.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(searchBarExpanded) {
        if (searchBarExpanded) {
            focusRequester.requestFocus()
        }
    }

    TopAppBar(
        title = {
            AnimatedContent(
                targetState = searchBarExpanded,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith
                        fadeOut(animationSpec = tween(160))
                },
                label = "exploreSearchTitle",
            ) { expanded ->
                if (!expanded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FashBrandMarkText(
                            text = stringResource(R.string.brand_wordmark),
                            style = FashBrandTypography.markBoldItalicMedium,
                        )
                        Text(
                            text = stringResource(MainTab.Explore.headerSuffixRes),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        placeholder = {
                            if (explorePrimarySection == ExplorePrimarySection.Sellers) {
                                val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                Text(
                                    text = stringResource(R.string.explore_search_placeholder_sellers),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = muted,
                                )
                            } else {
                                ExploreSearchPlaceholder()
                            }
                        },
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FashColors.Primary.copy(alpha = 0.45f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            cursorColor = FashColors.Primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    keyboard?.hide()
                                    viewModel.submitSearch()
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.search_label),
                                    tint = FashColors.Primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboard?.hide()
                                viewModel.submitSearch()
                            },
                        ),
                    )
                }
            }
        },
        navigationIcon = {
            if (searchBarExpanded) {
                IconButton(
                    onClick = {
                        keyboard?.hide()
                        viewModel.setSearchBarExpanded(false)
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                IconButton(onClick = { viewModel.requestSearchBarExpanded() }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_label),
                        tint = FashColors.Primary,
                    )
                }
            }
        },
        actions = {
            if (showGuestSignIn) {
                GuestTopBarSignInAction(onClick = onGuestSignInClick)
            } else {
                FashInboxNotificationIconButton(
                    unreadCount = inboxUnreadCount,
                    onClick = onNotificationsClick,
                )
                IconButton(onClick = onOrdersClick) {
                    Icon(
                        imageVector = Icons.Default.LocalMall,
                        contentDescription = stringResource(R.string.orders_icon_cd),
                        tint = FashColors.Primary,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

/** Muted “FASH.” + Explore lockup used as the search field placeholder. */
@Composable
private fun ExploreSearchPlaceholder() {
    val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 0.dp),
    ) {
        FashBrandMarkText(
            text = stringResource(R.string.brand_wordmark),
            style = FashBrandTypography.markBoldItalicSmall,
            color = muted,
        )
        Text(
            text = stringResource(MainTab.Explore.headerSuffixRes),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = muted,
        )
    }
}
