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
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.pc.fash_android_mobile.data.auth.AuthSessionStore
import com.pc.fash_android_mobile.data.auth.buildGoogleSignInClient
import com.pc.fash_android_mobile.data.user.UserRepository
import com.pc.fash_android_mobile.ui.explore.ExploreViewModel
import com.pc.fash_android_mobile.ui.home.HomeViewModel
import com.pc.fash_android_mobile.ui.listing.EditListingScreen
import com.pc.fash_android_mobile.ui.listing.EditListingViewModel
import com.pc.fash_android_mobile.ui.listing.ProductDetailScreen
import com.pc.fash_android_mobile.ui.listing.ProductDetailViewModel
import com.pc.fash_android_mobile.ui.checkout.CheckoutScreen
import com.pc.fash_android_mobile.ui.checkout.CheckoutViewModel
import com.pc.fash_android_mobile.data.chat.ConversationItem
import com.pc.fash_android_mobile.ui.chat.ChatDetailScreen
import com.pc.fash_android_mobile.ui.chat.ChatDetailViewModel
import com.pc.fash_android_mobile.ui.chat.ChatViewModel
import com.pc.fash_android_mobile.ui.profile.EditProfileScreen
import com.pc.fash_android_mobile.ui.profile.EditProfileViewModel
import com.pc.fash_android_mobile.ui.post.PostViewModel
import com.pc.fash_android_mobile.ui.main.MainNavScreen
import com.pc.fash_android_mobile.ui.main.MainTab
import com.pc.fash_android_mobile.ui.login.LoginScreen
import com.pc.fash_android_mobile.ui.onboarding.OnboardingScreen
import com.pc.fash_android_mobile.ui.onboarding.OnboardingViewModel
import com.pc.fash_android_mobile.ui.onboarding.OnboardingStep
import com.pc.fash_android_mobile.ui.onboarding.ProfileSetupScreen
import com.pc.fash_android_mobile.ui.login.LoginStep
import com.pc.fash_android_mobile.ui.login.LoginViewModel
import com.pc.fash_android_mobile.ui.login.OtpVerifyScreen
import com.pc.fash_android_mobile.ui.splash.FashWaitingScreen
import com.pc.fash_android_mobile.ui.theme.FashTheme
import com.pc.fash_android_mobile.ui.address.AddEditAddressScreen
import com.pc.fash_android_mobile.ui.address.AddressBookViewModel
import com.pc.fash_android_mobile.ui.address.ShippingAddressListScreen
import com.pc.fash_android_mobile.ui.orders.OrderDetailScreen
import com.pc.fash_android_mobile.ui.orders.OrderDetailViewModel
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SPLASH_DISPLAY_MS = 2_500L

/**
 * True when the user must complete onboarding (style + username).
 * Uses [UserRepository.getMeProfile] so we don't rely on GET /users/me returning 404 for new users
 * (the API often returns 200 with an incomplete profile).
 */
private fun computeNeedsOnboarding(
    userRepo: UserRepository,
    sessionStore: AuthSessionStore,
): Boolean {
    val session = sessionStore.read() ?: return false
    val profileResult = userRepo.getMeProfile()
    return profileResult.fold(
        onSuccess = { profile ->
            !isProfileOnboardingComplete(profile.username)
        },
        onFailure = {
            session.isNewUser || userRepo.getMe().isFailure
        },
    )
}

