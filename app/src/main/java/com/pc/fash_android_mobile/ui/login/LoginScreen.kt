package com.pc.fash_android_mobile.ui.login

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.advertising.AppAdvertisingSlideItem
import com.pc.fash_android_mobile.data.onboarding.AppWelcomeIntroStore
import com.pc.fash_android_mobile.data.onboarding.PreLoginMascotGuideStore
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import com.pc.fash_android_mobile.ui.onboarding.PreLoginMascotGuideContext
import com.pc.fash_android_mobile.ui.onboarding.PreLoginMascotGuideOverlay
import com.pc.fash_android_mobile.ui.onboarding.FeatureTourAnchor
import com.pc.fash_android_mobile.ui.onboarding.guideSpotlightAnchor
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.locale.LoginLanguageToggle
import com.pc.fash_android_mobile.ui.components.FashBrandMarkText
import com.pc.fash_android_mobile.ui.components.FashSnackbarHost
import com.pc.fash_android_mobile.ui.components.FashPrimaryButton
import com.pc.fash_android_mobile.ui.theme.FashBrandTypography
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn
import com.pc.fash_android_mobile.ui.theme.FashTheme

private val HeroCornerDp = 28.dp
private val FieldCornerDp = 16.dp
private val SocialCornerDp = 16.dp
private val PillCornerDp = 24.dp
private const val OTP_SHADOW_DP = 6

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    email: String,
    onEmailChange: (String) -> Unit,
    isOtpLoading: Boolean,
    isSocialLoading: Boolean = false,
    snackbarHostState: SnackbarHostState,
    /** When false, host the snackbar in a parent (e.g. auth flow root). */
    showSnackbarHost: Boolean = true,
    onSendOtp: () -> Unit,
    onGoogleClick: () -> Unit,
    onFacebookClick: () -> Unit,
    isGoogleConfigured: Boolean,
    showFacebookLogin: Boolean = true,
    isFacebookConfigured: Boolean,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    usePasswordLogin: Boolean = false,
    onTogglePasswordLogin: (() -> Unit)? = null,
    password: String = "",
    onPasswordChange: (String) -> Unit = {},
    onLoginWithPassword: (() -> Unit)? = null,
    isPasswordLoading: Boolean = false,
    remoteSlides: List<AppAdvertisingSlideItem> = emptyList(),
    /** When set (and public browse is configured), offers guest Home/Explore without sign-in. */
    onContinueWithoutAccount: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val appContext = LocalContext.current.applicationContext
    var showPreLoginGuide by rememberSaveable {
        mutableStateOf(!PreLoginMascotGuideStore.isCompleted(appContext))
    }
    val preLoginAnchors = remember { mutableStateMapOf<FeatureTourAnchor, LayoutCoordinates>() }
    val guideAnchorsEnabled = showPreLoginGuide && AppWelcomeIntroStore.isCompleted(appContext)
    val onPreLoginAnchorPositioned: (FeatureTourAnchor, LayoutCoordinates?) -> Unit = { key, coords ->
        val c = coords?.takeIf { it.isAttached }
        if (c == null) preLoginAnchors.remove(key) else preLoginAnchors[key] = c
    }
    val formLockedForSocial = isSocialLoading
    val signingInLabel = stringResource(R.string.login_social_signing_in)
    val emailValid = isValidEmail(email)
    val passwordValid = password.isNotBlank()

    val brandAnim = remember { Animatable(0f) }
    val heroAnim = remember { Animatable(0f) }
    val formAnim = remember { Animatable(0f) }
    val bottomAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { brandAnim.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
        launch {
            delay(50)
            heroAnim.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        }
        launch {
            delay(100)
            formAnim.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        }
        launch {
            delay(150)
            bottomAnim.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = scheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(start = FashTheme.spacing.editorialStart, end = FashTheme.spacing.editorialEnd)
                    .padding(top = 12.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LoginLanguageToggle()
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        alpha = brandAnim.value
                        translationY = (1f - brandAnim.value) * 20f
                    },
                ) {
                    FashBrandMarkText(
                        text = stringResource(R.string.brand_wordmark),
                        style = FashBrandTypography.markBoldItalicLarge,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.login_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                LoginHeroCarouselSection(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 120.dp)
                        .graphicsLayer {
                            alpha = heroAnim.value
                            translationY = (1f - heroAnim.value) * 24f
                        },
                    remoteSlides = remoteSlides,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = formAnim.value
                            translationY = (1f - formAnim.value) * 22f
                        }
                        .guideSpotlightAnchor(
                            anchor = FeatureTourAnchor.LoginEmailForm,
                            enabled = guideAnchorsEnabled,
                            onPositioned = onPreLoginAnchorPositioned,
                        ),
                ) {
                EmailFieldWithRail(
                    email = email,
                    onEmailChange = onEmailChange,
                    enabled = !formLockedForSocial,
                    onDone = {
                        if (usePasswordLogin) {
                            if (emailValid && passwordValid && !isPasswordLoading && !formLockedForSocial) {
                                onLoginWithPassword?.invoke()
                            }
                        } else {
                            if (emailValid && !isOtpLoading && !formLockedForSocial) onSendOtp()
                        }
                    },
                )

                if (usePasswordLogin) {
                    Spacer(modifier = Modifier.height(10.dp))
                    PasswordFieldWithRail(
                        password = password,
                        onPasswordChange = onPasswordChange,
                        enabled = !formLockedForSocial,
                        onDone = {
                            if (emailValid && passwordValid && !isPasswordLoading && !formLockedForSocial) {
                                onLoginWithPassword?.invoke()
                            }
                        },
                    )
                }

                if (onTogglePasswordLogin != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (usePasswordLogin) {
                            stringResource(R.string.login_use_otp_instead)
                        } else {
                            stringResource(R.string.login_with_password)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDecoration = TextDecoration.Underline,
                        ),
                        color = scheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(4.dp)
                            .clickable(
                                enabled = !formLockedForSocial,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onTogglePasswordLogin!!() },
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                FashPrimaryButton(
                    onClick = when {
                        usePasswordLogin -> onLoginWithPassword ?: {}
                        else -> onSendOtp
                    },
                    enabled = when {
                        usePasswordLogin -> emailValid && passwordValid && !isPasswordLoading && !formLockedForSocial
                        else -> emailValid && !isOtpLoading && !formLockedForSocial
                    },
                    horizontalArrangement = Arrangement.Center,
                    cornerRadius = PillCornerDp,
                    solidFill = scheme.primary,
                    softShadowElevation = OTP_SHADOW_DP.dp,
                ) {
                    if (isOtpLoading || isPasswordLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = FashColors.Primary.fashReadableOn(),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = when {
                                usePasswordLogin -> stringResource(R.string.login_submit)
                                else -> stringResource(R.string.login_send_otp)
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                }

                Column(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = bottomAnim.value
                            translationY = (1f - bottomAnim.value) * 18f
                        }
                        .guideSpotlightAnchor(
                            anchor = FeatureTourAnchor.LoginSocialRow,
                            enabled = guideAnchorsEnabled,
                            onPositioned = onPreLoginAnchorPositioned,
                        ),
                ) {
                    val hasSocialButtons = isGoogleConfigured || showFacebookLogin
                    Spacer(modifier = Modifier.height(12.dp))

                    if (hasSocialButtons) {
                        OrDivider()

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            SocialOutlineButton(
                                modifier = Modifier.then(
                                    if (!isGoogleConfigured) Modifier.alpha(0.55f) else Modifier,
                                ),
                                iconRes = R.drawable.ic_brand_google,
                                label = stringResource(R.string.login_google),
                                enabled = !isSocialLoading,
                                onClick = onGoogleClick,
                            )
                            if (showFacebookLogin) {
                                SocialOutlineButton(
                                    modifier = Modifier.then(
                                        if (!isFacebookConfigured) Modifier.alpha(0.55f) else Modifier,
                                    ),
                                    iconRes = R.drawable.ic_brand_facebook,
                                    label = stringResource(R.string.login_facebook),
                                    enabled = !isSocialLoading,
                                    onClick = onFacebookClick,
                                )
                            }
                        }
                    }

                    if (onContinueWithoutAccount != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = onContinueWithoutAccount,
                            modifier = Modifier
                                .fillMaxWidth()
                                .guideSpotlightAnchor(
                                    anchor = FeatureTourAnchor.LoginGuestBrowse,
                                    enabled = guideAnchorsEnabled,
                                    onPositioned = onPreLoginAnchorPositioned,
                                ),
                            enabled = !isSocialLoading && !isOtpLoading,
                        ) {
                            Text(stringResource(R.string.login_continue_without_account))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LoginLegalFooter(
                        onTermsClick = onTermsClick,
                        onPrivacyClick = onPrivacyClick,
                    )
                }
            }

            if (showSnackbarHost) {
                FashSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }
            if (isSocialLoading) {
                val soakClicks = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.38f))
                        .clickable(
                            interactionSource = soakClicks,
                            indication = null,
                        ) { },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 28.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(44.dp)
                                .semantics { contentDescription = signingInLabel },
                            strokeWidth = 3.dp,
                            color = scheme.primary,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = signingInLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.92f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            if (guideAnchorsEnabled) {
                PreLoginMascotGuideOverlay(
                    context = PreLoginMascotGuideContext.LoginScreen,
                    anchors = preLoginAnchors,
                    onFinish = { showPreLoginGuide = false },
                )
            }
        }
    }
}

