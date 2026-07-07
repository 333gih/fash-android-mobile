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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
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
import com.pc.fash_android_mobile.data.auth.GoogleSignInFlow
import com.pc.fash_android_mobile.data.auth.clearCachedSocialSignInForLogout
import com.pc.fash_android_mobile.ui.explore.ExplorePrimarySection
import com.pc.fash_android_mobile.ui.explore.ExploreViewModel
import com.pc.fash_android_mobile.ui.explore.FeaturedSellersScreen
import com.pc.fash_android_mobile.ui.explore.FeaturedSellersViewModel
import com.pc.fash_android_mobile.ui.home.HomeEditorialDetailScreen
import com.pc.fash_android_mobile.ui.home.HomeEditorialListScreen
import com.pc.fash_android_mobile.ui.home.UserExperienceSurveyScreen
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
import com.pc.fash_android_mobile.notifications.InAppNotificationNavigation
import com.pc.fash_android_mobile.notifications.PushNotificationRouter
import com.pc.fash_android_mobile.notifications.RealtimeNotificationRouter
import com.pc.fash_android_mobile.ui.chat.ChatInAppNotificationPolicy
import com.pc.fash_android_mobile.ui.chat.ChatNotificationPresence
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
import com.pc.fash_android_mobile.ui.main.GuestMainShell
import com.pc.fash_android_mobile.ui.orders.OrdersViewModel
import com.pc.fash_android_mobile.ui.main.MainNavScreen
import com.pc.fash_android_mobile.network.PublicBrowseHttp
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
import com.pc.fash_android_mobile.ui.login.LoginHeroSlidesViewModel
import com.pc.fash_android_mobile.ui.onboarding.ProfilePhotoOnboardScreen
import com.pc.fash_android_mobile.ui.onboarding.OnboardingFlowProgress
import com.pc.fash_android_mobile.ui.onboarding.OnboardingShoppingScreen
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
import com.pc.fash_android_mobile.ui.settings.NotificationPreferencesViewModel
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
import com.pc.fash_android_mobile.data.promo.AppPromoOnAppOpenLoader
import com.pc.fash_android_mobile.data.promo.AppPromoPendingQueue
import com.pc.fash_android_mobile.data.promo.AppPromoPresentationPolicy
import com.pc.fash_android_mobile.data.promo.isAppPromoPushData
import com.pc.fash_android_mobile.data.promo.parseAppPromoFromPushData
import com.pc.fash_android_mobile.data.promo.parseRemoteAppPromoPayload
import com.pc.fash_android_mobile.data.promo.toAppPromoCampaign
import com.pc.fash_android_mobile.data.recommendation.NotificationEngagementReporter
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
import com.pc.fash_android_mobile.deeplink.ProfileDeepLinks
import com.pc.fash_android_mobile.data.theme.AppThemePreference
import com.pc.fash_android_mobile.data.onboarding.AppFeatureTourStore
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val SPLASH_DISPLAY_MS = 750L
/** Cap cold-start session refresh so splash never blocks on a hung auth refresh. */
private const val SPLASH_SESSION_VALIDATE_TIMEOUT_MS = 12_000L
/** Home feed gate — never trap the user longer than this on the waiting screen (iOS parity). */
private const val SHELL_WARMUP_HOME_GATE_MAX_MS = 6_000L
/** Cap setup-status gate so authenticated users are not stuck on [FashWaitingScreen] indefinitely. */
private const val SETUP_GATE_TOTAL_TIMEOUT_MS = 15_000L

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

/**
 * Used after username submit (the final mandatory step). Same as [resolveShellNeedsOnboardingAfterStep]
 * but the VM will have already moved to [OnboardingStep.Completed] before the callback fires, so we
 * can rely solely on the access-status API poll here. Kept separate so the behaviour is explicit.
 */
private suspend fun resolveNeedsOnboardingAfterProfileSubmit(
    repo: UserRepository,
    onboardingVm: OnboardingViewModel,
): Boolean {
    // Primary gate: trust the VM step — if it says there are more steps, stay in onboarding.
    if (onboardingVm.onboardingStep.value != OnboardingStep.Completed) return true
    // Secondary gate: confirm with the server (up to N attempts).
    repeat(ACCESS_STATUS_POLL_ATTEMPTS) { attempt ->
        repo.getUserAccessStatus().fold(
            onSuccess = { status -> if (status.canAccessHome) return false },
            onFailure = { },
        )
        if (attempt < ACCESS_STATUS_POLL_ATTEMPTS - 1) delay(ACCESS_STATUS_POLL_MS)
    }
    // If server still says not ready but VM is Completed, go home anyway (server may lag).
    return false
}

class MainActivity : ComponentActivity() {

    private val callbackManager: CallbackManager = CallbackManager.Factory.create()
    private val loginViewModel: LoginViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
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
    private val notificationPreferencesViewModel: NotificationPreferencesViewModel by viewModels()
    private val notificationsViewModel: com.pc.fash_android_mobile.ui.notifications.NotificationsViewModel by viewModels()
    private val promoSlidesViewModel: PromoSlidesViewModel by viewModels()
    private val loginHeroSlidesViewModel: LoginHeroSlidesViewModel by viewModels()
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
        InviteDeepLinks.parseReferrerFromIntent(intent)?.let { fashApp.pendingReferrerUsername.value = it }
        fashApp.pendingDeepLinkListingId.value = ListingDeepLinks.parseListingIdFromIntent(intent)
        ProfileDeepLinks.parseUsernameFromIntent(intent)?.let { fashApp.pendingDeepLinkSellerUsername.value = it }
        AccountSwitchDeepLinks.parseFromIntent(intent)?.let {
            fashApp.requestAccountSwitchPrompt(it)
        } ?: PushNotificationRouter.routeFromTrayTap(fashApp, intent)
        NotificationEngagementReporter.reportOpenFromIntent(fashApp.feedEventReporter, intent)
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
            val loginHeroSlides by loginHeroSlidesViewModel.remoteSlides.collectAsState()
            val localeRev by AppLocale.localeRevisionFlow.collectAsState()
            LaunchedEffect(localeRev) {
                loginHeroSlidesViewModel.refresh()
            }
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
            val onboardingShoppingBuy by onboardingViewModel.shoppingBuy.collectAsState()
            val onboardingShoppingSell by onboardingViewModel.shoppingSell.collectAsState()
            val onboardingGenderPreference by onboardingViewModel.genderPreference.collectAsState()
            val onboardingUsername by onboardingViewModel.username.collectAsState()
            val onboardingReferenceSize by onboardingViewModel.referenceSize.collectAsState()
            val onboardingMeasurementUnit by onboardingViewModel.measurementUnit.collectAsState()
            val onboardingMeasHem by onboardingViewModel.measurementHem.collectAsState()
            val onboardingMeasChest by onboardingViewModel.measurementChest.collectAsState()
            val onboardingMeasLength by onboardingViewModel.measurementLength.collectAsState()
            val onboardingMeasShoulders by onboardingViewModel.measurementShoulders.collectAsState()
            val onboardingMeasSleeve by onboardingViewModel.measurementSleeve.collectAsState()
            val onboardingHeightCm by onboardingViewModel.heightCm.collectAsState()
            val onboardingWeightKg by onboardingViewModel.weightKg.collectAsState()
            val onboardingSetupPw by onboardingViewModel.setupPassword.collectAsState()
            val onboardingSetupPwConfirm by onboardingViewModel.setupPasswordConfirm.collectAsState()
            val onboardingLoading by onboardingViewModel.isLoading.collectAsState()
            val onboardingSubmitting by onboardingViewModel.isSubmitting.collectAsState()
            val facebookLoginEnabled = LoginViewModel.isFacebookLoginEnabled()
            val facebookOk = LoginViewModel.isFacebookConfigured()
            val googleOk = LoginViewModel.isGoogleConfigured()
            val profileSetupBlocksShellChrome =
                OnboardingFlowProgress.blocksShellPromosAndTours(needsOnboarding) ||
                    (isAuthenticated && onboardingStep != OnboardingStep.Completed)
            val onboardingProgressStep by onboardingViewModel.uiProgressStep.collectAsState()
            val onboardingProgressTotal by onboardingViewModel.progressTotalSteps.collectAsState()
            val onboardingAvatarUrl by onboardingViewModel.avatarUrl.collectAsState()
            val onboardingAvatarUploading by onboardingViewModel.avatarUploading.collectAsState()