/** Matches [com.pc.fash_android_mobile.ui.onboarding.OnboardingViewModel.isUsernameValid] rules. */
private fun isProfileOnboardingComplete(username: String): Boolean {
    val u = username.trim()
    return u.length in 3..30 && u.matches(Regex("^[a-z0-9_.]+$"))
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
    private val editProfileViewModel: EditProfileViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()
    private val chatDetailViewModel: ChatDetailViewModel by viewModels()
    private val checkoutViewModel: CheckoutViewModel by viewModels()
    private val ordersViewModel: com.pc.fash_android_mobile.ui.orders.OrdersViewModel by viewModels()
    private val orderDetailViewModel: OrderDetailViewModel by viewModels()
    private val addressBookViewModel: AddressBookViewModel by viewModels()
    private val authManager get() = (application as FashApplication).authManager

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
            LaunchedEffect(Unit) {
                launch { loginViewModel.events.collect { snackbarHostState.showSnackbar(it) } }
                launch { onboardingViewModel.events.collect { snackbarHostState.showSnackbar(it) } }
                launch { homeViewModel.events.collect { snackbarHostState.showSnackbar(it) } }
                launch { exploreViewModel.events.collect { snackbarHostState.showSnackbar(it) } }
                launch { productDetailViewModel.events.collect { snackbarHostState.showSnackbar(it) } }
                launch { postViewModel.events.collect { snackbarHostState.showSnackbar(it) } }
                launch { editProfileViewModel.events.collect { snackbarHostState.showSnackbar(it) } }
                launch { chatViewModel.events.collect { snackbarHostState.showSnackbar(it) } }
                launch { chatDetailViewModel.events.collect { snackbarHostState.showSnackbar(it) } }
                launch { checkoutViewModel.events.collect { snackbarHostState.showSnackbar(it) } }
                launch { ordersViewModel.events.collect { snackbarHostState.showSnackbar(it) } }
                launch { orderDetailViewModel.events.collect { snackbarHostState.showSnackbar(it) } }
                launch { addressBookViewModel.events.collect { snackbarHostState.showSnackbar(it) } }
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
            val isAuthenticated by authManager.isAuthenticated.collectAsState(initial = authManager.sessionStore.read() != null)
            val sessionExpiredMessage by authManager.sessionExpiredMessage.collectAsState()
            // Show snackbar when the server force-expires the session, then navigate to login
            LaunchedEffect(sessionExpiredMessage) {
                val msg = sessionExpiredMessage ?: return@LaunchedEffect
                snackbarHostState.showSnackbar(msg)
                authManager.clearSessionExpiredMessage()
            }
            var needsOnboarding by rememberSaveable { mutableStateOf<Boolean?>(null) }
            val isLoggingOut by loginViewModel.isLoggingOut.collectAsState()
            val onboardingStep by onboardingViewModel.onboardingStep.collectAsState()
            val onboardingTags by onboardingViewModel.tags.collectAsState()
            val onboardingSelected by onboardingViewModel.selectedIds.collectAsState()
            val onboardingUsername by onboardingViewModel.username.collectAsState()
            val onboardingLoading by onboardingViewModel.isLoading.collectAsState()
            val onboardingSubmitting by onboardingViewModel.isSubmitting.collectAsState()
            val facebookOk = LoginViewModel.isFacebookConfigured()
            val googleOk = LoginViewModel.isGoogleConfigured()

            FashTheme {
                var splashFinished by rememberSaveable { mutableStateOf(false) }
                var splashStartMs by rememberSaveable { mutableStateOf(0L) }
                LaunchedEffect(Unit) {
                    if (splashFinished) return@LaunchedEffect
                    val now = SystemClock.elapsedRealtime()
                    val start = if (splashStartMs == 0L) now else splashStartMs
                    if (splashStartMs == 0L) splashStartMs = start
                    val elapsed = now - start
                    delay((SPLASH_DISPLAY_MS - elapsed).coerceAtLeast(0L))
                    if (authManager.sessionStore.read() != null) {
                        withContext(Dispatchers.IO) {
                            authManager.validateOrClearSession()
                        }
                    }
                    splashFinished = true
                }

                // Connect / disconnect the realtime WebSocket on auth state changes
                val realtimeManager = (application as FashApplication).realtimeManager
                val fashApp = application as FashApplication
                val notifPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { }

                LaunchedEffect(isAuthenticated) {
                    if (isAuthenticated) {
                        realtimeManager.connect()
                    } else {
                        realtimeManager.disconnect()
                        needsOnboarding = null
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

                LaunchedEffect(isAuthenticated) {
                    if (!isAuthenticated) return@LaunchedEffect
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
                    if (needsOnboarding != null) return@LaunchedEffect
                    val userRepo = (this@MainActivity.application as FashApplication).userRepository
                    needsOnboarding = withContext(Dispatchers.IO) {
                        computeNeedsOnboarding(userRepo, authManager.sessionStore)
                    }
                }

                LaunchedEffect(Unit) {
                    onboardingViewModel.events.collect { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (splashFinished) {
                        Box(Modifier.fillMaxSize()) {
                            when {
                                isLoggingOut -> FashWaitingScreen()
                                isAuthenticated && needsOnboarding == null -> FashWaitingScreen()
                                isAuthenticated && needsOnboarding == true -> {
                                    LaunchedEffect(Unit) {
                                        if (onboardingStep == OnboardingStep.StyleSelection) {
                                            onboardingViewModel.loadTags()
                                        }
                                    }
                                    when (onboardingStep) {
                                        OnboardingStep.StyleSelection -> OnboardingScreen(
                                            tags = onboardingTags,
                                            selectedIds = onboardingSelected,
                                            isLoading = onboardingLoading,
                                            isSubmitting = onboardingSubmitting,
                                            progressStep = 2,
                                            progressTotal = 3,
                                            onToggleSelection = onboardingViewModel::toggleSelection,
                                            onContinue = {
                                                onboardingViewModel.goToProfileSetup(
                                                    onboardingViewModel.generateUsernameFromEmail(loginViewModel.email.value),
                                                )
                                            },
                                            onSkip = {
                                                onboardingViewModel.skipToProfileSetup(loginViewModel.email.value)
                                            },
                                            onBack = {
                                                authManager.sessionStore.clear()
                                                authManager.onSessionCleared()
                                            },
                                        )
                                        OnboardingStep.ProfileSetup -> ProfileSetupScreen(
                                            username = onboardingUsername,
                                            onUsernameChange = onboardingViewModel::onUsernameChange,
                                            isUsernameValid = onboardingViewModel.isUsernameValid(),
                                            isSubmitting = onboardingSubmitting,
                                            progressStep = 3,
                                            progressTotal = 3,
                                            onComplete = {
                                                onboardingViewModel.submitOnboard {
                                                    authManager.sessionStore.read()?.let { s ->
                                                        authManager.sessionStore.save(
                                                            s.copy(isNewUser = false),
                                                        )
                                                    }
                                                    needsOnboarding = false
                                                }
                                            },
                                            onBack = onboardingViewModel::goBackToStyle,
                                        )
                                    }
                                }
                                isAuthenticated -> {
                                    var selectedListingId by rememberSaveable { mutableStateOf<String?>(null) }
                                    var editListingId by rememberSaveable { mutableStateOf<String?>(null) }
                                    var showEditProfile by rememberSaveable { mutableStateOf(false) }
                                    var selectedConversationId by rememberSaveable { mutableStateOf<String?>(null) }
                                    var selectedConversationItem by remember { mutableStateOf<ConversationItem?>(null) }
                                    var selectedCheckoutListingId by rememberSaveable { mutableStateOf<String?>(null) }
                                    var selectedCheckoutOfferPrice by rememberSaveable { mutableStateOf(0L) }
                                    var checkoutExistingOrderId by rememberSaveable { mutableStateOf<String?>(null) }
                                    var selectedOrderId by rememberSaveable { mutableStateOf<String?>(null) }
                                    var addressFlowOrderId by rememberSaveable { mutableStateOf<String?>(null) }
                                    var showShippingAddressList by rememberSaveable { mutableStateOf(false) }
                                    var showAddAddressScreen by rememberSaveable { mutableStateOf(false) }
                                    var addAddressOpenedFromList by rememberSaveable { mutableStateOf(false) }
                                    var showOrdersScreen by rememberSaveable { mutableStateOf(false) }
                                    var selectedTab by rememberSaveable { mutableIntStateOf(MainTab.Home.ordinal) }
                                    val scope = rememberCoroutineScope()
                                    val context = LocalContext.current
                                    LaunchedEffect(Unit) {
                                        editListingViewModel.events.collect { msg ->
                                            snackbarHostState.showSnackbar(msg)
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
                                    val orderRepository = remember {
                                        (context.applicationContext as FashApplication).orderRepository
                                    }
                                    val chatUnreadCount by chatViewModel.unreadBadgeCount.collectAsState()
                                    val chatOrderId by chatDetailViewModel.orderId.collectAsState()
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        MainNavScreen(
                                            onLogout = loginViewModel::logout,
                                            onLogoutAll = loginViewModel::logoutAll,
                                            isLoggingOut = isLoggingOut,
                                            homeViewModel = homeViewModel,
                                            exploreViewModel = exploreViewModel,
                                            postViewModel = postViewModel,
                                            profileViewModel = profileViewModel,
                                            chatViewModel = chatViewModel,
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
                                            onOrdersClick = { showOrdersScreen = true },
                                            onConversationClick = { item ->
                                                selectedConversationItem = item
                                                chatDetailViewModel.loadFromItem(item)
                                                selectedConversationId = item.conversationId
                                            },
                                            selectedTab = selectedTab,
                                            onTabChange = { selectedTab = it },
                                        )
                                        if (selectedListingId != null) {
                                            ProductDetailScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                listingId = selectedListingId!!,
                                                viewModel = productDetailViewModel,
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
                                                                selectedConversationId = convId
                                                                chatViewModel.loadConversations()
                                                            },
                                                            onFailure = {
                                                                productDetailViewModel.setOpeningChat(false)
                                                                selectedListingId = null
                                                                selectedTab = MainTab.Chat.ordinal
                                                                snackbarHostState.showSnackbar(
                                                                    it.message ?: getString(R.string.chat_load_error),
                                                                )
                                                            },
                                                        )
                                                    }
                                                },
                                                onBuyNow = { listingId ->
                                                    scope.launch {
                                                        val price =
                                                            productDetailViewModel.detail.value?.priceVnd ?: 0L
                                                        if (price <= 0L) {
                                                            snackbarHostState.showSnackbar(
                                                                getString(R.string.feed_action_error),
                                                            )
                                                            return@launch
                                                        }
                                                        val result = withContext(Dispatchers.IO) {
                                                            orderRepository.createOrder(listingId, price)
                                                        }
                                                        result.fold(
                                                            onSuccess = { oid ->
                                                                selectedListingId = null
                                                                selectedOrderId = oid
                                                            },
                                                            onFailure = {
                                                                snackbarHostState.showSnackbar(
                                                                    it.message
                                                                        ?: getString(R.string.feed_action_error),
                                                                )
                                                            },
                                                        )
                                                    }
                                                },
                                                onShare = { /* TODO: share */ },
                                                onListingClick = { lid, sellerId ->
                                                    val myId = authManager.sessionStore.read()?.userId?.trim().orEmpty()
                                                    if (!sellerId.isNullOrBlank() && sellerId == myId) {
                                                        selectedListingId = null
                                                        editListingId = lid
                                                    } else {
                                                        selectedListingId = lid
                                                    }
                                                },
                                            )
                                        }
                                        if (editListingId != null) {
                                            BackHandler { editListingId = null }
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
                                                onBack = {
                                                    selectedConversationId = null
                                                    selectedConversationItem = null
                                                },
                                                onProductClick = {
                                                    selectedConversationId = null
                                                    selectedListingId = it
                                                },
                                                onCheckout = { listingId, offerAmount ->
                                                    selectedCheckoutListingId = listingId
                                                    selectedCheckoutOfferPrice = offerAmount
                                                    checkoutExistingOrderId = chatOrderId
                                                },
                                                onPayNow = { orderId, _, _ ->
                                                    selectedConversationId = null
                                                    selectedConversationItem = null
                                                    selectedOrderId = orderId
                                                },
                                                onOrderDetails = { orderId ->
                                                    selectedConversationId = null
                                                    selectedConversationItem = null
                                                    selectedOrderId = orderId
                                                },
                                            )
                                        }
                                        if (selectedOrderId != null) {
                                            OrderDetailScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                orderId = selectedOrderId!!,
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
                                            )
                                        }
                                        if (showShippingAddressList && addressFlowOrderId != null && !showAddAddressScreen) {
                                            BackHandler {
                                                showShippingAddressList = false
                                                addressFlowOrderId = null
                                            }
                                            ShippingAddressListScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                orderId = addressFlowOrderId!!,
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
                                                    showShippingAddressList = false
                                                    addressFlowOrderId = null
                                                },
                                            )
                                        }
                                        if (showAddAddressScreen) {
                                            BackHandler {
                                                val fromList = addAddressOpenedFromList
                                                showAddAddressScreen = false
                                                if (fromList) {
                                                    addAddressOpenedFromList = false
                                                } else {
                                                    addressFlowOrderId = null
                                                }
                                            }
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
                                                    orderDetailViewModel.load(paidOrderId)
                                                },
                                            )
                                        }
                                        if (showOrdersScreen) {
                                            com.pc.fash_android_mobile.ui.orders.OrdersScreen(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surface),
                                                viewModel = ordersViewModel,
                                                onBack = { showOrdersScreen = false },
                                                onOrderClick = { order ->
                                                    showOrdersScreen = false
                                                    selectedOrderId = order.orderId
                                                },
                                            )
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
                                    onTermsClick = { openUrl("https://example.com/terms") },
                                    onPrivacyClick = { openUrl("https://example.com/privacy") },
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
                                    showOnboardingProgress = true,
                                    onboardingProgressStep = 1,
                                    onboardingProgressTotal = 3,
                                )
                            }
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .navigationBarsPadding(),
                            )
                        }
                    } else {
                        FashWaitingScreen()
                    }
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