private data class LoginHeroSlide(
    val title: String,
    val subtitle: String,
    val badgeLabel: String,
    val bannerImageUrl: String?,
    val scrim: Brush,
    val fallbackCaptionRes: Int? = null,
)

private const val LoginHeroAutoAdvanceMs = 5_500L

private fun loginHeroScrim(stylePreset: String, scheme: ColorScheme): Brush {
    return when (stylePreset.trim()) {
        "gradient_warm" -> Brush.verticalGradient(
            listOf(
                FashColors.TertiaryAccent.copy(alpha = 0.08f),
                Color.Transparent,
                FashColors.Primary.copy(alpha = 0.10f),
            ),
        )
        "gradient_neutral" -> Brush.verticalGradient(
            listOf(
                scheme.surfaceContainerLow.copy(alpha = 0.10f),
                Color.Transparent,
                scheme.surfaceContainerHighest.copy(alpha = 0.30f),
            ),
        )
        else -> Brush.verticalGradient(
            listOf(Color.Transparent, FashColors.Primary.copy(alpha = 0.12f)),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LoginHeroCarouselSection(
    modifier: Modifier = Modifier,
    remoteSlides: List<AppAdvertisingSlideItem> = emptyList(),
) {
    val scheme = MaterialTheme.colorScheme
    val localSlides = remember(scheme) {
        listOf(
            LoginHeroSlide(
                title = "",
                subtitle = "",
                badgeLabel = "",
                bannerImageUrl = null,
                scrim = loginHeroScrim("gradient_primary", scheme),
                fallbackCaptionRes = R.string.login_hero_slide1_caption,
            ),
            LoginHeroSlide(
                title = "",
                subtitle = "",
                badgeLabel = "",
                bannerImageUrl = null,
                scrim = loginHeroScrim("gradient_warm", scheme),
                fallbackCaptionRes = R.string.login_hero_slide2_caption,
            ),
            LoginHeroSlide(
                title = "",
                subtitle = "",
                badgeLabel = "",
                bannerImageUrl = null,
                scrim = loginHeroScrim("gradient_neutral", scheme),
                fallbackCaptionRes = R.string.login_hero_slide3_caption,
            ),
        )
    }
    val slides = remember(remoteSlides, scheme) {
        val cmsSlides = remoteSlides.mapNotNull { item ->
            val title = item.title.trim()
            val subtitle = item.subtitle.trim()
            val badge = item.badgeLabel.trim()
            val imageUrl = item.bannerImageUrl.trim().ifEmpty { null }
            if (title.isBlank() && subtitle.isBlank() && imageUrl.isNullOrBlank()) return@mapNotNull null
            LoginHeroSlide(
                title = title,
                subtitle = subtitle,
                badgeLabel = badge,
                bannerImageUrl = imageUrl,
                scrim = loginHeroScrim(item.stylePreset, scheme),
                fallbackCaptionRes = null,
            )
        }
        if (cmsSlides.isEmpty()) localSlides else cmsSlides
    }
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val infiniteTransition = rememberInfiniteTransition(label = "loginHeroFloat")
    val floatY = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heroFloatY",
    )

    LaunchedEffect(pagerState) {
        while (isActive) {
            delay(LoginHeroAutoAdvanceMs)
            val next = (pagerState.currentPage + 1) % slides.size
            runCatching { pagerState.animateScrollToPage(next) }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(HeroCornerDp),
            color = scheme.surfaceContainer,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) { page ->
                val slide = slides[page]
                val titleText = if (slide.title.isNotBlank()) {
                    slide.title
                } else {
                    slide.fallbackCaptionRes?.let { stringResource(it) }.orEmpty()
                }
                val slideContentDescription = stringResource(
                    R.string.login_hero_pager_cd,
                    page + 1,
                    slides.size,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics(mergeDescendants = true) {
                            contentDescription = slideContentDescription
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(scheme.surfaceContainerLow),
                    )
                    if (!slide.bannerImageUrl.isNullOrBlank()) {
                        FashAsyncImage(
                            model = slide.bannerImageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { translationY = floatY.value * 0.6f },
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.login_hero_trench),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .graphicsLayer { translationY = floatY.value },
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(slide.scrim),
                    )
                    if (slide.badgeLabel.isNotBlank()) {
                        Text(
                            text = slide.badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.primary,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .background(
                                    color = scheme.surface.copy(alpha = 0.90f),
                                    shape = RoundedCornerShape(999.dp),
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                    ) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface.copy(alpha = 0.90f),
                            maxLines = 2,
                        )
                        if (slide.subtitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = slide.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurface.copy(alpha = 0.80f),
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        }
        LoginHeroPageIndicator(
            pageCount = slides.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun LoginHeroPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (selected) 18.dp else 6.dp,
                animationSpec = tween(durationMillis = 220),
                label = "loginHeroDot",
            )
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (selected) FashColors.Primary else scheme.outlineVariant.copy(alpha = 0.7f),
                    ),
            )
        }
    }
}

@Composable
private fun EmailFieldWithRail(
    email: String,
    onEmailChange: (String) -> Unit,
    enabled: Boolean = true,
    onDone: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val railShape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 8.dp, bottomEnd = 8.dp)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.login_email_label),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    stringResource(R.string.login_email_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.55f),
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp, top = 8.dp, bottom = 8.dp)
                        .width(52.dp)
                        .background(color = scheme.surfaceContainerLow, shape = railShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = stringResource(R.string.login_email_icon_cd),
                        modifier = Modifier.size(22.dp),
                        tint = scheme.onSurface.copy(alpha = 0.48f),
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(FieldCornerDp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = scheme.surfaceContainerHighest,
                unfocusedContainerColor = scheme.surfaceContainerHighest,
                disabledContainerColor = scheme.surfaceContainerHighest,
                focusedBorderColor = scheme.outline.copy(alpha = 0.55f),
                unfocusedBorderColor = scheme.outlineVariant.copy(alpha = 0.92f),
                cursorColor = scheme.primary,
                focusedLeadingIconColor = scheme.onSurfaceVariant,
                unfocusedLeadingIconColor = scheme.onSurfaceVariant,
            ),
        )
    }
}

