package com.pc.fash_android_mobile.ui.guest

import androidx.annotation.StringRes
import com.pc.fash_android_mobile.R

/** Why the guest login sheet was opened — drives copy on [GuestLoginSheet]. */
enum class GuestLoginReason(@StringRes val messageRes: Int) {
    Profile(R.string.guest_login_reason_profile),
    Chat(R.string.guest_login_reason_chat),
    Post(R.string.guest_login_reason_post),
    Orders(R.string.guest_login_reason_orders),
    Notifications(R.string.guest_login_reason_notifications),
    Saved(R.string.guest_login_reason_saved),
    Like(R.string.guest_login_reason_like),
    ChatFromHome(R.string.guest_login_reason_chat),
    SellFromHome(R.string.guest_login_reason_post),
    BuyOrChat(R.string.guest_login_reason_buy),
    Follow(R.string.guest_login_reason_follow),
    /** Friend invite deep link — sign up to join and attribute referral token. */
    Invite(R.string.guest_login_reason_invite),
    /** Top-bar “Sign in” chip on Home / Explore while browsing without an account. */
    TopBar(R.string.guest_login_reason_topbar),
    /** Explore “Match my size” filter — needs a profile with saved sizing reference. */
    SizingMatch(R.string.guest_login_reason_sizing_match),
    /** Explore “Shop nearby” filter — needs a default shipping address or manual pick. */
    BrowseLocation(R.string.guest_login_reason_browse_location),
    /** Home “Gợi ý riêng cho bạn” — personalized recommendations need an account. */
    HomeForYou(R.string.guest_login_reason_home_for_you),
    /** Home “Từ shop bạn theo dõi” — follow feed requires sign-in. */
    HomeFollowing(R.string.guest_login_reason_home_following),
    /** Home “Đúng gu của bạn” — style picks use profile taste signals. */
    HomeStylePicks(R.string.guest_login_reason_home_style),
    /** Home “Tương tự món đã lưu” — needs wishlist history. */
    HomeSimilarSaved(R.string.guest_login_reason_home_similar),
    /** Promo carousel / CMS CTA — sign up for daily picks. */
    PromoSignUp(R.string.guest_login_reason_home_for_you),
}
