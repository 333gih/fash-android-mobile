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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.pc.fash_android_mobile.ui.locale.LoginLanguageToggle
import com.pc.fash_android_mobile.ui.components.FashBrandMarkText
import com.pc.fash_android_mobile.ui.components.FashPrimaryButton
import com.pc.fash_android_mobile.ui.theme.FashBrandTypography
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn
import com.pc.fash_android_mobile.ui.theme.FashTheme

private val LoginCanvas = Color.White
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
    isFacebookConfigured: Boolean,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    usePasswordLogin: Boolean = false,
    onTogglePasswordLogin: (() -> Unit)? = null,
    password: String = "",
    onPasswordChange: (String) -> Unit = {},
    onLoginWithPassword: (() -> Unit)? = null,
    isPasswordLoading: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
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
        color = LoginCanvas,
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
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.graphicsLayer {
                        alpha = formAnim.value
                        translationY = (1f - formAnim.value) * 22f
                    },
                ) {
                EmailFieldWithRail(
                    email = email,
                    onEmailChange = onEmailChange,
                    onDone = {
                        if (usePasswordLogin) {
                            if (emailValid && passwordValid && !isPasswordLoading) onLoginWithPassword?.invoke()
                        } else {
                            if (emailValid && !isOtpLoading) onSendOtp()
                        }
                    },
                )

                if (usePasswordLogin) {
                    Spacer(modifier = Modifier.height(10.dp))
                    PasswordFieldWithRail(
                        password = password,
                        onPasswordChange = onPasswordChange,
                        onDone = { if (emailValid && passwordValid && !isPasswordLoading) onLoginWithPassword?.invoke() },
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
                            .clickable(onClick = onTogglePasswordLogin),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                FashPrimaryButton(
                    onClick = when {
                        usePasswordLogin -> onLoginWithPassword ?: {}
                        else -> onSendOtp
                    },
                    enabled = when {
                        usePasswordLogin -> emailValid && passwordValid && !isPasswordLoading
                        else -> emailValid && !isOtpLoading
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
                    modifier = Modifier.graphicsLayer {
                        alpha = bottomAnim.value
                        translationY = (1f - bottomAnim.value) * 18f
                    },
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

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

                    Spacer(modifier = Modifier.height(8.dp))

                    LoginLegalFooter(
                        onTermsClick = onTermsClick,
                        onPrivacyClick = onPrivacyClick,
                    )
                }
            }

            if (showSnackbarHost) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                )
            }
        }
    }
}

private data class LoginHeroSlide(
    val captionRes: Int,
    val scrim: Brush,
)

private const val LoginHeroAutoAdvanceMs = 5_500L

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LoginHeroCarouselSection(
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val slides = remember(scheme) {
        listOf(
            LoginHeroSlide(
                captionRes = R.string.login_hero_slide1_caption,
                scrim = Brush.verticalGradient(
                    listOf(Color.Transparent, FashColors.Primary.copy(alpha = 0.09f)),
                ),
            ),
            LoginHeroSlide(
                captionRes = R.string.login_hero_slide2_caption,
                scrim = Brush.verticalGradient(
                    listOf(
                        FashColors.TertiaryAccent.copy(alpha = 0.05f),
                        Color.Transparent,
                        FashColors.Primary.copy(alpha = 0.075f),
                    ),
                ),
            ),
            LoginHeroSlide(
                captionRes = R.string.login_hero_slide3_caption,
                scrim = Brush.verticalGradient(
                    listOf(Color.Transparent, FashColors.SecondaryContainer.copy(alpha = 0.42f)),
                ),
            ),
        )
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
                            .background(slide.scrim),
                    )
                    Image(
                        painter = painterResource(R.drawable.login_hero_trench),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .graphicsLayer { translationY = floatY.value },
                        contentScale = ContentScale.Fit,
                    )
                    Text(
                        text = stringResource(slide.captionRes),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = scheme.onSurface.copy(alpha = 0.82f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                    )
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
    onDone: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val railShape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 8.dp, bottomEnd = 8.dp)

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
            singleLine = true,
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
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.9f)),
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
