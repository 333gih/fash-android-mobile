package com.pc.fash_android_mobile.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.components.FashBrandMarkText
import com.pc.fash_android_mobile.ui.theme.FashBrandTypography
import com.pc.fash_android_mobile.ui.components.FashPrimaryButton
import com.pc.fash_android_mobile.ui.onboarding.OnboardingProgressBar
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlinx.coroutines.delay

private val OtpCanvas = Color.White
private const val OTP_LENGTH = 6
private val CellCorner = RoundedCornerShape(16.dp)
private val CellHeight = 56.dp
private const val OTP_SHADOW_DP = 6

@Composable
fun OtpVerifyScreen(
    modifier: Modifier = Modifier,
    email: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    resendCooldownSec: Int,
    isSendOtpLoading: Boolean,
    isVerifyLoading: Boolean,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    onBackClick: () -> Unit,
    showOnboardingProgress: Boolean = false,
    onboardingProgressStep: Int = 1,
    onboardingProgressTotal: Int = 3,
) {
    val scheme = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }
    val masked = remember(email) { maskEmailForDisplay(email) }
    val otpComplete = otp.length == OTP_LENGTH
    var showHelpCard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        delay(140)
        showHelpCard = true
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = OtpCanvas,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = FashTheme.spacing.editorialStart, end = FashTheme.spacing.editorialEnd)
                .padding(top = 8.dp, bottom = 32.dp),
        ) {
            val scroll = rememberScrollState()
            val canResend = resendCooldownSec <= 0 && !isSendOtpLoading

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scroll),
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.offset(x = (-8).dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.otp_back_cd),
                        tint = scheme.onSurface,
                    )
                }

                if (showOnboardingProgress) {
                    OnboardingProgressBar(
                        currentStep = onboardingProgressStep,
                        totalSteps = onboardingProgressTotal,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                FashBrandMarkText(
                    text = stringResource(R.string.brand_wordmark),
                    style = FashBrandTypography.markBoldItalic,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.otp_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.otp_subtitle, masked, OTP_LENGTH),
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(28.dp))

                OtpSixCells(
                    otp = otp,
                    onOtpChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(OTP_LENGTH)
                        onOtpChange(digits)
                    },
                    focusRequester = focusRequester,
                    onImeDone = { if (otpComplete && !isVerifyLoading && !isSendOtpLoading) onVerifyClick() },
                )

                Spacer(modifier = Modifier.height(28.dp))

                FashPrimaryButton(
                    onClick = onVerifyClick,
                    enabled = otpComplete && !isVerifyLoading && !isSendOtpLoading,
                    horizontalArrangement = Arrangement.Center,
                    cornerRadius = 24.dp,
                    solidFill = scheme.primary,
                    softShadowElevation = OTP_SHADOW_DP.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (isVerifyLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = FashColors.Primary.fashReadableOn(),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.otp_verify),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = FashColors.Primary.fashReadableOn(),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { if (canResend) onResendClick() },
                    ) {
                        if (isSendOtpLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = FashColors.Primary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.otp_resend_sending),
                                style = MaterialTheme.typography.labelLarge,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        } else {
                            Text(
                                text = if (resendCooldownSec > 0) {
                                    stringResource(R.string.otp_resend_wait, resendCooldownSec)
                                } else {
                                    stringResource(R.string.otp_resend)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = if (canResend) FashColors.Primary else scheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            AnimatedVisibility(
                visible = showHelpCard,
                enter = fadeIn(tween(380)) + slideInVertically(
                    animationSpec = tween(380),
                    initialOffsetY = { it / 5 },
                ),
            ) {
                OtpHelpBottomCard()
            }
        }
    }
}

@Composable
private fun OtpHelpBottomCard(
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusCard)
    val tips = listOf(
        R.string.otp_help_line1,
        R.string.otp_help_line2,
        R.string.otp_help_line3,
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = scheme.surfaceContainerHighest,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.75f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FashTheme.spacing.spacing4),
            horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing3),
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(68.dp)
                    .background(
                        FashColors.Primary.copy(alpha = 0.58f),
                        RoundedCornerShape(2.dp),
                    ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = FashColors.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.otp_help_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                    )
                }
                tips.forEach { lineRes ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "\u2022",
                            style = MaterialTheme.typography.bodySmall,
                            color = FashColors.Primary.copy(alpha = 0.72f),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Text(
                            text = stringResource(lineRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OtpSixCells(
    otp: String,
    onOtpChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onImeDone: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val outline = scheme.outlineVariant.copy(alpha = 0.88f)
    val activeIndex = when {
        otp.length >= OTP_LENGTH -> OTP_LENGTH - 1
        else -> otp.length
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(OTP_LENGTH) { index ->
                val char = otp.getOrNull(index)?.toString().orEmpty()
                val isActive = index == activeIndex
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(CellHeight),
                    shape = CellCorner,
                    color = scheme.surfaceContainerHighest,
                    border = BorderStroke(
                        width = if (isActive) 2.dp else 1.dp,
                        color = if (isActive) scheme.primary.copy(alpha = 0.85f) else outline,
                    ),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                            ),
                            color = scheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // FIX: was `fillMaxSize()` which made the invisible text field cover the entire
        // column below the OTP row (buttons, resend link), eating all touch events on
        // those elements.  Constrained to just the cell row height so only the OTP area
        // captures taps.
        BasicTextField(
            value = otp,
            onValueChange = onOtpChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(CellHeight)
                .focusRequester(focusRequester)
                .alpha(0f),
            singleLine = true,
            cursorBrush = SolidColor(Color.Transparent),
            textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onImeDone() }),
        )
    }
}

@Preview(showBackground = true, locale = "vi")
@Composable
private fun OtpVerifyScreenPreview() {
    FashTheme {
        OtpVerifyScreen(
            email = "user@example.com",
            otp = "12",
            onOtpChange = {},
            resendCooldownSec = 42,
            isSendOtpLoading = false,
            isVerifyLoading = false,
            onVerifyClick = {},
            onResendClick = {},
            onBackClick = {},
        )
    }
}