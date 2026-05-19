package com.pc.fash_android_mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pc.fash_android_mobile.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.pc.fash_android_mobile.data.auth.buildGoogleSignInClient
import com.pc.fash_android_mobile.data.auth.clearCachedSocialSignInForLogout
import com.pc.fash_android_mobile.ui.explore.ExplorePrimarySection
import com.pc.fash_android_mobile.ui.explore.ExploreViewModel
import com.pc.fash_android_mobile.ui.explore.FeaturedSellersScreen
import com.pc.fash_android_mobile.ui.explore.FeaturedSellersViewModel
import com.pc.fash_android_mobile.ui.home.HomeDeliveringScreen
import com.pc.fash_android_mobile.ui.home.HomeDeliveringViewModel
import com.pc.fash_android_mobile.ui.home.HomeViewModel
import com.pc.fash_android_mobile.ui.invite.InviteFriendsScreen
import com.pc.fash_android_mobile.ui.listing.EditListingScreen
import com.pc.fash_android_mobile.ui.listing.EditListingViewModel
import com.pc.fash_android_mobile.ui.listing.ProductDetailScreen
import com.pc.fash_android_mobile.ui.listing.ProductDetailViewModel
import com.pc.fash_android_mobile.ui.main.tabs.SellerProfileScreen
import com.pc.fash_android_mobile.ui.main.tabs.SellerProfileViewModel
import com.pc.fash_android_mobile.ui.checkout.CheckoutScreen
import com.pc.fash_android_mobile.ui.checkout.CheckoutViewModel
import com.pc.fash_android_mobile.data.chat.ConversationItem
import com.pc.fash_android_mobile.ui.chat.ChatDetailScreen
import com.pc.fash_android_mobile.ui.chat.ChatDetailViewModel
import com.pc.fash_android_mobile.ui.chat.ChatShipFlowArgs
import com.pc.fash_android_mobile.ui.chat.ChatShipFulfillmentScreen
import com.pc.fash_android_mobile.ui.chat.ShipFlowSource
import com.pc.fash_android_mobile.ui.chat.ChatViewModel
import com.pc.fash_android_mobile.ui.profile.EditProfileScreen
import com.pc.fash_android_mobile.ui.profile.EditProfileViewModel
import com.pc.fash_android_mobile.ui.post.PostViewModel
import com.pc.fash_android_mobile.ui.follow.FollowConnectionsScreen
import com.pc.fash_android_mobile.ui.follow.FollowConnectionsViewModel
import com.pc.fash_android_mobile.ui.main.ChatComposerBarOverlayInset
import com.pc.fash_android_mobile.ui.main.MainNavBottomBarOverlayInset
import com.pc.fash_android_mobile.ui.main.MainNavScreen
import com.pc.fash_android_mobile.ui.main.MainTab
import com.pc.fash_android_mobile.ui.navigation.SellerShopEntrySource
import com.pc.fash_android_mobile.ui.navigation.SellerShopRestoreContext
import com.pc.fash_android_mobile.ui.common.ReloadWhenVisible
import com.pc.fash_android_mobile.data.locale.AppLocale
import com.pc.fash_android_mobile.ui.main.PromoSlidesViewModel
import com.pc.fash_android_mobile.data.sellerpackages.SellerProductPackage
import com.pc.fash_android_mobile.ui.sellerpackages.SellerPackageCheckoutScreen
import com.pc.fash_android_mobile.ui.sellerpackages.SellerProductPackagesScreen
import com.pc.fash_android_mobile.ui.sellerpackages.SellerProductPackagesViewModel
import com.pc.fash_android_mobile.ui.login.LoginScreen
import com.pc.fash_android_mobile.ui.onboarding.OnboardingFlowProgress
import com.pc.fash_android_mobile.ui.onboarding.OnboardingScreen
import com.pc.fash_android_mobile.ui.onboarding.OnboardingViewModel
import com.pc.fash_android_mobile.ui.onboarding.OnboardingStep
import com.pc.fash_android_mobile.ui.onboarding.SizingReferenceScreen
import com.pc.fash_android_mobile.ui.onboarding.SetupPasswordOnboardScreen
import com.pc.fash_android_mobile.ui.onboarding.UsernameOnboardScreen
import com.pc.fash_android_mobile.ui.login.LoginStep
import com.pc.fash_android_mobile.ui.login.LoginViewModel
import com.pc.fash_android_mobile.ui.login.OtpVerifyScreen
import com.pc.fash_android_mobile.ui.settings.ChangePasswordViewModel
import com.pc.fash_android_mobile.ui.splash.FashWaitingScreen
import com.pc.fash_android_mobile.ui.splash.SetupGateRetryScreen
import com.pc.fash_android_mobile.ui.components.FashGlobalDialogHost
import com.pc.fash_android_mobile.ui.components.FashAppPromoOverlayDialog
import com.pc.fash_android_mobile.data.promo.AppPromoCampaign
import com.pc.fash_android_mobile.data.promo.AppPromoCampaignKind
import com.pc.fash_android_mobile.data.promo.AppPromoCampaignResolver
import com.pc.fash_android_mobile.data.promo.AppPromoCampaignStore
import com.pc.fash_android_mobile.data.promo.AppPromoGateContext
import com.pc.fash_android_mobile.data.promo.AppPromoNavigation
import com.pc.fash_android_mobile.data.promo.AppPromoPendingQueue
import com.pc.fash_android_mobile.data.promo.isAppPromoPushData
import com.pc.fash_android_mobile.data.promo.parseAppPromoFromPushData
import com.pc.fash_android_mobile.data.promo.parseRemoteAppPromoPayload
import com.pc.fash_android_mobile.data.promo.toAppPromoCampaign
import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.ui.components.FashInAppNotificationBanner
import com.pc.fash_android_mobile.ui.components.FashSnackbarHost
import com.pc.fash_android_mobile.ui.components.rememberSerialSnackbarChannel
import com.pc.fash_android_mobile.ui.locale.ProvideAppLocale
import com.pc.fash_android_mobile.ui.theme.FashLightAppearance
import com.pc.fash_android_mobile.ui.components.FashPromoSlideDef
import com.pc.fash_android_mobile.ui.components.toFashPromoSlideDef
import com.pc.fash_android_mobile.ui.theme.FashTheme
import com.pc.fash_android_mobile.ui.address.AddEditAddressScreen
import com.pc.fash_android_mobile.ui.address.AddressBookViewModel
import com.pc.fash_android_mobile.ui.address.ShippingAddressListScreen
import com.pc.fash_android_mobile.ui.orders.OrderDetailScreen
import com.pc.fash_android_mobile.ui.orders.LocalPendingPaymentRootCoordinates
import com.pc.fash_android_mobile.ui.orders.LocalPendingPaymentSliderRegistry
import com.pc.fash_android_mobile.ui.orders.PendingPaymentBanner
import com.pc.fash_android_mobile.ui.orders.PendingPaymentEvent
import com.pc.fash_android_mobile.ui.orders.PendingPaymentSliderRegistry
import com.pc.fash_android_mobile.ui.orders.PendingPaymentViewModel
import com.pc.fash_android_mobile.ui.orders.OrderDetailViewModel
import com.pc.fash_android_mobile.data.realtime.RealtimeEvent
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.config.BusinessFlowConfig
import com.pc.fash_android_mobile.deeplink.AccountSwitchDeepLinks
import com.pc.fash_android_mobile.deeplink.InboxDeepLinks
import com.pc.fash_android_mobile.deeplink.InviteDeepLinks
import com.pc.fash_android_mobile.deeplink.ListingDeepLinks
import com.pc.fash_android_mobile.data.theme.AppThemePreference
import com.pc.fash_android_mobile.data.onboarding.AppFeatureTourStore
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SPLASH_DISPLAY_MS = 2_500L

/** Delay between access-status polls after onboard + sizing (eventual consistency on server). */
private const val ACCESS_STATUS_POLL_MS = 350L
private const val ACCESS_STATUS_POLL_ATTEMPTS = 5

/** Initial GET setup-status retries after login / cold start (transient network / cold LB). */
private const val SETUP_STATUS_INITIAL_RETRY_MS = 450L
private const val SETUP_STATUS_INITIAL_ATTEMPTS = 4

/**
 * After [UserRepository.onboard] (username step) succeeds, the access-status endpoint can briefly still
 * return [com.pc.fash_android_mobile.data.user.UserAccessStatus.canAccessHome] false.
 * Poll a few times; if still not flipped, allow home anyway (write already succeeded).
 */
private suspend fun refreshNeedsOnboardingFlag(repo: UserRepository): Boolean =
    repo.getUserAccessStatus().fold(
        onSuccess = { !it.canAccessHome },
        // If we cannot confirm setup-status, stay in onboarding rather than opening main with a broken profile.
        onFailure = { true },
    )

/**
 * After a client onboarding step, access-status can briefly report [canAccessHome] true while the
 * ViewModel still has steps left — keep the onboarding shell until [OnboardingStep.Completed].
 */
private suspend fun resolveShellNeedsOnboardingAfterStep(
    repo: UserRepository,
    onboardingVm: OnboardingViewModel,
): Boolean {
    val apiStillNeeds = refreshNeedsOnboardingFlag(repo)
    val step = onboardingVm.onboardingStep.value
    return apiStillNeeds || step != OnboardingStep.Completed
}

private suspend fun resolveNeedsOnboardingAfterProfileSubmit(repo: UserRepository): Boolean {
    repeat(ACCESS_STATUS_POLL_ATTEMPTS) { attempt ->
        repo.getUserAccessStatus().fold(
            onSuccess = { status ->
                if (status.canAccessHome) return false
            },
            onFailure = { },
        )
        if (attempt < ACCESS_STATUS_POLL_ATTEMPTS - 1) delay(ACCESS_STATUS_POLL_MS)
    }
    return false
}

class MainActivity : ComponentActivity() {

    private val callbackManager: CallbackManager = CallbackManager.Factory.create()
    private val loginViewModel: LoginViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val homeDeliveringViewModel: HomeDeliveringViewModel by viewModels()
    private val exploreViewModel: ExploreViewModel by viewModels()
    private val productDetailViewModel: ProductDetailViewModel by viewModels()
    private val editListingViewModel: EditListingViewModel by viewModels()
    private val postViewModel: PostViewModel by viewModels()
    private val profileViewModel: com.pc.fash_android_mobile.ui.main.tabs.ProfileViewModel by viewModels()
    private val sellerProfileViewModel: SellerProfileViewModel by viewModels()
    private val editProfileViewModel: EditProfileViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()
    private val chatDetailViewModel: ChatDetailViewModel by viewModels()
    private val checkoutViewModel: CheckoutViewModel by viewModels()
    private val ordersViewModel: com.pc.fash_android_mobile.ui.orders.OrdersViewModel by viewModels()
    private val pendingPaymentViewModel: PendingPaymentViewModel by viewModels()
    private val orderDetailViewModel: OrderDetailViewModel by viewModels()
    private val addressBookViewModel: AddressBookViewModel by viewModels()
    private val followConnectionsViewModel: FollowConnectionsViewModel by viewModels()
    private val featuredSellersViewModel: FeaturedSellersViewModel by viewModels()
    private val changePasswordViewModel: ChangePasswordViewModel by viewModels()
    private val notificationsViewModel: com.pc.fash_android_mobile.ui.notifications.NotificationsViewModel by viewModels()
    private val promoSlidesViewModel: PromoSlidesViewModel by viewModels()
    private val sellerProductPackagesViewModel: SellerProductPackagesViewModel by viewModels()
    private val authManager get() = (application as FashApplication).authManager