@Composable
private fun PasswordFieldWithRail(
    password: String,
    onPasswordChange: (String) -> Unit,
    enabled: Boolean = true,
    onDone: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val railShape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 8.dp, bottomEnd = 8.dp)
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.login_password_label),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    stringResource(R.string.login_password_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.55f),
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp, top = 8.dp, bottom = 8.dp)
                        .width(52.dp)
                        .background(color = scheme.surfaceContainerLow, shape = railShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = scheme.onSurface.copy(alpha = 0.48f),
                    )
                }
            },
            trailingIcon = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    enabled = enabled,
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = stringResource(
                            if (passwordVisible) {
                                R.string.login_password_hide_cd
                            } else {
                                R.string.login_password_show_cd
                            },
                        ),
                        tint = scheme.onSurfaceVariant,
                    )
                }
            },
            singleLine = true,
            // Match [ChangePasswordScreen]: default mask (bullet) — custom '.' can render invisible on some fonts.
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            shape = RoundedCornerShape(FieldCornerDp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = scheme.surfaceContainerHighest,
                unfocusedContainerColor = scheme.surfaceContainerHighest,
                disabledContainerColor = scheme.surfaceContainerHighest,
                focusedBorderColor = scheme.outline.copy(alpha = 0.55f),
                unfocusedBorderColor = scheme.outlineVariant.copy(alpha = 0.92f),
                cursorColor = scheme.primary,
                focusedLeadingIconColor = scheme.onSurfaceVariant,
                unfocusedLeadingIconColor = scheme.onSurfaceVariant,
                focusedTrailingIconColor = scheme.onSurfaceVariant,
                unfocusedTrailingIconColor = scheme.onSurfaceVariant,
            ),
        )
    }
}

