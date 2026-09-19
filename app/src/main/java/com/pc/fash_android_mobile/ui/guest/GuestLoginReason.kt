package com.pc.fash_android_mobile.ui.guest

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.ui.graphics.vector.ImageVector
import com.pc.fash_android_mobile.R

/** Why the guest login sheet was opened — drives copy and icon on [GuestLoginSheet]. */
enum class GuestLoginReason(
    @StringRes val messageRes: Int,
    val iconVector: ImageVector,
) {
    Profile(R.string.guest_login_reason_profile, Icons.Outlined.Person),
    Chat(R.string.guest_login_reason_chat, Icons.Outlined.ChatBubbleOutline),
    Post(R.string.guest_login_reason_post, Icons.Outlined.Sell),
    Orders(R.string.guest_login_reason_orders, Icons.Outlined.ShoppingCart),
    Notifications(R.string.guest_login_reason_notifications, Icons.Outlined.NotificationsNone),
    Saved(R.string.guest_login_reason_saved, Icons.Outlined.BookmarkBorder),
    Like(R.string.guest_login_reason_like, Icons.Outlined.FavoriteBorder),
    ChatFromHome(R.string.guest_login_reason_chat, Icons.Outlined.ChatBubbleOutline),
    SellFromHome(R.string.guest_login_reason_post, Icons.Outlined.Sell),
    BuyOrChat(R.string.guest_login_reason_buy, Icons.Outlined.ShoppingCart),
    Follow(R.string.guest_login_reason_follow, Icons.Outlined.PersonAdd),
    /** Friend invite deep link — sign up to join and attribute referral token. */
    Invite(R.string.guest_login_reason_invite, Icons.Outlined.Share),
    /** Top-bar "Sign in" chip on Home / Explore while browsing without an account. */
    TopBar(R.string.guest_login_reason_topbar, Icons.Outlined.Lock),
    /** Explore "Match my size" filter — needs a profile with saved sizing reference. */
    SizingMatch(R.string.guest_login_reason_sizing_match, Icons.Outlined.Straighten),
    /** Explore "Shop nearby" filter — needs a default shipping address or manual pick. */
    BrowseLocation(R.string.guest_login_reason_browse_location, Icons.Outlined.LocationOn),
    /** Home "Gợi ý riêng cho bạn" — personalized recommendations need an account. */
    HomeForYou(R.string.guest_login_reason_home_for_you, Icons.Outlined.AutoAwesome),
    /** Home "Từ shop bạn theo dõi" — follow feed requires sign-in. */
    HomeFollowing(R.string.guest_login_reason_home_following, Icons.Outlined.Group),
    /** Home "Đúng gu của bạn" — style picks use profile taste signals. */
    HomeStylePicks(R.string.guest_login_reason_home_style, Icons.Outlined.AutoAwesome),
    /** Home "Tương tự món đã lưu" — needs wishlist history. */
    HomeSimilarSaved(R.string.guest_login_reason_home_similar, Icons.Outlined.AutoAwesome),
    /** Promo carousel / CMS CTA — sign up for daily picks. */
    PromoSignUp(R.string.guest_login_reason_home_for_you, Icons.Outlined.AutoAwesome),
}