    private val fashApp get() = application as FashApplication

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyLaunchNavigationIntents(intent)
    }

    private fun applyLaunchNavigationIntents(intent: Intent?) {
        if (InviteDeepLinks.parseInviteOpenFromIntent(intent)) {
            fashApp.pendingOpenInviteFriends.value = true
        }
        InviteDeepLinks.parseReferralTokenFromIntent(intent)?.let { fashApp.pendingReferralToken.value = it }
        fashApp.pendingDeepLinkListingId.value = ListingDeepLinks.parseListingIdFromIntent(intent)
        InboxDeepLinks.parseNotificationIdFromIntent(intent)?.let { fashApp.pendingInboxNotificationId.value = it }
        AccountSwitchDeepLinks.parseFromIntent(intent)?.let { fashApp.requestAccountSwitchPrompt(it) }
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            loginViewModel.onGoogleSignInSuccess(account.idToken, account.email)
        } catch (e: ApiException) {
            loginViewModel.onGoogleSignInFailure(e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyLaunchNavigationIntents(intent)

        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    loginViewModel.onFacebookSuccess(result)
                }

                override fun onCancel() {
                    loginViewModel.onFacebookCancel()
                }

                override fun onError(error: FacebookException) {
                    loginViewModel.onFacebookError(error)
                }
            },
        )

        enableEdgeToEdge()
        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val enqueueSnackbarSerial = rememberSerialSnackbarChannel(snackbarHostState)
            LaunchedEffect(Unit) {
                launch { loginViewModel.events.collect { enqueueSnackbarSerial { showSnackbar(it) } } }
                launch { onboardingViewModel.events.collect { enqueueSnackbarSerial { showSnackbar(it) } } }
                launch { homeViewModel.events.collect { enqueueSnackbarSerial { showSnackbar(it) } } }
                launch { exploreViewModel.events.collect { enqueueSnackbarSerial { showSnackbar(it) } } }
                launch { productDetailViewModel.events.collect { enqueueSnackbarSerial { showSnackbar(it) } } }
                launch { postViewModel.events.collect { enqueueSnackbarSerial { showSnackbar(it) } } }
                launch { editProfileViewModel.events.collect { enqueueSnackbarSerial { showSnackbar(it) } } }
                launch { chatViewModel.events.collect { enqueueSnackbarSerial { showSnackbar(it) } } }
                launch { chatDetailViewModel.events.collect { enqueueSnackbarSerial { showSnackbar(it) } } }
                launch { checkoutViewModel.events.collect { enqueueSnackbarSerial { showSnackbar(it) } } }
                launch { ordersViewModel.events.collect { enqueueSnackbarSerial { showSnackbar(it) } } }
                launch { orderDetailViewModel.events.collect { enqueueSnackbarSerial { showSnackbar(it) } } }
                launch { addressBookViewModel.events.collect { enqueueSnackbarSerial { showSnackbar(it) } } }
                launch { sellerProfileViewModel.events.collect { enqueueSnackbarSerial { showSnackbar(it) } } }
                launch { profileViewModel.events.collect { enqueueSnackbarSerial { showSnackbar(it) } } }
            }

            val email by loginViewModel.email.collectAsState()
            val loginStep by loginViewModel.loginStep.collectAsState()
            val otpCode by loginViewModel.otpCode.collectAsState()
            val resendCooldownSec by loginViewModel.resendCooldownSec.collectAsState()
            val isOtpLoading by loginViewModel.isOtpLoading.collectAsState()
            val isVerifyLoading by loginViewModel.isVerifyLoading.collectAsState()
            val isSocialLoading by loginViewModel.isSocialLoading.collectAsState()
            val password by loginViewModel.password.collectAsState()
            val usePasswordLogin by loginViewModel.usePasswordLogin.collectAsState()
            val isPasswordLoading by loginViewModel.isPasswordLoading.collectAsState()
            val isAuthenticated by authManager.isAuthenticated.collectAsState(initial = false)
            val sessionExpiredMessage by authManager.sessionExpiredMessage.collectAsState()
            // Show snackbar when the server force-expires the session, then navigate to login
            LaunchedEffect(sessionExpiredMessage) {
                val msg = sessionExpiredMessage ?: return@LaunchedEffect
                enqueueSnackbarSerial {
                    showSnackbar(msg)
                    authManager.clearSessionExpiredMessage()
                }
            }
            // Not saveable: a persisted false would skip re-fetching access-status after process restore (wrong home).
            var needsOnboarding by remember { mutableStateOf<Boolean?>(null) }
            var setupGateFetchFailed by remember { mutableStateOf(false) }
            var setupGateAttempt by remember { mutableIntStateOf(0) }
            val setupGateRecheckGen by fashApp.setupGateRecheckGeneration.collectAsState()
            val mainScope = rememberCoroutineScope()
            val clearLocalSessionAndSocial: () -> Unit = remember(mainScope) {
                {
                    mainScope.launch {
                        withContext(Dispatchers.IO) {
                            clearCachedSocialSignInForLogout(this@MainActivity.applicationContext)
                        }
                        authManager.sessionStore.clear()
                        authManager.onSessionCleared()
                    }
                    Unit
                }
            }
            val isLoggingOut by loginViewModel.isLoggingOut.collectAsState()
            val onboardingStep by onboardingViewModel.onboardingStep.collectAsState()
            val onboardingTags by onboardingViewModel.tags.collectAsState()
            val onboardingSelected by onboardingViewModel.selectedIds.collectAsState()
            val onboardingUsername by onboardingViewModel.username.collectAsState()
            val onboardingReferenceSize by onboardingViewModel.referenceSize.collectAsState()
            val onboardingMeasurementUnit by onboardingViewModel.measurementUnit.collectAsState()
            val onboardingMeasHem by onboardingViewModel.measurementHem.collectAsState()
            val onboardingMeasChest by onboardingViewModel.measurementChest.collectAsState()
            val onboardingMeasLength by onboardingViewModel.measurementLength.collectAsState()
            val onboardingMeasShoulders by onboardingViewModel.measurementShoulders.collectAsState()
            val onboardingMeasSleeve by onboardingViewModel.measurementSleeve.collectAsState()
            val onboardingSetupPw by onboardingViewModel.setupPassword.collectAsState()
            val onboardingSetupPwConfirm by onboardingViewModel.setupPasswordConfirm.collectAsState()
            val onboardingLoading by onboardingViewModel.isLoading.collectAsState()
            val onboardingSubmitting by onboardingViewModel.isSubmitting.collectAsState()
            val facebookOk = LoginViewModel.isFacebookConfigured()
            val googleOk = LoginViewModel.isGoogleConfigured()
            val profileSetupBlocksShellChrome =
                OnboardingFlowProgress.blocksShellPromosAndTours(needsOnboarding) ||
                    (isAuthenticated && onboardingStep != OnboardingStep.Completed)
            val onboardingProgressStep =
                OnboardingFlowProgress.progressStep(onboardingStep)
            val onboardingProgressTotal = OnboardingFlowProgress.TOTAL_STEPS

            ProvideAppLocale {
            val contextForTheme = LocalContext.current
            val themeRev by AppThemePreference.revision.collectAsState()
            val themeMode = remember(themeRev) { AppThemePreference.readMode(contextForTheme) }
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                AppThemePreference.Mode.LIGHT -> false
                AppThemePreference.Mode.DARK -> true
                AppThemePreference.Mode.SYSTEM -> systemDark
            }
            val lightAppearance = when (themeMode) {
                AppThemePreference.Mode.LIGHT -> FashLightAppearance.PureWhite
                else -> FashLightAppearance.Editorial
            }
            FashTheme(darkTheme = useDarkTheme, lightAppearance = lightAppearance) {
                var splashFinished by rememberSaveable { mutableStateOf(false) }
                var splashStartMs by rememberSaveable { mutableStateOf(0L) }
                LaunchedEffect(Unit) {
                    if (splashFinished) return@LaunchedEffect
                    val now = SystemClock.elapsedRealtime()
                    val start = if (splashStartMs == 0L) now else splashStartMs
                    if (splashStartMs == 0L) splashStartMs = start
                    val elapsed = now - start
                    delay((SPLASH_DISPLAY_MS - elapsed).coerceAtLeast(0L))
                    val hasSession = withContext(Dispatchers.IO) {
                        authManager.sessionStore.read() != null
                    }
                    if (hasSession) {
                        withContext(Dispatchers.IO) {
                            authManager.validateOrClearSession()
                        }
                    }
                    splashFinished = true
                }

                // Connect / disconnect the realtime WebSocket on auth state changes
                val realtimeManager = (application as FashApplication).realtimeManager
                val fashApp = application as FashApplication
                val notificationSnackbarContext = LocalContext.current
                val dialogMessage by fashApp.uiDialog.current.collectAsState()
                /** Hoisted so [FashGlobalDialogHost] can reserve bottom inset for chat composer vs main nav. */
                var selectedConversationId by rememberSaveable { mutableStateOf<String?>(null) }
                /** Hoisted for [FashSnackbarHost] — main bottom nav vs chat composer vs fullscreen overlays. */
                var snackbarBottomChromeInset by remember { mutableStateOf(0.dp) }
                /** Blocking center interstitial after home (welcome, KYC, rating, …). */
                var activePromoCampaign by remember { mutableStateOf<AppPromoCampaign?>(null) }
                var pendingPromoMainTab by remember { mutableIntStateOf(-1) }
                var pendingPromoOpenOrders by remember { mutableStateOf(false) }
                fun presentAdminPromoIfEligible(promo: AppPromoCampaign) {
                    AppPromoPendingQueue.enqueue(promo)
                    if (
                        splashFinished &&
                        isAuthenticated &&
                        !profileSetupBlocksShellChrome &&
                        selectedConversationId == null
                    ) {
                        activePromoCampaign = promo
                    }
                }
                val meetingReverifyRequired by profileViewModel.meetingSchedulingReverifyRequired.collectAsState()
                /** Guided main-shell tour after welcome (or immediately if welcome already dismissed). */
                var showFeatureTour by remember { mutableStateOf(false) }
                val notifPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { }

                LaunchedEffect(isAuthenticated) {
                    if (isAuthenticated) {
                        realtimeManager.connect()
                        pendingPaymentViewModel.startMonitoring()
                    } else {
                        realtimeManager.disconnect()
                        pendingPaymentViewModel.clearForLogout()
                        profileViewModel.clearCachedProfile()
                        homeViewModel.clearCachesForSignedOutUser()
                        homeDeliveringViewModel.clearCachesForSignedOutUser()
                        exploreViewModel.clearCachesForSignedOutUser()
                        chatViewModel.clearCachesForSignedOutUser()
                        chatDetailViewModel.clearCachesForSignedOutUser()
                        ordersViewModel.clearCachesForSignedOutUser()
                        orderDetailViewModel.clearCachesForSignedOutUser()
                        notificationsViewModel.clearCachesForSignedOutUser()
                        featuredSellersViewModel.clearCachesForSignedOutUser()
                        addressBookViewModel.clearCachesForSignedOutUser()
                        postViewModel.clearCachesForSignedOutUser()
                        productDetailViewModel.clearCachesForSignedOutUser()
                        followConnectionsViewModel.clearCachesForSignedOutUser()
                        needsOnboarding = null
                        fashApp.resetSetupGateRecheckGeneration()
                        fashApp.dismissInAppNotification()
                        setupGateAttempt = 0
                        setupGateFetchFailed = false
                        selectedConversationId = null
                        snackbarBottomChromeInset = 0.dp
                        activePromoCampaign = null
                        showFeatureTour = false
                    }
                }

                // Profile VM is activity-scoped: reconcile or reload after splash validation / login so the tab
                // never shows the previous user's profile or listings.
                LaunchedEffect(splashFinished, isAuthenticated) {
                    if (!splashFinished || !isAuthenticated) return@LaunchedEffect
                    profileViewModel.onAuthenticatedSessionReady()
                }

                /** After splash + authenticated session, reload tab feeds and inbox (cold start + account switch). */
                LaunchedEffect(splashFinished, isAuthenticated) {
                    if (!splashFinished || !isAuthenticated) return@LaunchedEffect
                    homeViewModel.refresh()
                    exploreViewModel.refresh()
                    notificationsViewModel.refreshUnreadSummary()
                    chatViewModel.loadConversations()
                }

                // Profile setup (loading gate or onboarding screens) blocks welcome promo + feature tour.
                LaunchedEffect(profileSetupBlocksShellChrome) {
                    if (profileSetupBlocksShellChrome) {
                        activePromoCampaign = null
                        showFeatureTour = false
                        fashApp.uiDialog.dismiss()
                    }
                }

                // Resolve app-open promo only once the setup gate has settled on main shell.
                LaunchedEffect(splashFinished, isAuthenticated, needsOnboarding) {
                    if (!splashFinished || !isAuthenticated) return@LaunchedEffect
                    if (profileSetupBlocksShellChrome) return@LaunchedEffect
                    if (selectedConversationId != null) return@LaunchedEffect

                    delay(550)
                    val appCtx = notificationSnackbarContext.applicationContext
                    val openCount = withContext(Dispatchers.IO) {
                        AppPromoCampaignStore.incrementAppOpenCount(appCtx)
                    }
                    withContext(Dispatchers.IO) {
                        fashApp.appPromoInterstitialRepository.fetchActiveCampaigns()
                            .getOrNull()
                            ?.filter { it.scheduleType == "on_app_open" || it.scheduleType == null }
                            ?.forEach { AppPromoPendingQueue.enqueue(it) }
                    }
                    val gate = AppPromoGateContext(
                        splashFinished = splashFinished,
                        isAuthenticated = isAuthenticated,
                        needsOnboarding = false,
                        blockPromoBecauseOtherUi = selectedConversationId != null,
                        meetingKycReverifyRequired = meetingReverifyRequired,
                        identityVerifyUrlAvailable = AppEnvironment.identityReverifyUrl.isNotBlank(),
                        sellerPackagePromoEnabled = AppEnvironment.isDev,
                        appOpenCount = openCount,
                    )
                    activePromoCampaign = withContext(Dispatchers.IO) {
                        AppPromoPendingQueue.pollHighest()?.let { remote ->
                            if (!AppPromoCampaignStore.isDismissed(appCtx, remote)) return@withContext remote
                        }
                        AppPromoCampaignResolver.resolve(gate, appCtx)
                    }
                }

                LaunchedEffect(selectedConversationId, needsOnboarding, splashFinished, isAuthenticated) {
                    if (selectedConversationId != null) {
                        activePromoCampaign = null
                        return@LaunchedEffect
                    }
                    if (!splashFinished || !isAuthenticated || profileSetupBlocksShellChrome) return@LaunchedEffect
                    val appCtx = notificationSnackbarContext.applicationContext
                    AppPromoPendingQueue.peekHighest()?.let { remote ->
                        if (!AppPromoCampaignStore.isDismissed(appCtx, remote)) {
                            activePromoCampaign = remote
                        }
                    }
                }

                LaunchedEffect(activePromoCampaign, splashFinished, isAuthenticated, needsOnboarding) {
                    if (!splashFinished || !isAuthenticated || profileSetupBlocksShellChrome) {
                        showFeatureTour = false
                        return@LaunchedEffect
                    }
                    if (activePromoCampaign != null) {
                        showFeatureTour = false
                        return@LaunchedEffect
                    }
                    delay(400)
                    val appCtx = notificationSnackbarContext.applicationContext
                    showFeatureTour = withContext(Dispatchers.IO) {
                        !AppFeatureTourStore.isCompletedForCurrentVersion(appCtx)
                    }
                }

                LaunchedEffect(isAuthenticated) {
                    if (!isAuthenticated) return@LaunchedEffect
                    if (Build.VERSION.SDK_INT >= 33) {
                        val granted = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // After splash: validateOrClearSession() has refreshed the access token — avoids FCM 401 from stale JWT.
                LaunchedEffect(splashFinished, isAuthenticated) {
                    if (!splashFinished || !isAuthenticated) return@LaunchedEffect
                    withContext(Dispatchers.IO) {
                        fashApp.fcmTokenRegistrar.registerCurrentTokenIfSession()
                    }
                }

                /** JSON application ping for Redis presence (integration-android-fullstack.md §5). */
                LaunchedEffect(isAuthenticated) {
                    if (!isAuthenticated) return@LaunchedEffect
                    while (true) {
                        delay(30_000L)
                        if (realtimeManager.state.value == RealtimeManager.State.CONNECTED) {
                            realtimeManager.sendPing()
                        }
                    }
                }

                LaunchedEffect(splashFinished, isAuthenticated) {
                    if (!splashFinished || !isAuthenticated) return@LaunchedEffect
                    var prevUnread = notificationsViewModel.unreadCount.value
                    fashApp.inboxUnreadRefreshSignals.collect {
                        val before = prevUnread
                        notificationsViewModel.refreshUnreadSummary()
                        delay(120)
                        val after = notificationsViewModel.unreadCount.value
                        prevUnread = after
                        if (after > before && fashApp.inAppNotification.value == null) {
                            fashApp.showInAppNotificationFromRealtime(
                                title = notificationSnackbarContext.getString(R.string.notifications),
                                body = notificationSnackbarContext.getString(
                                    R.string.notification_new_arrival_snackbar,
                                ),
                                data = null,
                                userNotificationId = null,
                            )
                        }
                    }
                }

                LaunchedEffect(splashFinished, isAuthenticated, needsOnboarding) {
                    if (!splashFinished || !isAuthenticated) return@LaunchedEffect
                    realtimeManager.events.collect { event ->
                        when (event) {
                            is RealtimeEvent.InboxRefresh -> {
                                fashApp.requestInboxUnreadRefreshDebounced()
                            }
                            is RealtimeEvent.NotificationShow -> {
                                // Account-switch hint is FCM-only by design; never surface it in-app.
                                val pushData = event.data ?: emptyMap()
                                val isAccountSwitchHint =
                                    AccountSwitchDeepLinks.parseFromFcmData(pushData) != null
                                if (isAccountSwitchHint) return@collect
                                if (isAppPromoPushData(pushData)) {
                                    parseAppPromoFromPushData(
                                        data = pushData,
                                        fallbackTitle = event.title,
                                        fallbackBody = event.body,
                                    )?.let { promo ->
                                        presentAdminPromoIfEligible(promo)
                                    }
                                    return@collect
                                }
                                fashApp.showInAppNotificationFromRealtime(
                                    title = event.title,
                                    body = event.body,
                                    data = event.data,
                                    userNotificationId = event.userNotificationId,
                                )
                                fashApp.requestInboxUnreadRefreshDebounced()
                            }
                            is RealtimeEvent.AppPromoShow -> {
                                val promo = parseRemoteAppPromoPayload(event.campaignJson)?.toAppPromoCampaign()
                                    ?: return@collect
                                presentAdminPromoIfEligible(promo)
                            }
                            else -> Unit
                        }
                    }
                }

                LaunchedEffect(splashFinished, isAuthenticated, needsOnboarding) {
                    if (!splashFinished || !isAuthenticated) return@LaunchedEffect
                    fashApp.appPromoShowSignals.collect { promo ->
                        presentAdminPromoIfEligible(promo)
                    }
                }

                LaunchedEffect(splashFinished, isAuthenticated, setupGateAttempt) {
                    if (!splashFinished || !isAuthenticated) return@LaunchedEffect
                    setupGateFetchFailed = false
                    needsOnboarding = null
                    val userRepo = (this@MainActivity.application as FashApplication).userRepository
                    val gate = withContext(Dispatchers.IO) {
                        repeat(SETUP_STATUS_INITIAL_ATTEMPTS) { attempt ->
                            userRepo.getUserAccessStatus().fold(
                                onSuccess = { status ->
                                    return@withContext status to !status.canAccessHome
                                },
                                onFailure = { },
                            )
                            if (attempt < SETUP_STATUS_INITIAL_ATTEMPTS - 1) {
                                delay(SETUP_STATUS_INITIAL_RETRY_MS)
                            }
                        }
                        null
                    }
                    if (gate == null) {
                        setupGateFetchFailed = true
                        needsOnboarding = null
                    } else {
                        val (status, needOnboarding) = gate
                        if (needOnboarding) {
                            onboardingViewModel.applyInitialStepFromAccessStatus(status)
                        } else {
                            onboardingViewModel.markProfileSetupGateSkippedForSession()
                        }
                        needsOnboarding = needOnboarding
                    }
                }

                LaunchedEffect(splashFinished, isAuthenticated, setupGateRecheckGen) {
                    if (!splashFinished || !isAuthenticated) return@LaunchedEffect
                    if (setupGateRecheckGen == 0L) return@LaunchedEffect
                    val userRepo = (this@MainActivity.application as FashApplication).userRepository
                    withContext(Dispatchers.IO) {
                        userRepo.getUserAccessStatus()
                    }.fold(
                        onSuccess = { status ->
                            if (!status.canAccessHome) {
                                setupGateFetchFailed = false
                                onboardingViewModel.applyInitialStepFromAccessStatus(status)
                                needsOnboarding = true
                                profileViewModel.clearCachedProfile()
                                profileViewModel.onAuthenticatedSessionReady()
                            }
                        },
                        onFailure = { },
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (splashFinished) {
                        Box(Modifier.fillMaxSize()) {
                            when {
                                isLoggingOut -> FashWaitingScreen()
                                isAuthenticated && setupGateFetchFailed -> {
                                    SetupGateRetryScreen(
                                        onRetry = {
                                            setupGateFetchFailed = false
                                            setupGateAttempt++
                                        },
                                        onSignOut = clearLocalSessionAndSocial,
                                    )
                                }
                                isAuthenticated && needsOnboarding == null -> FashWaitingScreen()
                                isAuthenticated && needsOnboarding == true -> {
                                    LaunchedEffect(onboardingStep) {
                                        if (onboardingStep == OnboardingStep.AestheticTags) {
                                            onboardingViewModel.loadTags()
                                        }
                                    }
                                    LaunchedEffect(onboardingStep, email) {
                                        if (onboardingStep == OnboardingStep.UsernameOnboard) {
                                            onboardingViewModel.seedUsernameFromEmailIfEmpty(email)
                                        }
                                    }
                                    val userRepoOnboarding = remember {
                                        (application as FashApplication).userRepository
                                    }
                                    when (onboardingStep) {
                                        OnboardingStep.AestheticTags -> OnboardingScreen(
                                            tags = onboardingTags,
                                            selectedIds = onboardingSelected,
                                            isLoading = onboardingLoading,
                                            isSubmitting = onboardingSubmitting,
                                            progressStep = onboardingProgressStep,
                                            progressTotal = onboardingProgressTotal,
                                            onToggleSelection = onboardingViewModel::toggleSelection,
                                            onContinue = {
                                                onboardingViewModel.submitAestheticTagsPut {
                                                    mainScope.launch {
                                                        needsOnboarding = withContext(Dispatchers.IO) {
                                                            resolveShellNeedsOnboardingAfterStep(
                                                                userRepoOnboarding,
                                                                onboardingViewModel,
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            onSkip = {
                                                onboardingViewModel.skipAestheticTagsPersistLocal {
                                                    mainScope.launch {
                                                        needsOnboarding = withContext(Dispatchers.IO) {
                                                            resolveShellNeedsOnboardingAfterStep(
                                                                userRepoOnboarding,
                                                                onboardingViewModel,
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            onBack = clearLocalSessionAndSocial,
                                        )
                                        OnboardingStep.SizingReference -> {
                                            val canSizing = remember(
                                                onboardingReferenceSize,
                                            ) {
                                                onboardingViewModel.canSubmitSizing()
                                            }
                                            SizingReferenceScreen(
                                                referenceSize = onboardingReferenceSize,
                                                onReferenceSizeChange = onboardingViewModel::onReferenceSizeChange,
                                                measurementUnit = onboardingMeasurementUnit,
                                                onMeasurementUnitChange = onboardingViewModel::onMeasurementUnitChange,
                                                measurementHem = onboardingMeasHem,
                                                onMeasurementHemChange = onboardingViewModel::onMeasurementHemChange,
                                                measurementChest = onboardingMeasChest,
                                                onMeasurementChestChange = onboardingViewModel::onMeasurementChestChange,
                                                measurementLength = onboardingMeasLength,
                                                onMeasurementLengthChange = onboardingViewModel::onMeasurementLengthChange,
                                                measurementShoulders = onboardingMeasShoulders,
                                                onMeasurementShouldersChange = onboardingViewModel::onMeasurementShouldersChange,
                                                measurementSleeve = onboardingMeasSleeve,
                                                onMeasurementSleeveChange = onboardingViewModel::onMeasurementSleeveChange,
                                                canSubmit = canSizing,
                                                isSubmitting = onboardingSubmitting,
                                                progressStep = onboardingProgressStep,
                                                progressTotal = onboardingProgressTotal,
                                                onComplete = {
                                                    onboardingViewModel.submitSizingOnly {
                                                        mainScope.launch {
                                                            needsOnboarding = withContext(Dispatchers.IO) {
                                                                resolveShellNeedsOnboardingAfterStep(
                                                                    userRepoOnboarding,
                                                                    onboardingViewModel,
                                                                )
                                                            }
                                                        }
                                                    }
                                                },
                                                onSkip = {
                                                    onboardingViewModel.skipSizingPersistLocal {
                                                        mainScope.launch {
                                                            needsOnboarding = withContext(Dispatchers.IO) {
                                                                resolveShellNeedsOnboardingAfterStep(
                                                                    userRepoOnboarding,
                                                                    onboardingViewModel,
                                                                )
                                                            }
                                                        }
                                                    }
                                                },
                                                onBack = {
                                                    if (!onboardingViewModel.handleBack()) {
                                                        clearLocalSessionAndSocial()
                                                    }
                                                },
                                            )
                                        }
                                        OnboardingStep.UsernameOnboard -> {
                                            val canUsername = remember(onboardingUsername) {
                                                onboardingViewModel.canSubmitUsername()
                                            }
                                            UsernameOnboardScreen(
                                                username = onboardingUsername,
                                                onUsernameChange = onboardingViewModel::onUsernameChange,
                                                isUsernameValid = onboardingViewModel.isUsernameValid(),
                                                canSubmit = canUsername,
                                                isSubmitting = onboardingSubmitting,
                                                progressStep = onboardingProgressStep,
                                                progressTotal = onboardingProgressTotal,
                                                onComplete = {
                                                    onboardingViewModel.submitUsernameOnboard {
                                                        authManager.sessionStore.read()?.let { s ->
                                                            authManager.sessionStore.save(
                                                                s.copy(isNewUser = false),
                                                            )
                                                        }
                                                        mainScope.launch {
                                                            val repo =
                                                                (this@MainActivity.application as FashApplication).userRepository
                                                            needsOnboarding = withContext(Dispatchers.IO) {
                                                                resolveNeedsOnboardingAfterProfileSubmit(repo)
                                                            }
                                                        }
                                                    }
                                                },
                                                onBack = {
                                                    if (!onboardingViewModel.handleBack()) {
                                                        clearLocalSessionAndSocial()
                                                    }
                                                },
                                            )
                                        }
                                        OnboardingStep.SetupPassword -> {
                                            val canPw = remember(onboardingSetupPw, onboardingSetupPwConfirm) {
                                                onboardingViewModel.canSubmitSetupPassword()
                                            }
                                            SetupPasswordOnboardScreen(
                                                newPassword = onboardingSetupPw,
                                                confirmPassword = onboardingSetupPwConfirm,
                                                onNewPasswordChange = onboardingViewModel::onSetupPasswordChange,
                                                onConfirmPasswordChange = onboardingViewModel::onSetupPasswordConfirmChange,
                                                canSubmit = canPw,
                                                isSubmitting = onboardingSubmitting,
                                                progressStep = onboardingProgressStep,
                                                progressTotal = onboardingProgressTotal,
                                                onComplete = {
                                                    onboardingViewModel.submitSetupPassword {
                                                        mainScope.launch {
                                                            needsOnboarding = withContext(Dispatchers.IO) {
                                                                resolveNeedsOnboardingAfterProfileSubmit(userRepoOnboarding)
                                                            }
                                                        }
                                                    }
                                                },
                                                onBack = {
                                                    if (!onboardingViewModel.handleBack()) {
                                                        clearLocalSessionAndSocial()
                                                    }
                                                },
                                            )
                                        }
                                        OnboardingStep.Completed -> {
                                            FashWaitingScreen()
                                        }
                                    }
                                }
                                isAuthenticated -> {
                                    var selectedListingId by rememberSaveable { mutableStateOf<String?>(null) }
                                    val pendingDeepLink by fashApp.pendingDeepLinkListingId.collectAsState()
                                    LaunchedEffect(pendingDeepLink) {
                                        val id = pendingDeepLink ?: return@LaunchedEffect
                                        selectedListingId = id
                                        fashApp.pendingDeepLinkListingId.value = null
                                    }
                                    var sellerShopUsername by rememberSaveable { mutableStateOf<String?>(null) }
                                    var sellerShopEntrySource by remember { mutableStateOf(SellerShopEntrySource.None) }
                                    /** Snapshot for restoring the screen under seller shop on back. */
                                    var sellerShopRestoreContext by remember {
                                        mutableStateOf(SellerShopRestoreContext())
                                    }
                                    /** True briefly after closing seller shop to block PDP from applying Explore filters (pointer replay). */
                                    var suppressPdpExploreNav by remember { mutableStateOf(false) }
                                    var editListingId by rememberSaveable { mutableStateOf<String?>(null) }
                                    var showEditProfile by rememberSaveable { mutableStateOf(false) }
                                    var selectedConversationItem by remember { mutableStateOf<ConversationItem?>(null) }
                                    var selectedCheckoutListingId by rememberSaveable { mutableStateOf<String?>(null) }
                                    var selectedCheckoutOfferPrice by rememberSaveable { mutableStateOf(0L) }
                                    var checkoutExistingOrderId by rememberSaveable { mutableStateOf<String?>(null) }
                                    var selectedOrderId by rememberSaveable { mutableStateOf<String?>(null) }
                                    /** Order detail as a medium-height sheet over chat (keeps conversation open). */
                                    var chatOrderDetailOverlayId by rememberSaveable { mutableStateOf<String?>(null) }
                                    var chatShipFlowArgs by remember { mutableStateOf<ChatShipFlowArgs?>(null) }
                                    var orderIdPendingCancel by remember { mutableStateOf<String?>(null) }
                                    var addressFlowOrderId by rememberSaveable { mutableStateOf<String?>(null) }
                                    var showShippingAddressList by rememberSaveable { mutableStateOf(false) }
                                    var showAddAddressScreen by rememberSaveable { mutableStateOf(false) }
                                    var addAddressOpenedFromList by rememberSaveable { mutableStateOf(false) }
                                    var showOrdersScreen by rememberSaveable { mutableStateOf(false) }
                                    var showHomeDeliveringScreen by rememberSaveable { mutableStateOf(false) }
                                    var showFollowConnections by rememberSaveable { mutableStateOf(false) }
                                    var followConnectionsInitialTab by rememberSaveable { mutableIntStateOf(0) }
                                    var showFeaturedSellersAll by rememberSaveable { mutableStateOf(false) }
                                    var showSellerPackagesScreen by rememberSaveable { mutableStateOf(false) }
                                    var showInviteFriendsScreen by rememberSaveable { mutableStateOf(false) }
                                    var sellerPackageCheckout by remember { mutableStateOf<SellerProductPackage?>(null) }
                                    val pendingInviteFriends by fashApp.pendingOpenInviteFriends.collectAsState()
                                    LaunchedEffect(pendingInviteFriends) {
                                        if (!pendingInviteFriends) return@LaunchedEffect
                                        showInviteFriendsScreen = true
                                        fashApp.pendingOpenInviteFriends.value = false
                                    }
                                    var selectedTab by rememberSaveable { mutableIntStateOf(MainTab.Home.ordinal) }
                                    LaunchedEffect(pendingPromoMainTab, pendingPromoOpenOrders) {
                                        if (pendingPromoMainTab >= 0) {
                                            selectedTab = pendingPromoMainTab
                                            pendingPromoMainTab = -1
                                        }
                                        if (pendingPromoOpenOrders) {
                                            showOrdersScreen = true
                                            pendingPromoOpenOrders = false
                                        }
                                    }
                                    val pendingInboxOpenId by fashApp.pendingInboxNotificationId.collectAsState()
                                    val inboxOpenGen by fashApp.inboxOpenRequestGeneration.collectAsState()
                                    val scope = rememberCoroutineScope()
                                    /**
                                     * Closes the seller storefront overlay. Does **not** clear [selectedListingId];
                                     * if the user opened the shop from PDP, the listing detail stays on screen (back behavior).
                                     */
                                    val dismissSellerShopOverlay: () -> Unit = {
                                        val entry = sellerShopEntrySource
                                        val restore = sellerShopRestoreContext
                                        suppressPdpExploreNav = true
                                        sellerShopUsername = null
                                        sellerShopEntrySource = SellerShopEntrySource.None
                                        sellerShopRestoreContext = SellerShopRestoreContext()
                                        when (entry) {
                                            SellerShopEntrySource.Explore -> {
                                                selectedTab = MainTab.Explore.ordinal
                                                restore.exploreSection?.let { exploreViewModel.setPrimarySection(it) }
                                            }
                                            SellerShopEntrySource.Chat -> {
                                                restore.chatConversationId?.let { selectedConversationId = it }
                                            }
                                            SellerShopEntrySource.FollowConnections -> {
                                                followConnectionsInitialTab = restore.followConnectionsTab
                                                showFollowConnections = true
                                            }
                                            SellerShopEntrySource.FeaturedSellers -> {
                                                showFeaturedSellersAll = true
                                            }
                                            SellerShopEntrySource.Orders -> {
                                                showOrdersScreen = true
                                            }
                                            SellerShopEntrySource.HomeDelivering -> {
                                                showHomeDeliveringScreen = true
                                            }
                                            SellerShopEntrySource.OrderDetail -> {
                                                restore.orderId?.let { selectedOrderId = it }
                                            }
                                            else -> Unit
                                        }
                                        scope.launch {
                                            delay(100)
                                            suppressPdpExploreNav = false
                                        }
                                    }
                                    /**
                                     * Leave seller shop and show Explore. Must clear [selectedListingId] first: PDP is
                                     * composed above main nav but below the seller overlay, so dismissing only the shop
                                     * would otherwise reveal the previous listing screen instead of Explore.
                                     */
                                    val navigateToExploreFromSellerShop: () -> Unit = {
                                        selectedListingId = null
                                        dismissSellerShopOverlay()
                                        selectedTab = MainTab.Explore.ordinal
                                    }
                                    val context = LocalContext.current
                                    LaunchedEffect(Unit) {
                                        editListingViewModel.events.collect { msg ->
                                            enqueueSnackbarSerial {
                                                showSnackbar(msg)
                                                if (msg == context.getString(R.string.edit_listing_saved) ||
                                                    msg == context.getString(R.string.edit_listing_deleted)
                                                ) {
                                                    editListingId = null
                                                    homeViewModel.loadFeed()
                                                    exploreViewModel.loadAll()
                                                    profileViewModel.loadProfile()
                                                }
                                            }
                                        }
                                    }
                                    LaunchedEffect(showFollowConnections, followConnectionsInitialTab) {
                                        if (showFollowConnections) {
                                            followConnectionsViewModel.show(followConnectionsInitialTab)
                                        }
                                    }
                                    ReloadWhenVisible(showOrdersScreen, selectedOrderId) {
                                        if (showOrdersScreen && selectedOrderId == null) {
                                            ordersViewModel.refreshOrders()
                                        }
                                    }
                                    ReloadWhenVisible(showHomeDeliveringScreen, selectedOrderId) {
                                        if (showHomeDeliveringScreen && selectedOrderId == null) {
                                            homeDeliveringViewModel.refresh(AppEnvironment.shippingEnabled)
                                        }
                                    }
                                    ReloadWhenVisible(showFeaturedSellersAll, sellerShopUsername, selectedListingId) {
                                        if (showFeaturedSellersAll && sellerShopUsername == null && selectedListingId == null) {
                                            featuredSellersViewModel.refresh()
                                        }
                                    }
                                    ReloadWhenVisible(showEditProfile) {
                                        if (showEditProfile) {
                                            editProfileViewModel.loadProfile()
                                        }
                                    }
                                    ReloadWhenVisible(sellerShopUsername != null, sellerShopUsername) {
                                        sellerShopUsername?.let { sellerProfileViewModel.loadForSeller(it) }
                                    }
                                    ReloadWhenVisible(
                                        showShippingAddressList && !showAddAddressScreen,
                                        showShippingAddressList,
                                        showAddAddressScreen,
                                    ) {
                                        if (showShippingAddressList && !showAddAddressScreen) {
                                            addressBookViewModel.refresh()
                                        }
                                    }
                                    val orderRepository = remember {
                                        (context.applicationContext as FashApplication).orderRepository
                                    }
                                    val chatUnreadCount by chatViewModel.unreadBadgeCount.collectAsState()
                                    val remotePromo by promoSlidesViewModel.remoteSlides.collectAsState()
                                    val profileForInvite by profileViewModel.profile.collectAsState()
                                    val localeRev by AppLocale.localeRevisionFlow.collectAsState()
                                    LaunchedEffect(localeRev) {
                                        promoSlidesViewModel.refresh()
                                    }
                                    ReloadWhenVisible(
                                        !showOrdersScreen &&
                                            !showHomeDeliveringScreen &&
                                            !showSellerPackagesScreen &&
                                            !showInviteFriendsScreen &&
                                            sellerPackageCheckout == null &&
                                            sellerShopUsername == null &&
                                            !showEditProfile &&
                                            !showFeaturedSellersAll,
                                        selectedTab,
                                    ) {
                                        promoSlidesViewModel.refresh()
                                    }
                                    val scheme = MaterialTheme.colorScheme
                                    val mappedPromoSlides = remember(remotePromo, scheme) {
                                        remotePromo.map { it.toFashPromoSlideDef(scheme) }
                                    }
                                    val handlePromoClick: (FashPromoSlideDef, Int) -> Unit = { slide, _ ->
                                        val nav = slide.navigation
                                        val t = nav?.type?.trim()?.lowercase().orEmpty()
                                        when (t) {
                                            "", "none" -> Unit
                                            "in_app_explore" -> selectedTab = MainTab.Explore.ordinal
                                            "in_app_orders" -> showOrdersScreen = true
                                            "in_app_chat" -> selectedTab = MainTab.Chat.ordinal
                                            "in_app_product_packages" -> {
                                                sellerPackageCheckout = null
                                                showSellerPackagesScreen = true
                                            }
                                            "in_app_invite_friends" -> {
                                                showInviteFriendsScreen = true
                                            }
                                            "external_url" -> {
                                                val url = nav?.payload?.trim().orEmpty()
                                                if (url.isNotEmpty()) {
                                                    runCatching {
                                                        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
                                                    }
                                                }
                                            }
                                            "deeplink" -> {
                                                val u = nav?.payload?.trim().orEmpty()
                                                if (u.isNotEmpty()) {
                                                    runCatching {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                                                    }
                                                }
                                            }
                                            else -> Unit
                                        }
                                    }
                                    val chatConversations by chatViewModel.conversations.collectAsState()
                                    val chatDisplayGroups by chatViewModel.displayGroups.collectAsState()
                                    val otherInboxUnread = remember(
                                        selectedConversationId,
                                        chatUnreadCount,
                                        chatConversations,
                                        chatDisplayGroups,
                                    ) {
                                        val cid = selectedConversationId
                                            ?: return@remember 0
                                        chatViewModel.unreadCountExcludingConversation(cid)
                                    }
                                    val chatOrderId by chatDetailViewModel.orderId.collectAsState()
                                    val snackbarChromeInsetForMainApp = remember(
                                        selectedConversationId,
                                        selectedListingId,
                                        selectedOrderId,
                                        selectedCheckoutListingId,
                                        chatShipFlowArgs,
                                        sellerShopUsername,
                                        editListingId,
                                        showEditProfile,
                                        showShippingAddressList,
                                        showAddAddressScreen,
                                        showOrdersScreen,
                                        showHomeDeliveringScreen,
                                        showFollowConnections,
                                        showFeaturedSellersAll,
                                        showInviteFriendsScreen,
                                    ) {
                                        val fullscreenOverlay =
                                            selectedListingId != null ||
                                                selectedOrderId != null ||
                                                selectedCheckoutListingId != null ||
                                                chatShipFlowArgs != null ||
                                                sellerShopUsername != null ||
                                                editListingId != null ||
                                                showEditProfile ||
                                                showShippingAddressList ||
                                                showAddAddressScreen ||
                                                showOrdersScreen ||
                                                showHomeDeliveringScreen ||
                                                showSellerPackagesScreen ||
                                                sellerPackageCheckout != null ||
                                                showFollowConnections ||
                                                showFeaturedSellersAll ||
                                                showInviteFriendsScreen
                                        when {
                                            selectedConversationId != null -> ChatComposerBarOverlayInset
                                            fullscreenOverlay -> 0.dp
                                            else -> MainNavBottomBarOverlayInset
                                        }
                                    }
                                    SideEffect {
                                        snackbarBottomChromeInset = snackbarChromeInsetForMainApp
                                    }
                                    val pendingPaymentBanner by pendingPaymentViewModel.banner.collectAsState()
                                    val pendingPaymentSliderRegistry = remember { PendingPaymentSliderRegistry() }
                                    var rootLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
                                    val pendingPaymentBannerExpanded =
                                        pendingPaymentBanner != null &&
                                            selectedOrderId == null &&
                                            selectedCheckoutListingId == null &&
                                            chatOrderDetailOverlayId == null
                                    val anchorTopPx = remember(pendingPaymentSliderRegistry.sliders, rootLayoutCoordinates) {
                                        val coords = rootLayoutCoordinates ?: return@remember null
                                        val h = coords.size.height.toFloat()
                                        if (h <= 0f) null else pendingPaymentSliderRegistry.topmostAnchorEligibleSliderTop(h)
                                    }
                                    val usePendingPaymentAnchorPlacement =
                                        anchorTopPx != null && pendingPaymentBannerExpanded
                                    LaunchedEffect(Unit) {
                                        pendingPaymentViewModel.events.collect { ev ->
                                            enqueueSnackbarSerial {
                                                when (ev) {
                                                    is PendingPaymentEvent.Expired -> {
                                                        showSnackbar(
                                                            message = context.getString(R.string.pending_payment_expired_snackbar),
                                                            duration = SnackbarDuration.Long,
                                                        )
                                                    }
                                                    PendingPaymentEvent.CancelSuccess -> {
                                                        showSnackbar(context.getString(R.string.order_cancel_success))
                                                    }
                                                    is PendingPaymentEvent.CancelFailed -> {
                                                        showSnackbar(ev.message)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .onGloballyPositioned { rootLayoutCoordinates = it },
                                    ) {
                                        CompositionLocalProvider(
                                            LocalPendingPaymentRootCoordinates provides rootLayoutCoordinates,
                                            LocalPendingPaymentSliderRegistry provides pendingPaymentSliderRegistry,
                                        ) {
                                            Box(Modifier.fillMaxSize()) {
                                            MainNavScreen(
                                            onLogout = loginViewModel::logout,
                                            onLogoutAll = loginViewModel::logoutAll,
                                            isLoggingOut = isLoggingOut,
                                            homeViewModel = homeViewModel,
                                            exploreViewModel = exploreViewModel,
                                            postViewModel = postViewModel,
                                            addressBookViewModel = addressBookViewModel,
                                            profileViewModel = profileViewModel,
                                            chatViewModel = chatViewModel,
                                            changePasswordViewModel = changePasswordViewModel,
                                            notificationsViewModel = notificationsViewModel,
                                            pendingInboxNotificationIdToOpen = pendingInboxOpenId,
                                            onConsumePendingInboxNotificationId = {
                                                fashApp.pendingInboxNotificationId.value = null
                                            },
                                            inboxOpenRequestGeneration = inboxOpenGen,
                                            onOpenOrderFromNotification = { oid ->
                                                // Close chat / overlays so order detail is not covered by ChatDetailScreen.
                                                chatOrderDetailOverlayId = null
                                                selectedConversationId = null
                                                selectedConversationItem = null
                                                chatViewModel.loadConversations()
                                                chatViewModel.refreshUnreadCount()
                                                selectedListingId = null
                                                selectedOrderId = oid
                                            },
                                            onOpenListingFromNotification = { lid, sellerId ->
                                                // Chat (and other overlays) are composed after PDP in this Box — clear
                                                // them or "View listing" appears to do nothing / wrong screen.
                                                chatOrderDetailOverlayId = null
                                                selectedConversationId = null
                                                selectedConversationItem = null
                                                chatViewModel.loadConversations()
                                                chatViewModel.refreshUnreadCount()
                                                selectedOrderId = null
                                                val myId =
                                                    authManager.sessionStore.read()?.userId?.trim().orEmpty()
                                                if (!sellerId.isNullOrBlank() && sellerId == myId) {
                                                    selectedListingId = null
                                                    editListingId = lid
                                                } else {
                                                    selectedListingId = lid
                                                }
                                            },
                                            onNavigateToChatConversation = { conversationId ->
                                                selectedListingId = null
                                                selectedOrderId = null
                                                editListingId = null
                                                chatOrderDetailOverlayId = null
                                                selectedConversationId = conversationId.trim()
                                                selectedTab = MainTab.Chat.ordinal
                                            },
                                            snackbarHostState = snackbarHostState,
                                            chatUnreadCount = chatUnreadCount,
                                            onListingClick = { lid, sellerId ->
                                                val myId = authManager.sessionStore.read()?.userId?.trim().orEmpty()
                                                if (!sellerId.isNullOrBlank() && sellerId == myId) {
                                                    selectedListingId = null
                                                    editListingId = lid
                                                } else {
                                                    selectedListingId = lid
                                                }
                                            },
                                            onEditProfile = { showEditProfile = true },
                                            onShippingAddressesClick = {
                                                addressFlowOrderId = null
                                                showShippingAddressList = true
                                            },
                                            onInviteFriendsClick = { showInviteFriendsScreen = true },
                                            onOrdersClick = { showOrdersScreen = true },
                                            onHomeDeliveringJourneyClick = { showHomeDeliveringScreen = true },
                                            onOpenFollowConnections = { tab ->
                                                followConnectionsInitialTab = tab
                                                showFollowConnections = true
                                            },
                                            onOpenFeaturedSellersAll = { showFeaturedSellersAll = true },
                                            onFeaturedSellerClick = { seller ->
                                                val u = seller.username.trim()
                                                if (u.isNotEmpty()) {
                                                    sellerShopEntrySource = SellerShopEntrySource.Explore
                                                    sellerShopRestoreContext = SellerShopRestoreContext(
                                                        exploreSection = exploreViewModel.primarySection.value,
                                                    )
                                                    sellerShopUsername = u
                                                }
                                            },
                                            onConversationClick = { item ->
                                                chatOrderDetailOverlayId = null
                                                selectedConversationItem = item
                                                chatDetailViewModel.loadFromItem(item)
                                                selectedConversationId = item.conversationId
                                            },
                                            onNavigateToExploreFromProfile = { cat, brand, aes, q, countryId, countryIso2 ->
                                                exploreViewModel.openExploreFromProfileFilter(
                                                    categoryId = cat,
                                                    brandId = brand,
                                                    aestheticTagId = aes,
                                                    searchQuery = q,
                                                    countryId = countryId,
                                                    countryIso2 = countryIso2,
                                                )
                                                selectedListingId = null
                                                selectedTab = MainTab.Explore.ordinal
                                                sellerShopUsername = null
                                                sellerShopEntrySource = SellerShopEntrySource.None
                                                sellerShopRestoreContext = SellerShopRestoreContext()
                                            },
                                            promoSlides = mappedPromoSlides,
                                            onPromoSlideClick = handlePromoClick,
                                            selectedTab = selectedTab,
                                            onTabChange = { selectedTab = it },
                                            featureTourActive = showFeatureTour,
                                            onFeatureTourFinished = { showFeatureTour = false },
                                            )
                                        PendingPaymentBanner(
                                            data = pendingPaymentBanner,
                                            expanded = pendingPaymentBannerExpanded,
                                            anchorTopInRootPx = if (usePendingPaymentAnchorPlacement) anchorTopPx else null,
                                            onPayClick = { row ->
                                                selectedCheckoutListingId = row.listingId
                                                selectedCheckoutOfferPrice = row.amountVnd
                                                checkoutExistingOrderId = row.orderId
                                            },
                                            onCancelOrder = { row -> orderIdPendingCancel = row.orderId },
                                            onDeadlineElapsed = pendingPaymentViewModel::onDeadlineElapsed,
                                            modifier = if (usePendingPaymentAnchorPlacement) {
                                                Modifier
                                                    .align(Alignment.TopStart)
                                                    .fillMaxWidth()
                                            } else {
                                                Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                                    .navigationBarsPadding()
                                                    // Above bottom nav only; sticky promo (when shown) sits in content — do not
                                                    // reserve FashStickyPromoDockHeight or the banner floats too high.
                                                    .padding(bottom = 80.dp)
                                            },
                                        )
                                        com.pc.fash_android_mobile.ui.orders.OrderCancelFlowHost(
                                            orderId = orderIdPendingCancel,
                                            onDismiss = { orderIdPendingCancel = null },
                                            onSuccess = { oid ->
                                                orderIdPendingCancel = null
                                                pendingPaymentViewModel.onCancelFlowComplete()
                                                chatShipFlowArgs = null
                                                orderDetailViewModel.onOrderCancelFlowComplete(oid)
                                                selectedConversationId?.let {
                                                    chatDetailViewModel.loadConversation(it)
                                                }
                                            },
                                        )
                                        if (selectedListingId != null) {
                                            ProductDetailScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                listingId = selectedListingId!!,
                                                viewModel = productDetailViewModel,
                                                profileExploreNavigationEnabled = sellerShopUsername == null &&
                                                    !suppressPdpExploreNav,
                                                onBack = { selectedListingId = null },
                                                onChat = { listingId ->
                                                    scope.launch {
                                                        productDetailViewModel.setOpeningChat(true)
                                                        chatViewModel.startConversation(listingId).fold(
                                                            onSuccess = { convId ->
                                                                productDetailViewModel.setOpeningChat(false)
                                                                selectedListingId = null
                                                                selectedTab = MainTab.Chat.ordinal
                                                                selectedConversationItem = null
                                                                chatOrderDetailOverlayId = null
                                                                selectedConversationId = convId
                                                                chatViewModel.loadConversations()
                                                            },
                                                            onFailure = {
                                                                productDetailViewModel.setOpeningChat(false)
                                                                selectedListingId = null
                                                                selectedTab = MainTab.Chat.ordinal
                                                                enqueueSnackbarSerial {
                                                                    showSnackbar(
                                                                        it.message ?: getString(R.string.chat_load_error),
                                                                    )
                                                                }
                                                            },
                                                        )
                                                    }
                                                },
                                                onBuyNow = { listingId ->
                                                    scope.launch {
                                                        val price =
                                                            productDetailViewModel.detail.value?.priceVnd ?: 0L
                                                        if (price <= 0L) {
                                                            enqueueSnackbarSerial {
                                                                showSnackbar(getString(R.string.feed_action_error))
                                                            }
                                                            return@launch
                                                        }
                                                        val existing = withContext(Dispatchers.IO) {
                                                            productDetailViewModel.findBuyerActiveOrderForListing(
                                                                listingId,
                                                            )
                                                        }
                                                        fun openBuyNowShipFlow(orderId: String, amountVnd: Long) {
                                                            selectedListingId = null
                                                            sellerShopUsername = null
                                                            sellerShopRestoreContext = SellerShopRestoreContext()
                                                            sellerShopEntrySource = SellerShopEntrySource.None
                                                            chatShipFlowArgs = ChatShipFlowArgs(
                                                                orderId = orderId,
                                                                listingId = listingId,
                                                                agreedAmountVnd = amountVnd,
                                                                source = ShipFlowSource.BuyNow,
                                                            )
                                                            chatDetailViewModel.syncLinkedOrderFromBuyNow(
                                                                listingId,
                                                                orderId,
                                                            )
                                                            pendingPaymentViewModel.refresh()
                                                        }
                                                        if (existing != null) {
                                                            openBuyNowShipFlow(
                                                                existing.orderId,
                                                                existing.amountVnd.takeIf { it > 0 } ?: price,
                                                            )
                                                            return@launch
                                                        }
                                                        val result = withContext(Dispatchers.IO) {
                                                            orderRepository.createOrder(listingId, price)
                                                        }
                                                        result.fold(
                                                            onSuccess = { oid ->
                                                                openBuyNowShipFlow(oid, price)
                                                            },
                                                            onFailure = {
                                                                enqueueSnackbarSerial {
                                                                    showSnackbar(
                                                                        it.message
                                                                            ?: getString(R.string.feed_action_error),
                                                                    )
                                                                }
                                                            },
                                                        )
                                                    }
                                                },
                                                onShare = { lid, title ->
                                                    val web = AppEnvironment.listingShareUrl(lid)
                                                    val fashUri = ListingDeepLinks.fashListingUri(lid).toString()
                                                    val text = getString(
                                                        R.string.share_listing_text,
                                                        title.ifBlank { getString(R.string.product_detail_title) },
                                                        web,
                                                        fashUri,
                                                    )
                                                    val send = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_listing_subject))
                                                        putExtra(Intent.EXTRA_TEXT, text)
                                                    }
                                                    startActivity(
                                                        Intent.createChooser(send, getString(R.string.share)),
                                                    )
                                                },
                                                onListingClick = { lid, sellerId ->
                                                    val myId = authManager.sessionStore.read()?.userId?.trim().orEmpty()
                                                    if (!sellerId.isNullOrBlank() && sellerId == myId) {
                                                        selectedListingId = null
                                                        editListingId = lid
                                                    } else {
                                                        selectedListingId = lid
                                                    }
                                                },
                                                onVisitSellerShop = { username ->
                                                    sellerShopEntrySource = SellerShopEntrySource.ProductDetail
                                                    sellerShopRestoreContext = SellerShopRestoreContext()
                                                    sellerShopUsername = username
                                                },
                                                onNavigateToExploreFromProfile = { cat, brand, aes, q, countryId, countryIso2 ->
                                                    exploreViewModel.openExploreFromProfileFilter(
                                                        categoryId = cat,
                                                        brandId = brand,
                                                        aestheticTagId = aes,
                                                        searchQuery = q,
                                                        countryId = countryId,
                                                        countryIso2 = countryIso2,
                                                    )
                                                    selectedListingId = null
                                                    selectedTab = MainTab.Explore.ordinal
                                                    sellerShopRestoreContext = SellerShopRestoreContext()
                                                },
                                            )
                                        }
                                        if (sellerShopUsername != null &&
                                            chatShipFlowArgs == null &&
                                            selectedCheckoutListingId == null
                                        ) {
                                            SellerProfileScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                viewModel = sellerProfileViewModel,
                                                sellerUsername = sellerShopUsername!!,
                                                onBack = dismissSellerShopOverlay,
                                                onListingClick = { lid, sellerId ->
                                                    val myId =
                                                        authManager.sessionStore.read()?.userId?.trim().orEmpty()
                                                    if (!sellerId.isNullOrBlank() && sellerId == myId) {
                                                        dismissSellerShopOverlay()
                                                        editListingId = lid
                                                    } else {
                                                        dismissSellerShopOverlay()
                                                        selectedListingId = lid
                                                    }
                                                },
                                                onNavigateToExploreFromProfile = { cat, brand, aes, q, countryId, countryIso2 ->
                                                    exploreViewModel.openExploreFromProfileFilter(
                                                        categoryId = cat,
                                                        brandId = brand,
                                                        aestheticTagId = aes,
                                                        searchQuery = q,
                                                        countryId = countryId,
                                                        countryIso2 = countryIso2,
                                                    )
                                                    selectedListingId = null
                                                    selectedTab = MainTab.Explore.ordinal
                                                    sellerShopUsername = null
                                                    sellerShopEntrySource = SellerShopEntrySource.None
                                                    sellerShopRestoreContext = SellerShopRestoreContext()
                                                },
                                                onPromoSlideClick = handlePromoClick,
                                                promoSlides = mappedPromoSlides,
                                                onExploreClick = {
                                                    navigateToExploreFromSellerShop()
                                                },
                                            )
                                        }
                                        if (editListingId != null) {
                                            EditListingScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                listingId = editListingId!!,
                                                viewModel = editListingViewModel,
                                                onBack = { editListingId = null },
                                            )
                                        }
                                        if (showEditProfile) {
                                            EditProfileScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                viewModel = editProfileViewModel,
                                                onBack = {
                                                    showEditProfile = false
                                                },
                                                onSaved = {
                                                    showEditProfile = false
                                                    profileViewModel.loadProfile()
                                                },
                                            )
                                        }
                                        if (selectedConversationId != null) {
                                            ChatDetailScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                conversationId = selectedConversationId!!,
                                                viewModel = chatDetailViewModel,
                                                otherInboxUnreadCount = otherInboxUnread,
                                                onBack = {
                                                    chatOrderDetailOverlayId = null
                                                    chatShipFlowArgs = null
                                                    selectedConversationId = null
                                                    selectedConversationItem = null
                                                    chatViewModel.loadConversations()
                                                    chatViewModel.refreshUnreadCount()
                                                },
                                                onProductClick = {
                                                    chatOrderDetailOverlayId = null
                                                    selectedConversationId = null
                                                    chatViewModel.loadConversations()
                                                    chatViewModel.refreshUnreadCount()
                                                    selectedListingId = it
                                                },
                                                onCheckout = { listingId, offerAmount ->
                                                    selectedCheckoutListingId = listingId
                                                    selectedCheckoutOfferPrice = offerAmount
                                                    checkoutExistingOrderId = chatOrderId
                                                },
                                                onPayNow = { orderId, _, _ ->
                                                    chatOrderDetailOverlayId = orderId
                                                },
                                                onStartShipFulfillment = { orderId, listingId, amountVnd ->
                                                    chatShipFlowArgs = ChatShipFlowArgs(
                                                        orderId = orderId,
                                                        listingId = listingId,
                                                        agreedAmountVnd = amountVnd,
                                                        source = ShipFlowSource.Chat,
                                                    )
                                                },
                                                onOrderDetails = { orderId ->
                                                    val oid = orderId.trim().takeIf { it.isNotEmpty() }
                                                    if (oid != null) {
                                                        chatOrderDetailOverlayId = null
                                                        selectedOrderId = oid
                                                    }
                                                },
                                                onOtherUserProfileClick = { username ->
                                                    val u = username.trim()
                                                    if (u.isNotEmpty()) {
                                                        chatOrderDetailOverlayId = null
                                                        sellerShopRestoreContext = SellerShopRestoreContext(
                                                            chatConversationId = selectedConversationId,
                                                        )
                                                        selectedConversationId = null
                                                        selectedConversationItem = null
                                                        chatViewModel.loadConversations()
                                                        chatViewModel.refreshUnreadCount()
                                                        sellerShopEntrySource = SellerShopEntrySource.Chat
                                                        sellerShopUsername = u
                                                    }
                                                },
                                                onOrdersClick = { showOrdersScreen = true },
                                                onSellerSuggestNewListing = {
                                                    selectedTab = MainTab.Post.ordinal
                                                },
                                                orderDetailOverlayOrderId = chatOrderDetailOverlayId,
                                                onDismissOrderDetailOverlay = { chatOrderDetailOverlayId = null },
                                                orderDetailViewModel = orderDetailViewModel,
                                                addressBookViewModel = addressBookViewModel,
                                                onOrderOverlayPayment = { listingId, amountVnd, existingOid ->
                                                    chatOrderDetailOverlayId = null
                                                    selectedCheckoutListingId = listingId
                                                    selectedCheckoutOfferPrice = amountVnd
                                                    checkoutExistingOrderId = existingOid
                                                },
                                                onOrderOverlayNavigateToChat = { conversationId ->
                                                    chatOrderDetailOverlayId = null
                                                    selectedConversationId = conversationId
                                                },
                                                onOrderOverlayOpenShippingList = {
                                                    addressFlowOrderId = chatOrderDetailOverlayId
                                                    showShippingAddressList = true
                                                },
                                                onOrderOverlayOpenAddShipping = {
                                                    addressFlowOrderId = chatOrderDetailOverlayId
                                                    showAddAddressScreen = true
                                                    addAddressOpenedFromList = false
                                                },
                                                onOrderOverlayOpenUserProfile = { username ->
                                                    val u = username.trim()
                                                    if (u.isNotEmpty()) {
                                                        chatOrderDetailOverlayId = null
                                                        sellerShopRestoreContext = SellerShopRestoreContext(
                                                            chatConversationId = selectedConversationId,
                                                        )
                                                        selectedConversationId = null
                                                        selectedConversationItem = null
                                                        chatViewModel.loadConversations()
                                                        chatViewModel.refreshUnreadCount()
                                                        sellerShopEntrySource = SellerShopEntrySource.Chat
                                                        sellerShopUsername = u
                                                    }
                                                },
                                                onOrderOverlayOpenListing = { listingId, sellerUserId ->
                                                    chatOrderDetailOverlayId = null
                                                    selectedConversationId = null
                                                    selectedConversationItem = null
                                                    chatViewModel.loadConversations()
                                                    chatViewModel.refreshUnreadCount()
                                                    val myId =
                                                        authManager.sessionStore.read()?.userId?.trim().orEmpty()
                                                    if (sellerUserId.isNotBlank() &&
                                                        sellerUserId.equals(myId, ignoreCase = true)
                                                    ) {
                                                        selectedListingId = null
                                                        editListingId = listingId
                                                    } else {
                                                        selectedListingId = listingId
                                                    }
                                                },
                                            )
                                        }
                                        selectedOrderId?.let { orderIdForDetail ->
                                            key(orderIdForDetail) {
                                                OrderDetailScreen(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(MaterialTheme.colorScheme.surface),
                                                    orderId = orderIdForDetail,
                                                    viewModel = orderDetailViewModel,
                                                    onBack = { selectedOrderId = null },
                                                    onNavigateToPayment = { listingId, amountVnd, existingOid ->
                                                        selectedCheckoutListingId = listingId
                                                        selectedCheckoutOfferPrice = amountVnd
                                                        checkoutExistingOrderId = existingOid
                                                    },
                                                    onNavigateToChat = { conversationId ->
                                                        selectedOrderId = null
                                                        selectedConversationId = conversationId
                                                    },
                                                    addressBookViewModel = addressBookViewModel,
                                                    onOpenShippingAddressList = {
                                                        addressFlowOrderId = selectedOrderId
                                                        showShippingAddressList = true
                                                    },
                                                    onOpenAddShippingAddress = {
                                                        addressFlowOrderId = selectedOrderId
                                                        showAddAddressScreen = true
                                                        addAddressOpenedFromList = false
                                                    },
                                                    onOpenUserProfile = { username ->
                                                        val u = username.trim()
                                                        if (u.isNotEmpty()) {
                                                            sellerShopRestoreContext = SellerShopRestoreContext(
                                                                orderId = selectedOrderId,
                                                            )
                                                            sellerShopEntrySource = SellerShopEntrySource.OrderDetail
                                                            sellerShopUsername = u
                                                        }
                                                    },
                                                    onOpenListing = { listingId, sellerUserId ->
                                                        selectedOrderId = null
                                                        val myId =
                                                            authManager.sessionStore.read()?.userId?.trim().orEmpty()
                                                        if (sellerUserId.isNotBlank() &&
                                                            sellerUserId.equals(myId, ignoreCase = true)
                                                        ) {
                                                            selectedListingId = null
                                                            editListingId = listingId
                                                        } else {
                                                            selectedListingId = listingId
                                                        }
                                                    },
                                                )
                                            }
                                        }
                                        if (showShippingAddressList && !showAddAddressScreen) {
                                            ShippingAddressListScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                orderId = addressFlowOrderId,
                                                viewModel = addressBookViewModel,
                                                onBack = {
                                                    showShippingAddressList = false
                                                    addressFlowOrderId = null
                                                },
                                                onAddNew = {
                                                    addAddressOpenedFromList = true
                                                    showAddAddressScreen = true
                                                },
                                                onConfirmed = {
                                                    val oid = addressFlowOrderId?.trim().orEmpty()
                                                    if (oid.isNotEmpty()) {
                                                        orderDetailViewModel.load(oid)
                                                    }
                                                    showShippingAddressList = false
                                                    addressFlowOrderId = null
                                                },
                                            )
                                        }
                                        if (showAddAddressScreen) {
                                            AddEditAddressScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                viewModel = addressBookViewModel,
                                                onBack = {
                                                    val fromList = addAddressOpenedFromList
                                                    showAddAddressScreen = false
                                                    if (fromList) {
                                                        addAddressOpenedFromList = false
                                                    } else {
                                                        addressFlowOrderId = null
                                                    }
                                                },
                                                onSaved = { newId ->
                                                    val oid = addressFlowOrderId
                                                    val addr = addressBookViewModel.addressById(newId)
                                                    if (oid != null && addr != null) {
                                                        addressBookViewModel.setOrderShipping(oid, addr)
                                                    }
                                                    showAddAddressScreen = false
                                                    if (addAddressOpenedFromList) {
                                                        addAddressOpenedFromList = false
                                                    } else {
                                                        addressFlowOrderId = null
                                                    }
                                                },
                                            )
                                        }
                                        if (selectedCheckoutListingId != null) {
                                            CheckoutScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                listingId = selectedCheckoutListingId!!,
                                                overridePriceVnd = selectedCheckoutOfferPrice,
                                                existingOrderId = checkoutExistingOrderId,
                                                viewModel = checkoutViewModel,
                                                onBack = {
                                                    selectedCheckoutListingId = null
                                                    selectedCheckoutOfferPrice = 0L
                                                    checkoutExistingOrderId = null
                                                },
                                                onSuccess = { paidOrderId ->
                                                    selectedCheckoutListingId = null
                                                    selectedCheckoutOfferPrice = 0L
                                                    checkoutExistingOrderId = null
                                                    selectedOrderId = paidOrderId
                                                    orderDetailViewModel.load(paidOrderId)
                                                },
                                            )
                                        }
                                        if (showOrdersScreen && selectedOrderId == null) {
                                            com.pc.fash_android_mobile.ui.orders.OrdersScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                viewModel = ordersViewModel,
                                                onBack = { showOrdersScreen = false },
                                                onExploreClick = {
                                                    showOrdersScreen = false
                                                    selectedTab = MainTab.Explore.ordinal
                                                },
                                                promoSlides = mappedPromoSlides,
                                                onPromoSlideClick = handlePromoClick,
                                                onOrderClick = { order ->
                                                    selectedOrderId = order.orderId
                                                },
                                            )
                                        }
                                        if (showHomeDeliveringScreen && selectedOrderId == null) {
                                            HomeDeliveringScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                viewModel = homeDeliveringViewModel,
                                                onBack = { showHomeDeliveringScreen = false },
                                                onOrderClick = { order ->
                                                    selectedOrderId = order.orderId
                                                },
                                                onOpenAllOrders = {
                                                    showHomeDeliveringScreen = false
                                                    showOrdersScreen = true
                                                },
                                                onDataMutated = {
                                                    homeViewModel.refresh()
                                                    ordersViewModel.refreshOrders()
                                                },
                                            )
                                        }
                                        if (sellerPackageCheckout != null) {
                                            SellerPackageCheckoutScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                pkg = sellerPackageCheckout!!,
                                                onBack = { sellerPackageCheckout = null },
                                            )
                                        } else if (showSellerPackagesScreen && selectedOrderId == null) {
                                            SellerProductPackagesScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                viewModel = sellerProductPackagesViewModel,
                                                onBack = { showSellerPackagesScreen = false },
                                                onBuyPackage = { pkg ->
                                                    sellerPackageCheckout = pkg
                                                },
                                            )
                                        }
                                        if (showFollowConnections) {
                                            FollowConnectionsScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                viewModel = followConnectionsViewModel,
                                                onBack = { showFollowConnections = false },
                                                onExploreClick = {
                                                    showFollowConnections = false
                                                    selectedTab = MainTab.Explore.ordinal
                                                },
                                                onUserClick = { user ->
                                                    val u = user.username.trim()
                                                    if (u.isNotEmpty()) {
                                                        sellerShopEntrySource =
                                                            SellerShopEntrySource.FollowConnections
                                                        sellerShopRestoreContext = SellerShopRestoreContext(
                                                            followConnectionsTab = followConnectionsInitialTab,
                                                        )
                                                        sellerShopUsername = u
                                                    }
                                                },
                                            )
                                        }
                                        if (showFeaturedSellersAll) {
                                            FeaturedSellersScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                viewModel = featuredSellersViewModel,
                                                onBack = { showFeaturedSellersAll = false },
                                                onSellerClick = { seller ->
                                                    val u = seller.username.trim()
                                                    if (u.isNotEmpty()) {
                                                        sellerShopEntrySource = SellerShopEntrySource.FeaturedSellers
                                                        sellerShopRestoreContext = SellerShopRestoreContext(
                                                            reopenFeaturedSellers = true,
                                                        )
                                                        sellerShopUsername = u
                                                    }
                                                },
                                                onListingClick = { lid, sellerId ->
                                                    val myId =
                                                        authManager.sessionStore.read()?.userId?.trim().orEmpty()
                                                    if (!sellerId.isNullOrBlank() && sellerId == myId) {
                                                        selectedListingId = null
                                                        editListingId = lid
                                                    } else {
                                                        selectedListingId = lid
                                                    }
                                                },
                                            )
                                        }
                                        if (showInviteFriendsScreen) {
                                            InviteFriendsScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                referrerUsername = profileForInvite?.username,
                                                userRepository = fashApp.userRepository,
                                                onBack = { showInviteFriendsScreen = false },
                                                onUserMessage = { msg ->
                                                    mainScope.launch {
                                                        enqueueSnackbarSerial { showSnackbar(msg) }
                                                    }
                                                },
                                            )
                                        }
                                        if (chatShipFlowArgs != null) {
                                            ChatShipFulfillmentScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                args = chatShipFlowArgs!!,
                                                onBack = { chatShipFlowArgs = null },
                                                orderDetailViewModel = orderDetailViewModel,
                                                addressBookViewModel = addressBookViewModel,
                                                onOpenShippingAddressList = {
                                                    addressFlowOrderId = chatShipFlowArgs?.orderId
                                                    showShippingAddressList = true
                                                },
                                                onOpenAddShippingAddress = {
                                                    addressFlowOrderId = chatShipFlowArgs?.orderId
                                                    showAddAddressScreen = true
                                                    addAddressOpenedFromList = false
                                                },
                                                onContinueToCheckout = { listingId, amountVnd, existingOid ->
                                                    chatShipFlowArgs = null
                                                    selectedCheckoutListingId = listingId
                                                    selectedCheckoutOfferPrice = amountVnd
                                                    checkoutExistingOrderId = existingOid
                                                },
                                                shipOnlinePaymentEnabled = BusinessFlowConfig.c2cShipOnlinePaymentEnabled,
                                                onCancelOrder = {
                                                    chatShipFlowArgs?.orderId?.trim()?.takeIf { it.isNotEmpty() }
                                                        ?.let { orderIdPendingCancel = it }
                                                },
                                            )
                                        }
                                        val hasMainOverlayBack = remember(
                                            showFeaturedSellersAll,
                                            sellerShopUsername,
                                            showFollowConnections,
                                            showHomeDeliveringScreen,
                                            showSellerPackagesScreen,
                                            showInviteFriendsScreen,
                                            sellerPackageCheckout,
                                            showOrdersScreen,
                                            selectedCheckoutListingId,
                                            showAddAddressScreen,
                                            showShippingAddressList,
                                            selectedOrderId,
                                            chatShipFlowArgs,
                                            selectedConversationId,
                                            chatOrderDetailOverlayId,
                                            showEditProfile,
                                            editListingId,
                                            selectedListingId,
                                        ) {
                                            showFeaturedSellersAll ||
                                                sellerShopUsername != null ||
                                                showFollowConnections ||
                                                showHomeDeliveringScreen ||
                                                showSellerPackagesScreen ||
                                                showInviteFriendsScreen ||
                                                sellerPackageCheckout != null ||
                                                showOrdersScreen ||
                                                selectedCheckoutListingId != null ||
                                                showAddAddressScreen ||
                                                showShippingAddressList ||
                                                selectedOrderId != null ||
                                                chatShipFlowArgs != null ||
                                                selectedConversationId != null ||
                                                chatOrderDetailOverlayId != null ||
                                                showEditProfile ||
                                                editListingId != null ||
                                                selectedListingId != null
                                        }
                                        BackHandler(enabled = hasMainOverlayBack) {
                                            when {
                                                showFeaturedSellersAll && sellerShopUsername == null -> {
                                                    showFeaturedSellersAll = false
                                                }
                                                sellerShopUsername != null -> {
                                                    dismissSellerShopOverlay()
                                                }
                                                showFollowConnections -> {
                                                    showFollowConnections = false
                                                }
                                                showHomeDeliveringScreen && selectedOrderId == null -> {
                                                    showHomeDeliveringScreen = false
                                                }
                                                sellerPackageCheckout != null -> {
                                                    sellerPackageCheckout = null
                                                }
                                                showInviteFriendsScreen -> {
                                                    showInviteFriendsScreen = false
                                                }
                                                showSellerPackagesScreen -> {
                                                    showSellerPackagesScreen = false
                                                }
                                                selectedCheckoutListingId != null -> {
                                                    selectedCheckoutListingId = null
                                                    selectedCheckoutOfferPrice = 0L
                                                    checkoutExistingOrderId = null
                                                }
                                                showAddAddressScreen -> {
                                                    val fromList = addAddressOpenedFromList
                                                    showAddAddressScreen = false
                                                    if (fromList) {
                                                        addAddressOpenedFromList = false
                                                    } else {
                                                        addressFlowOrderId = null
                                                    }
                                                }
                                                showShippingAddressList -> {
                                                    showShippingAddressList = false
                                                    addressFlowOrderId = null
                                                }
                                                chatShipFlowArgs != null -> {
                                                    chatShipFlowArgs = null
                                                }
                                                selectedOrderId != null -> {
                                                    selectedOrderId = null
                                                }
                                                chatOrderDetailOverlayId != null -> {
                                                    chatOrderDetailOverlayId = null
                                                }
                                                selectedConversationId != null -> {
                                                    chatOrderDetailOverlayId = null
                                                    chatShipFlowArgs = null
                                                    selectedConversationId = null
                                                    selectedConversationItem = null
                                                    chatViewModel.loadConversations()
                                                    chatViewModel.refreshUnreadCount()
                                                }
                                                showEditProfile -> {
                                                    showEditProfile = false
                                                }
                                                editListingId != null -> {
                                                    editListingId = null
                                                }
                                                showOrdersScreen -> {
                                                    showOrdersScreen = false
                                                }
                                                selectedListingId != null -> {
                                                    selectedListingId = null
                                                }
                                            }
                                        }
                                        val inApp by fashApp.inAppNotification.collectAsState()
                                        LaunchedEffect(inApp?.shownAt) {
                                            val token = inApp?.shownAt ?: return@LaunchedEffect
                                            delay(4500)
                                            if (fashApp.inAppNotification.value?.shownAt == token) {
                                                fashApp.dismissInAppNotification()
                                            }
                                        }
                                        val showInAppBanner = inApp != null &&
                                            !profileSetupBlocksShellChrome &&
                                            (inApp?.title?.isNotBlank() == true || inApp?.body?.isNotBlank() == true)
                                        FashInAppNotificationBanner(
                                            visible = showInAppBanner,
                                            title = inApp?.title.orEmpty(),
                                            body = inApp?.body.orEmpty(),
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .zIndex(24f),
                                            onClick = {
                                                val s = inApp ?: return@FashInAppNotificationBanner
                                                val data = s.data
                                                val deepNid = data?.entries
                                                    ?.find { it.key.equals("deep_link", ignoreCase = true) }
                                                    ?.value
                                                    ?.let { InboxDeepLinks.parseNotificationIdFromDeepLinkString(it) }
                                                val nid = s.userNotificationId?.takeIf { it.isNotBlank() }
                                                    ?: data?.get("user_notification_id")?.trim()?.takeIf { it.isNotEmpty() }
                                                    ?: deepNid
                                                if (!nid.isNullOrBlank()) {
                                                    fashApp.pendingInboxNotificationId.value = nid
                                                    fashApp.requestOpenNotificationInbox()
                                                    fashApp.dismissInAppNotification()
                                                    return@FashInAppNotificationBanner
                                                }
                                                val navEarly = data?.entries?.find { e ->
                                                    e.key.equals("nav_target", ignoreCase = true) ||
                                                        e.key.equals("navTarget", ignoreCase = true)
                                                }?.value?.trim()?.lowercase()
                                                val ptypeEarly = data?.get("type")?.trim()?.lowercase().orEmpty()
                                                if (navEarly == "in_app_invite_friends" ||
                                                    ptypeEarly.equals("marketplace.referral.invite_rewarded", ignoreCase = true)
                                                ) {
                                                    showInviteFriendsScreen = true
                                                    fashApp.dismissInAppNotification()
                                                    return@FashInAppNotificationBanner
                                                }
                                                val conv = data?.entries?.find { e ->
                                                    e.key.equals("conversation_id", ignoreCase = true) ||
                                                        e.key.equals("conversationId", ignoreCase = true)
                                                }?.value?.trim()?.takeIf { it.isNotEmpty() }
                                                val nav = data?.entries?.find { e ->
                                                    e.key.equals("nav_target", ignoreCase = true) ||
                                                        e.key.equals("navTarget", ignoreCase = true)
                                                }?.value?.trim()?.lowercase()
                                                val ptype = data?.get("type")?.trim()?.lowercase().orEmpty()
                                                val isChat = nav == "chat" ||
                                                    ptype.contains("chat")
                                                if (!conv.isNullOrBlank() && isChat) {
                                                    selectedListingId = null
                                                    selectedOrderId = null
                                                    editListingId = null
                                                    chatOrderDetailOverlayId = null
                                                    selectedConversationId = conv
                                                    selectedTab = MainTab.Chat.ordinal
                                                    fashApp.dismissInAppNotification()
                                                    return@FashInAppNotificationBanner
                                                }
                                                fashApp.requestOpenNotificationInbox()
                                                fashApp.dismissInAppNotification()
                                            },
                                            onDismissClick = { fashApp.dismissInAppNotification() },
                                        )
                                            }
                                        }
                                    }
                                }
                                loginStep == LoginStep.Email -> LoginScreen(
                                    email = email,
                                    onEmailChange = loginViewModel::onEmailChange,
                                    isOtpLoading = isOtpLoading,
                                    isSocialLoading = isSocialLoading,
                                    snackbarHostState = snackbarHostState,
                                    showSnackbarHost = false,
                                    onSendOtp = loginViewModel::requestEmailOtp,
                                    onGoogleClick = {
                                        if (!googleOk) {
                                            loginViewModel.warnGoogleNotConfigured()
                                        } else {
                                            runCatching {
                                                val client = buildGoogleSignInClient(this@MainActivity)
                                                googleSignInLauncher.launch(client.signInIntent)
                                            }.onFailure {
                                                loginViewModel.warnGoogleNotConfigured()
                                            }
                                        }
                                    },
                                    onFacebookClick = {
                                        if (!facebookOk) {
                                            loginViewModel.warnFacebookNotConfigured()
                                        } else {
                                            LoginManager.getInstance().logInWithReadPermissions(
                                                this@MainActivity,
                                                listOf("email", "public_profile"),
                                            )
                                        }
                                    },
                                    isGoogleConfigured = googleOk,
                                    isFacebookConfigured = facebookOk,
                                    onTermsClick = {
                                        openUrl(AppEnvironment.legalTermsUrl(AppLocale.currentTag(this@MainActivity)))
                                    },
                                    onPrivacyClick = {
                                        openUrl(AppEnvironment.legalPrivacyUrl(AppLocale.currentTag(this@MainActivity)))
                                    },
                                    usePasswordLogin = usePasswordLogin,
                                    onTogglePasswordLogin = loginViewModel::togglePasswordLogin,
                                    password = password,
                                    onPasswordChange = loginViewModel::onPasswordChange,
                                    onLoginWithPassword = loginViewModel::loginWithPassword,
                                    isPasswordLoading = isPasswordLoading,
                                )
                                else -> OtpVerifyScreen(
                                    email = email,
                                    otp = otpCode,
                                    onOtpChange = loginViewModel::onOtpChange,
                                    resendCooldownSec = resendCooldownSec,
                                    isSendOtpLoading = isOtpLoading,
                                    isVerifyLoading = isVerifyLoading,
                                    onVerifyClick = loginViewModel::verifyOtpCode,
                                    onResendClick = loginViewModel::resendEmailOtp,
                                    onBackClick = loginViewModel::backFromOtp,
                                    showOnboardingProgress = false,
                                )
                            }
                            FashSnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 12.dp),
                                additionalBottomInset = snackbarBottomChromeInset,
                            )
                        }
                    } else {
                        FashWaitingScreen()
                    }
                }

                val welcomeBottomInset = when {
                    selectedConversationId != null -> ChatComposerBarOverlayInset
                    isAuthenticated && needsOnboarding == false -> MainNavBottomBarOverlayInset
                    else -> 0.dp
                }
                // Full-screen interstitial — only after profile setup (not during gate load / onboarding).
                FashAppPromoOverlayDialog(
                    campaign = if (profileSetupBlocksShellChrome) null else activePromoCampaign,
                    onDismiss = {
                        activePromoCampaign?.let { campaign ->
                            AppPromoCampaignStore.markDismissed(
                                notificationSnackbarContext.applicationContext,
                                campaign,
                            )
                        }
                        activePromoCampaign = null
                    },
                    onPrimaryClick = { campaign ->
                        when (campaign.kind) {
                            AppPromoCampaignKind.Remote -> {
                                AppPromoCampaignStore.markDismissed(
                                    notificationSnackbarContext.applicationContext,
                                    campaign,
                                )
                                activePromoCampaign = null
                                AppPromoNavigation.applyPrimary(
                                    activity = this@MainActivity,
                                    campaign = campaign,
                                    onTab = { tab -> pendingPromoMainTab = tab.ordinal },
                                    onOpenOrders = { pendingPromoOpenOrders = true },
                                )
                            }
                            AppPromoCampaignKind.Welcome -> {
                                AppPromoCampaignStore.markDismissed(
                                    notificationSnackbarContext.applicationContext,
                                    campaign,
                                )
                                activePromoCampaign = null
                            }
                            AppPromoCampaignKind.AppRating -> {
                                AppPromoCampaignStore.markDismissed(
                                    notificationSnackbarContext.applicationContext,
                                    campaign,
                                )
                                activePromoCampaign = null
                                runCatching {
                                    startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(
                                                "market://details?id=${BuildConfig.APPLICATION_ID}",
                                            ),
                                        ),
                                    )
                                }.onFailure {
                                    startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(
                                                "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}",
                                            ),
                                        ),
                                    )
                                }
                            }
                            AppPromoCampaignKind.KycVerification -> {
                                AppPromoCampaignStore.markDismissed(
                                    notificationSnackbarContext.applicationContext,
                                    campaign,
                                )
                                activePromoCampaign = null
                                val verifyUrl = AppEnvironment.identityReverifyUrl.trim()
                                if (verifyUrl.isNotEmpty()) {
                                    runCatching {
                                        CustomTabsIntent.Builder()
                                            .setShowTitle(true)
                                            .build()
                                            .launchUrl(
                                                this@MainActivity,
                                                Uri.parse(verifyUrl),
                                            )
                                    }
                                }
                            }
                            AppPromoCampaignKind.SellerPackage -> {
                                AppPromoCampaignStore.markDismissed(
                                    notificationSnackbarContext.applicationContext,
                                    campaign,
                                )
                                activePromoCampaign = null
                                pendingPromoMainTab = MainTab.Post.ordinal
                            }
                        }
                    },
                    onSecondaryClick = { campaign ->
                        AppPromoCampaignStore.markDismissed(
                            notificationSnackbarContext.applicationContext,
                            campaign,
                        )
                        if (campaign.kind == AppPromoCampaignKind.Remote) {
                            AppPromoNavigation.applySecondary(
                                activity = this@MainActivity,
                                campaign = campaign,
                                onTab = { tab -> pendingPromoMainTab = tab.ordinal },
                                onOpenOrders = { pendingPromoOpenOrders = true },
                            )
                        }
                        activePromoCampaign = null
                    },
                )
                val accountSwitchPrompt by fashApp.pendingAccountSwitchPrompt.collectAsState()
                accountSwitchPrompt?.let { prompt ->
                    val activeUserId = authManager.sessionStore.read()?.userId?.trim().orEmpty()
                    if (activeUserId.isEmpty() || !activeUserId.equals(prompt.pendingUserId, ignoreCase = true)) {
                        AlertDialog(
                            onDismissRequest = { fashApp.clearAccountSwitchPrompt() },
                            title = { Text(stringResource(R.string.account_switch_dialog_title)) },
                            text = {
                                Text(
                                    stringResource(
                                        R.string.account_switch_dialog_message,
                                        prompt.emailMasked ?: "…",
                                        prompt.unreadCount,
                                    ),
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        fashApp.clearAccountSwitchPrompt()
                                        val session = authManager.sessionStore.read()
                                        mainScope.launch {
                                            if (session != null) {
                                                withContext(Dispatchers.IO) {
                                                    authManager.logout(session.accessToken)
                                                }
                                            }
                                            loginViewModel.prefillEmailForAccountSwitch(prompt.emailMasked)
                                        }
                                    },
                                ) {
                                    Text(stringResource(R.string.account_switch_dialog_confirm))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { fashApp.clearAccountSwitchPrompt() }) {
                                    Text(stringResource(R.string.account_switch_dialog_dismiss))
                                }
                            },
                        )
                    } else {
                        LaunchedEffect(prompt.pendingUserId) {
                            fashApp.clearAccountSwitchPrompt()
                        }
                    }
                }
                FashGlobalDialogHost(
                    message = if (profileSetupBlocksShellChrome) null else dialogMessage,
                    onDismiss = { fashApp.uiDialog.dismiss() },
                    bottomOverlayInset = welcomeBottomInset,
                )
            }
            }
        }
    }

    private fun openUrl(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }
}

@Preview(showBackground = true)
@Composable
private fun MainPreview() {
    FashTheme {
        LoginScreen(
            email = "",
            onEmailChange = {},
            isOtpLoading = false,
            isSocialLoading = false,
            snackbarHostState = remember { SnackbarHostState() },
            onSendOtp = {},
            onGoogleClick = {},
            onFacebookClick = {},
            isGoogleConfigured = false,
            isFacebookConfigured = false,
            onTermsClick = {},
            onPrivacyClick = {},
        )
    }
}