@Composable
private fun OrDivider() {
    val scheme = MaterialTheme.colorScheme
    val lineColor = scheme.outlineVariant.copy(alpha = 0.7f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = lineColor, thickness = 1.dp)
        Text(
            text = stringResource(R.string.login_or_continue),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant.copy(alpha = 0.85f),
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = lineColor, thickness = 1.dp)
    }
}

@Composable
private fun SocialOutlineButton(
    modifier: Modifier = Modifier,
    iconRes: Int,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(FashTheme.spacing.buttonHeight),
        shape = RoundedCornerShape(SocialCornerDp),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.52f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = scheme.surfaceContainerHighest,
            contentColor = scheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.width(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun LoginLegalFooter(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val linkColor = scheme.primary
    val baseStyle = MaterialTheme.typography.bodySmall.copy(
        color = scheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    val prefix = stringResource(R.string.login_legal_prefix)
    val terms = stringResource(R.string.login_terms)
    val mid = stringResource(R.string.login_legal_and)
    val privacy = stringResource(R.string.login_privacy)

    val linkStyle = TextLinkStyles(
        style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
    )
    val annotated = buildAnnotatedString {
        append(prefix)
        withLink(
            LinkAnnotation.Clickable(
                tag = "terms",
                styles = linkStyle,
                linkInteractionListener = { onTermsClick() },
            ),
        ) {
            append(terms)
        }
        append(mid)
        withLink(
            LinkAnnotation.Clickable(
                tag = "privacy",
                styles = linkStyle,
                linkInteractionListener = { onPrivacyClick() },
            ),
        ) {
            append(privacy)
        }
    }

    Text(
        text = annotated,
        style = baseStyle,
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true, locale = "vi")
@Composable
private fun LoginScreenPreviewVi() {
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
            usePasswordLogin = false,
            onTogglePasswordLogin = {},
            password = "",
            onPasswordChange = {},
            onLoginWithPassword = {},
        )
    }
}