            ProvideAppLocale {
            val contextForTheme = LocalContext.current
            val themeRev by AppThemePreference.revision.collectAsState()
            val themeMode = remember(themeRev) { AppThemePreference.readMode(contextForTheme) }
            val useDarkTheme = when (themeMode) {
                AppThemePreference.Mode.LIGHT -> false
                AppThemePreference.Mode.DARK,
                AppThemePreference.Mode.SYSTEM -> true
            }
            val lightAppearance = when (themeMode) {
                AppThemePreference.Mode.LIGHT -> FashLightAppearance.PureWhite
                else -> FashLightAppearance.Editorial
            }
            FashTheme(darkTheme = useDarkTheme, lightAppearance = lightAppearance) {
                var splashFinished by rememberSaveable { mutableStateOf(false) }
                var splashStartMs by rememberSaveable { mutableStateOf(0L) }
                var isGuestBrowse by rememberSaveable { mutableStateOf(false) }
                var shellWarmupComplete by remember { mutableStateOf(false) }
                /** One-shot: cold start without session may enter guest shell; logout does not. */
                var initialGuestShellDecided by rememberSaveable { mutableStateOf(false) }
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
                    // `authenticated` is the definitive result: we don't rely on the Compose
                    // collectAsState snapshot of isAuthenticated here because validateOrClearSession
                    // sets _isAuthenticated on the IO thread and the StateFlow update may not yet
                    // have been delivered to the Compose snapshot by the time we return to the main
                    // thread. Using the direct boolean prevents a one-frame race where
                    // splashFinished=true but isAuthenticated is still false, which would cause
                    // LaunchedEffect(splashFinished, isAuthenticated) below to incorrectly set
                    // isGuestBrowse=true and briefly show the guest shell.
                    val authenticated = if (hasSession) {
                        withContext(Dispatchers.IO) {
                            withTimeoutOrNull(SPLASH_SESSION_VALIDATE_TIMEOUT_MS) {
                                authManager.validateOrClearSession()
                            } ?: true
                        }
                    } else {
                        false
                    }
                    // Decide guest vs authenticated shell in the same synchronous block as
                    // splashFinished so all three mutations are batched into one recomposition.
                    if (!initialGuestShellDecided) {
                        initialGuestShellDecided = true
                        if (!authenticated && PublicBrowseHttp.isConfigured()) {
                            isGuestBrowse = true
                            fashApp.isGuestBrowseActive = true
                        }
                    }
                    splashFinished = true
                }

                // Fallback: handles the (rare) process-restore path where splashFinished is
                // already true from rememberSaveable but guest-shell was not yet decided.
                // In the normal cold-start path, initialGuestShellDecided is already true by
                // the time splashFinished=true so this block is a no-op.
                LaunchedEffect(splashFinished, isAuthenticated) {
                    if (!splashFinished || initialGuestShellDecided) return@LaunchedEffect
                    initialGuestShellDecided = true
                    if (!isAuthenticated && PublicBrowseHttp.isConfigured()) {
                        isGuestBrowse = true
                    }
                }

                // Connect / disconnect the realtime WebSocket on auth state changes
                val realtimeManager = (application as FashApplication).realtimeManager
                val fashApp = application as FashApplication
                val notificationSnackbarContext = LocalContext.current
                val shellCoroutineScope = rememberCoroutineScope()
                val dialogMessage by fashApp.uiDialog.current.collectAsState()
                /** Hoisted so [FashGlobalDialogHost] can reserve bottom inset for chat composer vs main nav. */
                var selectedConversationId by rememberSaveable { mutableStateOf<String?>(null) }
                /** Hoisted for [FashSnackbarHost] — main bottom nav vs chat composer vs fullscreen overlays. */
                var snackbarBottomChromeInset by remember { mutableStateOf(0.dp) }
                /** Blocking center interstitial after home (welcome, KYC, rating, …). */
                var activePromoCampaign by remember { mutableStateOf<AppPromoCampaign?>(null) }
                var pendingPromoMainTab by remember { mutableIntStateOf(-1) }
                var pendingPromoOpenOrders by remember { mutableStateOf(false) }
                var pendingPromoOpenExplore by remember { mutableStateOf(false) }
                fun presentAdminPromoIfEligible(promo: AppPromoCampaign) {
                    val appCtx = notificationSnackbarContext.applicationContext
                    if (
                        !splashFinished ||
                        !isAuthenticated ||
                        profileSetupBlocksShellChrome ||
                        selectedConversationId != null
                    ) {
                        return
                    }
                    if (AppPromoCampaignStore.isDialogConsumed(appCtx, promo)) {
                        AppPromoPendingQueue.remove(promo.id)
                        return
                    }
                    if (!AppPromoCampaignStore.canShow(appCtx, promo)) return
                    activePromoCampaign = promo
                    AppPromoCampaignStore.recordShow(appCtx, promo)
                    AppPromoCampaignStore.markDialogConsumed(appCtx, promo)
                    AppPromoPendingQueue.remove(promo.id)
                    AppPromoPresentationPolicy.markInboxReadAfterDialogShown(
                        shellCoroutineScope,
                        fashApp,
                        promo,
                    )
                }
                val meetingReverifyRequired by profileViewModel.meetingSchedulingReverifyRequired.collectAsState()
                /** Guided main-shell tour after promo (once per session if not completed). */
                var showFeatureTour by remember { mutableStateOf(false) }
                var featureTourPromptedThisSession by remember { mutableStateOf(false) }
                val notifPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { }

                LaunchedEffect(isAuthenticated) {
                    if (isAuthenticated) {
                        isGuestBrowse = false
                        fashApp.isGuestBrowseActive = false
                        shellWarmupComplete = false
                        realtimeManager.connect()
                        pendingPaymentViewModel.startMonitoring()
                    } else if (!isGuestBrowse) {
                        realtimeManager.disconnect()
                        pendingPaymentViewModel.clearForLogout()
                        profileViewModel.clearCachedProfile()
                        homeViewModel.clearCachesForSignedOutUser()
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

                /** Gate on Home feed before revealing the main shell; other tabs prefetch in the background (iOS parity). */
                LaunchedEffect(splashFinished, isAuthenticated, isGuestBrowse, needsOnboarding) {
                    if (!splashFinished) return@LaunchedEffect
                    if (isAuthenticated && needsOnboarding != false) return@LaunchedEffect
                    if (shellWarmupComplete) return@LaunchedEffect
                    try {
                        withTimeoutOrNull(SHELL_WARMUP_HOME_GATE_MAX_MS) {
                            homeViewModel.awaitLaunchReady(isGuestBrowse)
                        }
                    } finally {
                        shellWarmupComplete = true
                        shellCoroutineScope.launch {
                            exploreViewModel.refresh()
                            if (isAuthenticated) {
                                profileViewModel.onAuthenticatedSessionReady()
                                profileViewModel.refresh(force = true)
                                ordersViewModel.refreshOrders()
                                chatViewModel.loadConversations()
                                notificationsViewModel.refreshUnreadSummary()
                            }
                        }
                    }
                }

                // Profile setup (loading gate or onboarding screens) blocks welcome promo + feature tour.
                LaunchedEffect(profileSetupBlocksShellChrome) {
                    if (profileSetupBlocksShellChrome) {
                        activePromoCampaign = null
                        showFeatureTour = false
                        fashApp.uiDialog.dismiss()
                    }
                }

                // Resolve app-open promo once main shell is ready (and again when returning to foreground).
                val lifecycleOwner = LocalLifecycleOwner.current
                var promoOpenCountIncremented by remember { mutableStateOf(false) }
                LaunchedEffect(splashFinished, isAuthenticated, needsOnboarding, lifecycleOwner) {
                    if (!splashFinished || !isAuthenticated) return@LaunchedEffect
                    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        if (profileSetupBlocksShellChrome) return@repeatOnLifecycle
                        if (selectedConversationId != null) return@repeatOnLifecycle
                        if (activePromoCampaign != null) return@repeatOnLifecycle
                        delay(550)
                        val appCtx = notificationSnackbarContext.applicationContext
                        val openCount = withContext(Dispatchers.IO) {
                            if (!promoOpenCountIncremented) {
                                promoOpenCountIncremented = true
                                AppPromoCampaignStore.incrementAppOpenCount(appCtx)
                            } else {
                                AppPromoCampaignStore.readAppOpenCount(appCtx)
                            }
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
                        if (activePromoCampaign != null) return@repeatOnLifecycle
                        withContext(Dispatchers.IO) {
                            val resolved = AppPromoOnAppOpenLoader.syncAndResolve(
                                app = fashApp,
                                appContext = appCtx,
                                isGuestMode = false,
                                blockBecauseOtherUi = selectedConversationId != null,
                                incrementOpenCount = false,
                            ) ?: AppPromoCampaignResolver.resolve(gate, appCtx)
                            if (resolved != null && !AppPromoCampaignStore.isDialogConsumed(appCtx, resolved)) {
                                withContext(Dispatchers.Main) {
                                    activePromoCampaign = resolved
                                }
                                AppPromoCampaignStore.recordShow(appCtx, resolved)
                                AppPromoCampaignStore.markDialogConsumed(appCtx, resolved)
                                AppPromoPendingQueue.remove(resolved.id)
                                AppPromoPresentationPolicy.markInboxReadAfterDialogShown(
                                    shellCoroutineScope,
                                    fashApp,
                                    resolved,
                                )
                            }
                        }
                    }
                }

                LaunchedEffect(selectedConversationId) {
                    if (selectedConversationId != null) {
                        activePromoCampaign = null
                    }
                }

                LaunchedEffect(activePromoCampaign, splashFinished, isAuthenticated, needsOnboarding) {
                    if (!splashFinished || !isAuthenticated || profileSetupBlocksShellChrome) {
                        showFeatureTour = false
                        return@LaunchedEffect
                    }
                    val appCtx = notificationSnackbarContext.applicationContext
                    val tourCompleted = withContext(Dispatchers.IO) {
                        AppFeatureTourStore.isCompletedForCurrentVersion(appCtx)
                    }
                    if (tourCompleted || featureTourPromptedThisSession) {
                        showFeatureTour = false
                        return@LaunchedEffect
                    }
                    if (activePromoCampaign != null) {
                        showFeatureTour = false
                        return@LaunchedEffect
                    }
                    delay(400)
                    if (withContext(Dispatchers.IO) {
                            AppFeatureTourStore.isCompletedForCurrentVersion(appCtx)
                        } || activePromoCampaign != null
                    ) {
                        return@LaunchedEffect
                    }
                    featureTourPromptedThisSession = true
                    showFeatureTour = true
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
                        fashApp.preferredLocaleSync.syncIfSessionSuspend(
                            AppLocale.currentTag(this@MainActivity),
                        )
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
                                val pushData = event.data ?: emptyMap()
                                if (AccountSwitchDeepLinks.parseFromFcmData(pushData) != null) {
                                    return@collect
                                }
                                val openId = ChatNotificationPresence.openConversationId(
                                    selectedConversationId,
                                    fashApp.activeChatConversationId,
                                )
                                RealtimeNotificationRouter.handleNotificationShow(
                                    event = event,
                                    app = fashApp,
                                    context = notificationSnackbarContext,
                                    openConversationId = openId,
                                    chatViewModel = chatViewModel,
                                    presentAdminPromo = ::presentAdminPromoIfEligible,
                                )
                            }
                            is RealtimeEvent.MessageNew -> {
                                val openId = ChatNotificationPresence.openConversationId(
                                    selectedConversationId,
                                    fashApp.activeChatConversationId,
                                )
                                RealtimeNotificationRouter.handleMessageNew(
                                    event = event,
                                    app = fashApp,
                                    context = notificationSnackbarContext,
                                    openConversationId = openId,
                                    chatViewModel = chatViewModel,
                                    isGuestMode = isGuestBrowse,
                                )
                            }
                            is RealtimeEvent.AppPromoShow -> {
                                val promo = parseRemoteAppPromoPayload(event.campaignJson)?.toAppPromoCampaign()
                                    ?: return@collect
                                AppPromoPresentationPolicy.handleIncoming(
                                    app = fashApp,
                                    campaign = promo,
                                    openConversationId = selectedConversationId,
                                    userNotificationId = null,
                                    presentDialog = ::presentAdminPromoIfEligible,
                                )
                            }
                            else -> Unit
                        }
                    }
                }

                LaunchedEffect(splashFinished, isAuthenticated, needsOnboarding) {
                    if (!splashFinished || !isAuthenticated) return@LaunchedEffect
                    fashApp.appPromoShowSignals.collect { promo ->
                        AppPromoPresentationPolicy.handleIncoming(
                            app = fashApp,
                            campaign = promo,
                            openConversationId = selectedConversationId,
                            userNotificationId = null,
                            presentDialog = ::presentAdminPromoIfEligible,
                        )
                    }
                }

                LaunchedEffect(splashFinished, isAuthenticated, setupGateAttempt) {
                    if (!splashFinished || !isAuthenticated) return@LaunchedEffect
                    setupGateFetchFailed = false
                    needsOnboarding = null
                    val userRepo = (this@MainActivity.application as FashApplication).userRepository
                    val gate = withContext(Dispatchers.IO) {
                        withTimeoutOrNull(SETUP_GATE_TOTAL_TIMEOUT_MS) {
                            repeat(SETUP_STATUS_INITIAL_ATTEMPTS) { attempt ->
                                userRepo.getUserAccessStatus().fold(
                                    onSuccess = { status ->
                                        return@withTimeoutOrNull status to !status.canAccessHome
                                    },
                                    onFailure = { },
                                )
                                if (attempt < SETUP_STATUS_INITIAL_ATTEMPTS - 1) {
                                    delay(SETUP_STATUS_INITIAL_RETRY_MS)
                                }
                            }
                            null
                        }
                    }
                    if (gate == null) {
                        setupGateFetchFailed = true
                        needsOnboarding = null
                    } else {
                        val (status, needOnboarding) = gate
                        withContext(Dispatchers.IO) {
                            AppFeatureTourStore.markCompletedIfPreviouslyOnboarded(
                                notificationSnackbarContext.applicationContext,
                                status.onboardingDone,
                            )
                        }
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
                        val blockingShellWarmup = !shellWarmupComplete &&
                            (isGuestBrowse || (isAuthenticated && needsOnboarding == false))
                        if (blockingShellWarmup) {
                            FashWaitingScreen()
                        } else {
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
                                    // Username field intentionally starts blank — user must type their own handle.
                                    // (seedUsernameFromEmailIfEmpty removed to avoid pre-filling a name that may conflict.)
                                    val userRepoOnboarding = remember {
                                        (application as FashApplication).userRepository
                                    }
                                    val onboardingAvatarPicker = rememberLauncherForActivityResult(
                                        contract = ActivityResultContracts.GetContent(),
                                    ) { uri: android.net.Uri? ->
                                        uri?.let { u ->
                                            mainScope.launch {
                                                val pair = withContext(Dispatchers.IO) {
                                                    val mimeType = contextForTheme.contentResolver.getType(u)
                                                        ?.takeIf { !it.contains('*') } ?: "image/jpeg"
                                                    contextForTheme.contentResolver.openInputStream(u)?.use { stream ->
                                                        Pair(stream.readBytes(), mimeType)
                                                    }
                                                }
                                                pair?.let { (bytes, mime) ->
                                                    if (bytes.isNotEmpty()) {
                                                        onboardingViewModel.setAvatarFromBytes(bytes, mime)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    when (onboardingStep) {
                                        OnboardingStep.AestheticTags -> OnboardingScreen(
                                            tags = onboardingTags,
                                            selectedIds = onboardingSelected,
                                            isLoading = onboardingLoading,
                                            isSubmitting = onboardingSubmitting,
                                            displayProgressStep = onboardingProgressStep,
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
                                            onBack = { onboardingViewModel.goBack() },
                                        )
                                        OnboardingStep.ShoppingPreferences -> OnboardingShoppingScreen(
                                            buySelected = onboardingShoppingBuy,
                                            sellSelected = onboardingShoppingSell,
                                            isSubmitting = onboardingSubmitting,
                                            displayProgressStep = onboardingProgressStep,
                                            progressTotal = onboardingProgressTotal,
                                            onToggleBuy = onboardingViewModel::toggleShoppingBuy,
                                            onToggleSell = onboardingViewModel::toggleShoppingSell,
                                            selectedGender = onboardingGenderPreference,
                                            onGenderSelect = onboardingViewModel::setGenderPreference,
                                            onContinue = {
                                                onboardingViewModel.submitShoppingPreferences {
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
                                            onBack = { onboardingViewModel.goBack() },
                                        )
                                        OnboardingStep.ProfilePhoto -> {
                                            val canProfilePhoto = remember(onboardingAvatarUrl) {
                                                !onboardingAvatarUrl.isNullOrBlank()
                                            }
                                            ProfilePhotoOnboardScreen(
                                            avatarUrl = onboardingAvatarUrl,
                                            isUploading = onboardingAvatarUploading,
                                            isSubmitting = onboardingSubmitting,
                                            canContinue = canProfilePhoto,
                                            displayProgressStep = onboardingProgressStep,
                                            progressTotal = onboardingProgressTotal,
                                            onPickPhoto = { onboardingAvatarPicker.launch("image/*") },
                                            onContinue = {
                                                onboardingViewModel.completeProfilePhotoStep {
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
                                                onboardingViewModel.skipProfilePhoto {
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
                                            onBack = { onboardingViewModel.goBack() },
                                        )
                                        }
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
                                                genderPreference = onboardingGenderPreference,
                                                heightCm = onboardingHeightCm,
                                                onHeightCmChange = onboardingViewModel::onHeightCmChange,
                                                weightKg = onboardingWeightKg,
                                                onWeightKgChange = onboardingViewModel::onWeightKgChange,
                                                canSubmit = canSizing,
                                                isSubmitting = onboardingSubmitting,
                                                displayProgressStep = onboardingProgressStep,
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
                                                onBack = { onboardingViewModel.goBack() },
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
                                                displayProgressStep = onboardingProgressStep,
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
                                                                resolveNeedsOnboardingAfterProfileSubmit(repo, onboardingViewModel)
                                                            }
                                                        }
                                                    }
                                                },
                                                onBack = { onboardingViewModel.goBack() },
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
                                                displayProgressStep = onboardingProgressStep,
                                                progressTotal = onboardingProgressTotal,
                                                onComplete = {
                                                    onboardingViewModel.submitSetupPassword {
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
                                                onBack = { onboardingViewModel.goBack() },
                                            )
                                        }
                                        OnboardingStep.Completed -> {
                                            LaunchedEffect(Unit) {
                                                needsOnboarding = withContext(Dispatchers.IO) {
                                                    resolveNeedsOnboardingAfterProfileSubmit(
                                                        userRepoOnboarding,
                                                        onboardingViewModel,
                                                    )
                                                }
                                            }
                                            FashWaitingScreen()
                                        }
                                    }
                                }
                                isGuestBrowse && !isAuthenticated -> {
                                    GuestMainShell(
                                        fashApp = fashApp,
                                        homeViewModel = homeViewModel,
                                        exploreViewModel = exploreViewModel,
                                        ordersViewModel = ordersViewModel,
                                        postViewModel = postViewModel,
                                        addressBookViewModel = addressBookViewModel,
                                        profileViewModel = profileViewModel,
                                        chatViewModel = chatViewModel,
                                        changePasswordViewModel = changePasswordViewModel,
                                        notificationPreferencesViewModel = notificationPreferencesViewModel,
                                        notificationsViewModel = notificationsViewModel,
                                        productDetailViewModel = productDetailViewModel,
                                        sellerProfileViewModel = sellerProfileViewModel,
                                        featuredSellersViewModel = featuredSellersViewModel,
                                        promoSlidesViewModel = promoSlidesViewModel,
                                        snackbarHostState = snackbarHostState,
                                        onExitGuestToLogin = {
                                            isGuestBrowse = false
                                            fashApp.isGuestBrowseActive = false
                                        },
                                    )
                                }
                                isAuthenticated -> {
                                    var selectedListingId by rememberSaveable { mutableStateOf<String?>(null) }
                                    var listingDetailBackStack by rememberSaveable { mutableStateOf(listOf<String>()) }
                                    val pendingDeepLink by fashApp.pendingDeepLinkListingId.collectAsState()
                                    var sellerShopUsername by rememberSaveable { mutableStateOf<String?>(null) }
                                    val pendingSellerDeepLink by fashApp.pendingDeepLinkSellerUsername.collectAsState()
                                    LaunchedEffect(pendingSellerDeepLink) {
                                        val handle = pendingSellerDeepLink ?: return@LaunchedEffect
                                        sellerShopUsername = handle
                                        fashApp.pendingDeepLinkSellerUsername.value = null
                                    }
                                    var sellerShopEntrySource by remember { mutableStateOf(SellerShopEntrySource.None) }
                                    /** Snapshot for restoring the screen under seller shop on back. */
                                    var sellerShopRestoreContext by remember {
                                        mutableStateOf(SellerShopRestoreContext())
                                    }
                                    /** True briefly after closing seller shop to block PDP from applying Explore filters (pointer replay). */
                                    var suppressPdpExploreNav by remember { mutableStateOf(false) }
                                    var editListingId by rememberSaveable { mutableStateOf<String?>(null) }
                                    fun closeListingDetail() {
                                        selectedListingId = null
                                        listingDetailBackStack = emptyList()
                                    }
                                    fun openListingDetailFresh(listingId: String) {
                                        listingDetailBackStack = emptyList()
                                        selectedListingId = listingId
                                    }
                                    fun pushListingDetail(listingId: String) {
                                        val lid = listingId.trim()
                                        if (lid.isEmpty()) return
                                        val current = selectedListingId?.trim().orEmpty()
                                        if (current.equals(lid, ignoreCase = true)) return
                                        if (listingDetailBackStack.any { it.equals(lid, ignoreCase = true) }) return
                                        if (current.isNotEmpty()) {
                                            listingDetailBackStack = listingDetailBackStack + current
                                        }
                                        selectedListingId = lid
                                    }
                                    fun popListingDetail() {
                                        if (listingDetailBackStack.isNotEmpty()) {
                                            selectedListingId = listingDetailBackStack.last()
                                            listingDetailBackStack = listingDetailBackStack.dropLast(1)
                                        } else {
                                            closeListingDetail()
                                        }
                                    }
                                    fun openListingDetail(listingId: String, sellerId: String?) {
                                        val myId = authManager.sessionStore.read()?.userId?.trim().orEmpty()
                                        if (!sellerId.isNullOrBlank() && sellerId == myId) {
                                            closeListingDetail()
                                            editListingId = listingId
                                        } else if (selectedListingId != null) {
                                            pushListingDetail(listingId)
                                        } else {
                                            openListingDetailFresh(listingId)
                                        }
                                    }
                                    LaunchedEffect(pendingDeepLink) {
                                        val id = pendingDeepLink ?: return@LaunchedEffect
                                        openListingDetailFresh(id)
                                        fashApp.pendingDeepLinkListingId.value = null
                                    }
                                    var profileEditReturnTab by rememberSaveable { mutableIntStateOf(-1) }
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
                                    var homeEditorialSlug by rememberSaveable { mutableStateOf<String?>(null) }
                                    var showEditorialListScreen by rememberSaveable { mutableStateOf(false) }
                                    var uxSurveyKey by rememberSaveable { mutableStateOf<String?>(null) }
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
                                    var exploreOverlayOpenNonce by rememberSaveable { mutableLongStateOf(0L) }
                                    var selectedTab by rememberSaveable { mutableIntStateOf(MainTab.Home.ordinal) }
                                    val pendingOpenOrderId by fashApp.pendingOpenOrderId.collectAsState()
                                    LaunchedEffect(pendingOpenOrderId) {
                                        val oid = pendingOpenOrderId ?: return@LaunchedEffect
                                        chatOrderDetailOverlayId = null
                                        selectedConversationId = null
                                        selectedConversationItem = null
                                        closeListingDetail()
                                        selectedOrderId = oid
                                        selectedTab = MainTab.Orders.ordinal
                                        fashApp.pendingOpenOrderId.value = null
                                    }
                                    val pendingOpenChatId by fashApp.pendingOpenChatConversationId.collectAsState()
                                    LaunchedEffect(pendingOpenChatId) {
                                        val cid = pendingOpenChatId?.trim()?.takeIf { it.isNotEmpty() }
                                            ?: return@LaunchedEffect
                                        chatOrderDetailOverlayId = null
                                        selectedOrderId = null
                                        closeListingDetail()
                                        editListingId = null
                                        selectedConversationItem = null
                                        selectedConversationId = cid
                                        selectedTab = MainTab.Chat.ordinal
                                        chatViewModel.loadConversations()
                                        chatViewModel.refreshUnreadCount()
                                        ChatNotificationPresence.registerOpenConversation(fashApp, cid)
                                        fashApp.pendingOpenChatConversationId.value = null
                                    }
                                    LaunchedEffect(pendingPromoMainTab, pendingPromoOpenOrders, pendingPromoOpenExplore) {
                                        if (pendingPromoMainTab >= 0) {
                                            selectedTab = pendingPromoMainTab
                                            pendingPromoMainTab = -1
                                        }
                                        if (pendingPromoOpenOrders) {
                                            selectedTab = MainTab.Orders.ordinal
                                            pendingPromoOpenOrders = false
                                        }
                                        if (pendingPromoOpenExplore) {
                                            exploreOverlayOpenNonce++
                                            pendingPromoOpenExplore = false
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
                                                exploreOverlayOpenNonce++
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
                                                selectedTab = MainTab.Orders.ordinal
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
                                        closeListingDetail()
                                        dismissSellerShopOverlay()
                                        exploreOverlayOpenNonce++
                                    }
                                    /** Seller shop focus chips (category / brand / style) — PDP-parity Explore + filters. */
                                    val openExploreFromSellerProfileChips: (
                                        String?,
                                        String?,
                                        String?,
                                        String,
                                        String?,
                                        String?,
                                    ) -> Unit = { cat, brand, aes, q, countryId, countryIso2 ->
                                        sellerShopUsername = null
                                        sellerShopEntrySource = SellerShopEntrySource.None
                                        sellerShopRestoreContext = SellerShopRestoreContext()
                                        closeListingDetail()
                                        exploreViewModel.openExploreFromProfileFilter(
                                            categoryId = cat,
                                            brandId = brand,
                                            aestheticTagId = aes,
                                            searchQuery = q,
                                            countryId = countryId,
                                            countryIso2 = countryIso2,
                                        )
                                        exploreOverlayOpenNonce++
                                    }
                                    /** Bottom nav while seller shop overlay is open — dismiss overlay then land on [tabIndex]. */
                                    val handleMainTabSelectedFromSellerShop: (Int) -> Unit = { tabIndex ->
                                        dismissSellerShopOverlay()
                                        selectedTab = tabIndex
                                    }
                                    val context = LocalContext.current
                                    LaunchedEffect(Unit) {
                                        editListingViewModel.events.collect { msg ->
                                            enqueueSnackbarSerial {
                                                showSnackbar(msg)
                if (msg == context.getString(R.string.edit_listing_saved) ||
                    msg == context.getString(R.string.edit_listing_resubmitted) ||
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
                                            "in_app_explore" -> exploreOverlayOpenNonce++
                                            "in_app_orders" -> selectedTab = MainTab.Orders.ordinal
                                            "in_app_chat" -> selectedTab = MainTab.Chat.ordinal
                                            "in_app_product_packages" -> {
                                                sellerPackageCheckout = null
                                                showSellerPackagesScreen = true
                                            }
                                            "in_app_invite_friends" -> {
                                                showInviteFriendsScreen = true
                                            }
                                            "in_app_editorial_guides" -> {
                                                val slug = nav?.payload?.trim().orEmpty()
                                                if (slug.isNotEmpty()) {
                                                    showEditorialListScreen = false
                                                    homeEditorialSlug = slug
                                                } else {
                                                    homeEditorialSlug = null
                                                    showEditorialListScreen = true
                                                }
                                            }
                                            "in_app_ux_survey" -> {
                                                uxSurveyKey = nav?.payload?.trim().orEmpty().ifBlank { "fash_ux_v1" }
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
                                            ordersViewModel = ordersViewModel,
                                            postViewModel = postViewModel,
                                            addressBookViewModel = addressBookViewModel,
                                            profileViewModel = profileViewModel,
                                            chatViewModel = chatViewModel,
                                            changePasswordViewModel = changePasswordViewModel,
                                        notificationPreferencesViewModel = notificationPreferencesViewModel,
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
                                                closeListingDetail()
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
                                                openListingDetail(lid, sellerId)
                                            },
                                            onNavigateToChatConversation = { conversationId ->
                                                closeListingDetail()
                                                selectedOrderId = null
                                                editListingId = null
                                                chatOrderDetailOverlayId = null
                                                selectedConversationId = conversationId.trim()
                                                selectedTab = MainTab.Chat.ordinal
                                            },
                                            snackbarHostState = snackbarHostState,
                                            chatUnreadCount = chatUnreadCount,
                                            onListingClick = { lid, sellerId ->
                                                openListingDetail(lid, sellerId)
                                            },
                                            onProfileOwnListingClick = { lid, tab ->
                                                profileEditReturnTab = tab
                                                closeListingDetail()
                                                editListingId = lid
                                            },
                                            onEditProfile = { showEditProfile = true },
                                            onShippingAddressesClick = {
                                                addressFlowOrderId = null
                                                showShippingAddressList = true
                                            },
                                            onInviteFriendsClick = { showInviteFriendsScreen = true },
                                            onOrdersClick = { selectedTab = MainTab.Orders.ordinal },
                                            onHomeEditorialPostClick = { post ->
                                                val slug = post.slug.trim().ifBlank { post.id.trim() }
                                                if (slug.isNotEmpty()) homeEditorialSlug = slug
                                            },
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
                                            onNavigateToExploreFromProfile = openExploreFromSellerProfileChips,
                                            promoSlides = mappedPromoSlides,
                                            onPromoSlideClick = handlePromoClick,
                                            selectedTab = selectedTab,
                                            onTabChange = { tabIndex ->
                                                if (sellerShopUsername != null) {
                                                    handleMainTabSelectedFromSellerShop(tabIndex)
                                                } else {
                                                    selectedTab = tabIndex
                                                }
                                            },
                                            exploreOverlayOpenNonce = exploreOverlayOpenNonce,
                                            onMainTabReselectedIntercept = { tab ->
                                                if (sellerShopUsername == null) {
                                                    false
                                                } else {
                                                    handleMainTabSelectedFromSellerShop(tab.ordinal)
                                                    true
                                                }
                                            },
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
                                        selectedListingId?.let { currentListingId ->
                                            key(currentListingId) {
                                            ProductDetailScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                listingId = currentListingId,
                                                viewModel = productDetailViewModel,
                                                profileExploreNavigationEnabled = sellerShopUsername == null &&
                                                    !suppressPdpExploreNav,
                                                onBack = { popListingDetail() },
                                                onChat = { listingId ->
                                                    scope.launch {
                                                        productDetailViewModel.setOpeningChat(true)
                                                        chatViewModel.startConversation(listingId).fold(
                                                            onSuccess = { convId ->
                                                                productDetailViewModel.setOpeningChat(false)
                                                                closeListingDetail()
                                                                selectedTab = MainTab.Chat.ordinal
                                                                selectedConversationItem = null
                                                                chatOrderDetailOverlayId = null
                                                                selectedConversationId = convId
                                                                chatViewModel.loadConversations()
                                                            },
                                                            onFailure = {
                                                                productDetailViewModel.setOpeningChat(false)
                                                                closeListingDetail()
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
                                                    if (!BusinessFlowConfig.c2cBuyNowEnabled) return@ProductDetailScreen
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
                                                            closeListingDetail()
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
                                                    openListingDetail(lid, sellerId)
                                                },
                                                onVisitSellerShop = { username ->
                                                    sellerShopEntrySource = SellerShopEntrySource.ProductDetail
                                                    sellerShopRestoreContext = SellerShopRestoreContext()
                                                    sellerShopUsername = username
                                                },
                                                onNavigateToExploreFromProfile = openExploreFromSellerProfileChips,
                                            )
                                            }
                                        }
                                        if (sellerShopUsername != null &&
                                            selectedListingId == null &&
                                            chatShipFlowArgs == null &&
                                            selectedCheckoutListingId == null
                                        ) {
                                            SellerProfileScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                viewModel = sellerProfileViewModel,
                                                sellerUsername = sellerShopUsername!!,
                                                listingPreviewViewModel = homeViewModel,
                                                onBack = dismissSellerShopOverlay,
                                                onListingClick = { lid, sellerId ->
                                                    val myId =
                                                        authManager.sessionStore.read()?.userId?.trim().orEmpty()
                                                    if (!sellerId.isNullOrBlank() && sellerId == myId) {
                                                        dismissSellerShopOverlay()
                                                        editListingId = lid
                                                    } else {
                                                        openListingDetail(lid, sellerId)
                                                    }
                                                },
                                                onNavigateToExploreFromProfile = openExploreFromSellerProfileChips,
                                                onPromoSlideClick = handlePromoClick,
                                                promoSlides = mappedPromoSlides,
                                            )
                                        }
                                        if (editListingId != null) {
                                            EditListingScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                listingId = editListingId!!,
                                                viewModel = editListingViewModel,
                                                onBack = {
                                                    val tab = profileEditReturnTab
                                                    editListingId = null
                                                    if (tab >= 0 && selectedTab == MainTab.Profile.ordinal) {
                                                        profileViewModel.completeEditReturn(tab, "")
                                                    }
                                                    profileEditReturnTab = -1
                                                },
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
                                                    exploreViewModel.refreshProfileSizingStateAfterSave()
                                                    homeViewModel.refreshSizingBannerAfterProfileSave()
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
                                                    openListingDetailFresh(it)
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
                                                onSellerSuggestNewListing = {
                                                    chatOrderDetailOverlayId = null
                                                    selectedConversationId = null
                                                    selectedConversationItem = null
                                                    chatViewModel.loadConversations()
                                                    chatViewModel.refreshUnreadCount()
                                                    postViewModel.cancel()
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
                                                    openListingDetail(listingId, sellerUserId.takeIf { it.isNotBlank() })
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
                                                        openListingDetail(listingId, sellerUserId.takeIf { it.isNotBlank() })
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
                                                    exploreViewModel.refreshBrowseLocationAfterAddressSave()
                                                    homeViewModel.refreshShoppingContextAfterProfileSave()
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
                                        val editorialSlug = homeEditorialSlug
                                        if (uxSurveyKey != null && selectedOrderId == null) {
                                            UserExperienceSurveyScreen(
                                                surveyKey = uxSurveyKey!!,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                onBack = { uxSurveyKey = null },
                                            )
                                        } else if (showEditorialListScreen && editorialSlug == null && selectedOrderId == null) {
                                            HomeEditorialListScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                onBack = { showEditorialListScreen = false },
                                                onPostClick = { post ->
                                                    val slug = post.slug.trim().ifBlank { post.id.trim() }
                                                    if (slug.isNotEmpty()) {
                                                        showEditorialListScreen = false
                                                        homeEditorialSlug = slug
                                                    }
                                                },
                                            )
                                        } else if (editorialSlug != null && selectedOrderId == null) {
                                            HomeEditorialDetailScreen(
                                                slug = editorialSlug,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                onBack = { homeEditorialSlug = null },
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
                                                    openListingDetail(lid, sellerId)
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
                                            showSellerPackagesScreen,
                                            showInviteFriendsScreen,
                                            showEditorialListScreen,
                                            homeEditorialSlug,
                                            uxSurveyKey,
                                            sellerPackageCheckout,
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
                                                showSellerPackagesScreen ||
                                                showInviteFriendsScreen ||
                                                showEditorialListScreen ||
                                                homeEditorialSlug != null ||
                                                uxSurveyKey != null ||
                                                sellerPackageCheckout != null ||
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
                                                uxSurveyKey != null -> {
                                                    uxSurveyKey = null
                                                }
                                                homeEditorialSlug != null -> {
                                                    homeEditorialSlug = null
                                                }
                                                showEditorialListScreen -> {
                                                    showEditorialListScreen = false
                                                }
                                                showFeaturedSellersAll && sellerShopUsername == null -> {
                                                    showFeaturedSellersAll = false
                                                }
                                                sellerShopUsername != null -> {
                                                    dismissSellerShopOverlay()
                                                }
                                                showFollowConnections -> {
                                                    showFollowConnections = false
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
                                                selectedListingId != null -> {
                                                    popListingDetail()
                                                }
                                            }
                                        }
                                            }
                                        }
                                    }
                                }
                                loginStep == LoginStep.Email -> LoginScreen(
                                    email = email,
                                    onEmailChange = loginViewModel::onEmailChange,
                                    remoteSlides = loginHeroSlides,
                                    isOtpLoading = isOtpLoading,
                                    isSocialLoading = isSocialLoading,
                                    snackbarHostState = snackbarHostState,
                                    showSnackbarHost = false,
                                    onContinueWithoutAccount = if (PublicBrowseHttp.isConfigured()) {
                                        {
                                            isGuestBrowse = true
                                            fashApp.isGuestBrowseActive = true
                                            shellWarmupComplete = false
                                        }
                                    } else {
                                        null
                                    },
                                    onSendOtp = loginViewModel::requestEmailOtp,
                                    onGoogleClick = {
                                        loginViewModel.launchGoogleSignIn(
                                            this@MainActivity,
                                            googleSignInLauncher,
                                        )
                                    },
                                    onFacebookClick = {
                                        if (!facebookLoginEnabled) return@LoginScreen
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
                                    showFacebookLogin = facebookLoginEnabled,
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
                                    .fillMaxWidth(),
                                additionalBottomInset = snackbarBottomChromeInset,
                            )
                        }
                        }
                    } else {
                        FashWaitingScreen()
                    }
                }

                val welcomeBottomInset = when {
                    selectedConversationId != null -> ChatComposerBarOverlayInset
                    (isAuthenticated && needsOnboarding == false) || isGuestBrowse -> MainNavBottomBarOverlayInset
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
                            AppPromoPresentationPolicy.markInboxReadAfterDialogShown(
                                shellCoroutineScope,
                                fashApp,
                                campaign,
                            )
                        }
                        activePromoCampaign = null
                    },
                    onPrimaryClick = { campaign ->
                        AppPromoPresentationPolicy.markInboxReadAfterDialogShown(
                            shellCoroutineScope,
                            fashApp,
                            campaign,
                        )
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
                                    onOpenExplore = { pendingPromoOpenExplore = true },
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
                        AppPromoPresentationPolicy.markInboxReadAfterDialogShown(
                            shellCoroutineScope,
                            fashApp,
                            campaign,
                        )
                        if (campaign.kind == AppPromoCampaignKind.Remote) {
                            AppPromoNavigation.applySecondary(
                                activity = this@MainActivity,
                                campaign = campaign,
                                onTab = { tab -> pendingPromoMainTab = tab.ordinal },
                                onOpenOrders = { pendingPromoOpenOrders = true },
                                onOpenExplore = { pendingPromoOpenExplore = true },
                            )
                        } else {
                            AppPromoCampaignStore.markDismissed(
                                notificationSnackbarContext.applicationContext,
                                campaign,
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
                val inAppNotificationShell by fashApp.inAppNotification.collectAsState()
                LaunchedEffect(inAppNotificationShell?.shownAt) {
                    val token = inAppNotificationShell?.shownAt ?: return@LaunchedEffect
                    delay(4500)
                    if (fashApp.inAppNotification.value?.shownAt == token) {
                        fashApp.dismissInAppNotification()
                    }
                }
                val showInAppNotificationShell = splashFinished &&
                    isAuthenticated &&
                    needsOnboarding == false &&
                    !profileSetupBlocksShellChrome &&
                    inAppNotificationShell?.let { s ->
                        s.title.isNotBlank() || s.body.isNotBlank()
                    } == true
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(200f),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    FashInAppNotificationBanner(
                        visible = showInAppNotificationShell,
                        title = inAppNotificationShell?.title.orEmpty(),
                        body = inAppNotificationShell?.body.orEmpty(),
                        onClick = {
                            val s = inAppNotificationShell ?: return@FashInAppNotificationBanner
                            NotificationEngagementReporter.reportOpen(
                                fashApp.feedEventReporter,
                                s.data,
                            )
                            val ptypeEarly = s.data?.get("type")?.trim()?.lowercase().orEmpty()
                            if (ptypeEarly.equals("marketplace.referral.invite_rewarded", ignoreCase = true)) {
                                fashApp.pendingOpenInviteFriends.value = true
                                fashApp.dismissInAppNotification()
                                return@FashInAppNotificationBanner
                            }
                            InAppNotificationNavigation.handleBannerTap(
                                session = s,
                                onOpenChat = { conv ->
                                    fashApp.pendingOpenChatConversationId.value = conv.trim()
                                    fashApp.dismissInAppNotification()
                                },
                                onOpenOrder = { orderId ->
                                    fashApp.pendingOpenOrderId.value = orderId
                                    fashApp.dismissInAppNotification()
                                },
                                onOpenInviteFriends = {
                                    fashApp.pendingOpenInviteFriends.value = true
                                    fashApp.dismissInAppNotification()
                                },
                                onOpenNotificationDetail = { nid ->
                                    fashApp.pendingInboxNotificationId.value = nid
                                    fashApp.requestOpenNotificationInbox()
                                    fashApp.dismissInAppNotification()
                                },
                                onOpenNotificationInbox = {
                                    fashApp.requestOpenNotificationInbox()
                                    fashApp.dismissInAppNotification()
                                },
                                onOpenDeepLink = { deepLink ->
                                    routeInAppBannerDeepLink(fashApp, deepLink)
                                    fashApp.dismissInAppNotification()
                                },
                            )
                        },
                        onDismissClick = { fashApp.dismissInAppNotification() },
                    )
                }
                FashGlobalDialogHost(
                    message = if (profileSetupBlocksShellChrome) null else dialogMessage,
                    onDismiss = { fashApp.uiDialog.dismiss() },
                    onDismissAll = { fashApp.uiDialog.dismissAll() },
                    bottomOverlayInset = welcomeBottomInset,
                )
            }
            }
        }
    }

    private fun routeInAppBannerDeepLink(fashApp: FashApplication, deepLink: String) {
        InboxDeepLinks.parseNotificationIdFromDeepLinkString(deepLink)?.let { nid ->
            fashApp.pendingInboxNotificationId.value = nid
            fashApp.requestOpenNotificationInbox()
            return
        }
        runCatching {
            val uri = Uri.parse(deepLink.trim())
            ListingDeepLinks.parseListingId(uri)?.let { lid ->
                fashApp.pendingDeepLinkListingId.value = lid
                return
            }
            ProfileDeepLinks.parseUsername(uri)?.let { handle ->
                fashApp.pendingDeepLinkSellerUsername.value = handle
                return
            }
        }
        openUrl(deepLink)
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
