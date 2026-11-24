package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.safePainterResource
import com.example.ui.theme.CobaltPulse
import com.example.ui.theme.CobaltPulseSoft
import com.example.ui.theme.MutedFilmText
import com.example.ui.theme.ObsidianStudio
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ShutterAmber
import com.example.ui.theme.SignalCrimson
import com.example.ui.theme.SlateEmulsion
import com.example.ui.theme.SlateHairline

@Composable
fun WelcomeChoiceScreen(
    onExistingUserClick: () -> Unit,
    onNewUserClick: () -> Unit,
    onContinueAsGuest: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianStudio)
            .drawBehind {
                // Top-left Cobalt optical glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CobaltPulse.copy(alpha = 0.24f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.22f, size.height * 0.16f),
                        radius = size.width * 0.72f
                    ),
                    radius = size.width * 0.72f,
                    center = Offset(size.width * 0.22f, size.height * 0.16f)
                )
                // Center-right Shutter Amber darkroom glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ShutterAmber.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.82f, size.height * 0.34f),
                        radius = size.width * 0.65f
                    ),
                    radius = size.width * 0.65f,
                    center = Offset(size.width * 0.82f, size.height * 0.34f)
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("welcome_choice_screen")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            // Top Brand & Editorial Contact-Sheet Showcase
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
            ) {
                // Studio Eyebrow Pill
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = SlateHairline,
                        shape = RoundedCornerShape(50)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(ShutterAmber)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PULSE STUDIO · ANALOG & VISUAL SOCIAL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.1.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Asymmetric Editorial Contact-Sheet Triptych
                PulseEditorialShowcaseHeader()

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Welcome to Pulse",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 28.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A curated darkroom for analog photography, optical film stocks, and visual storytelling.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Cards Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
            ) {
                // Option 1: Existing Member -> Sign In
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    CobaltPulse.copy(alpha = 0.24f),
                                    Color(0xFF121826),
                                    Color(0xFF0D121C)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    CobaltPulse.copy(alpha = 0.85f),
                                    CobaltPulse.copy(alpha = 0.25f)
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable(onClick = onExistingUserClick)
                        .testTag("choice_existing_user_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CobaltPulse.copy(alpha = 0.22f))
                                .border(
                                    1.dp,
                                    CobaltPulse.copy(alpha = 0.45f),
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Login,
                                contentDescription = "Existing User",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Existing User",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = CobaltPulse.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "CONTINUE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF93C5FD),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Already logged in? Continue directly to your feed, darkroom presets & saved collections",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                lineHeight = 17.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CobaltPulse)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Go to Login",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2: New Creator -> Create Account
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    ShutterAmber.copy(alpha = 0.18f),
                                    Color(0xFF141822),
                                    Color(0xFF0D111A)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    ShutterAmber.copy(alpha = 0.8f),
                                    ShutterAmber.copy(alpha = 0.22f)
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable(onClick = onNewUserClick)
                        .testTag("choice_new_user_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(ShutterAmber.copy(alpha = 0.18f))
                                .border(
                                    1.dp,
                                    ShutterAmber.copy(alpha = 0.45f),
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PersonAdd,
                                contentDescription = "New User",
                                tint = ShutterAmber,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "New User",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = ShutterAmber.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "JOIN STUDIO",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = ShutterAmber,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Claim your @handle, curate galleries & publish visual loops",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                lineHeight = 17.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ShutterAmber.copy(alpha = 0.22f))
                                .border(1.dp, ShutterAmber.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Create New Account",
                                tint = ShutterAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (onContinueAsGuest != null) {
                    Spacer(modifier = Modifier.height(18.dp))

                    Surface(
                        color = Color(0xFF111622).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(50)
                            )
                            .clickable(onClick = onContinueAsGuest)
                            .testTag("choice_guest_feed_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Explore,
                                contentDescription = null,
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Preview Curated Feed as Guest",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Darkroom Film Stock Signature
            Text(
                text = "KODACHROME 64 · TRI-X 400 · CINESTILL 800T",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                letterSpacing = 1.4.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PulseEditorialShowcaseHeader() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(146.dp)
    ) {
        // Left angled darkroom print (Kyoto Street)
        EditorialPrintCard(
            imageRes = R.drawable.img_post_kyoto_1794021771843,
            filmCode = "800T",
            rotationDegrees = -8f,
            modifier = Modifier
                .width(102.dp)
                .height(124.dp)
                .offset(x = (-72).dp, y = 6.dp)
        )

        // Right angled darkroom print (Dolomites Dawn)
        EditorialPrintCard(
            imageRes = R.drawable.img_post_alpine_1794021773292,
            filmCode = "TX4",
            rotationDegrees = 8f,
            modifier = Modifier
                .width(102.dp)
                .height(124.dp)
                .offset(x = 72.dp, y = 6.dp)
        )

        // Center hero print (Portrait Darkroom)
        EditorialPrintCard(
            imageRes = R.drawable.img_post_portrait_1794021773202,
            filmCode = "K64",
            rotationDegrees = 0f,
            isCenterHero = true,
            modifier = Modifier
                .width(118.dp)
                .height(138.dp)
        )

        // Floating App Logo Emblem overlapping bottom of center frame
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 8.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF0A0F1A))
                .border(2.5.dp, Color(0xFF070A0F), CircleShape)
                .testTag("pulse_app_logo_badge")
        ) {
            Image(
                painter = safePainterResource(R.drawable.img_launcher_icon_1794021768478),
                contentDescription = "Pulse Studio Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                CobaltPulse.copy(alpha = 0.85f),
                                ShutterAmber.copy(alpha = 0.85f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun EditorialPrintCard(
    imageRes: Int,
    filmCode: String,
    rotationDegrees: Float,
    isCenterHero: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF121722),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotationDegrees
            }
            .border(
                width = if (isCenterHero) 1.5.dp else 1.dp,
                color = if (isCenterHero) {
                    Color.White.copy(alpha = 0.28f)
                } else {
                    Color.White.copy(alpha = 0.12f)
                },
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = safePainterResource(imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.55f)
                            )
                        )
                    )
            )
            Surface(
                color = Color.Black.copy(alpha = 0.68f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = filmCode,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = ShutterAmber,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun AuthScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (email: String, password: String, username: String, displayName: String) -> Unit,
    onDismissError: () -> Unit,
    initialSignUpMode: Boolean = false,
    entryBannerText: String? = null,
    onBackToChoice: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isSignUpMode by remember(initialSignUpMode) { mutableStateOf(initialSignUpMode) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color(0xFFF8FAFC),
        focusedContainerColor = SlateEmulsion,
        unfocusedContainerColor = ObsidianSurface,
        focusedBorderColor = CobaltPulseSoft,
        unfocusedBorderColor = SlateHairline,
        focusedLabelColor = CobaltPulseSoft,
        unfocusedLabelColor = MutedFilmText,
        focusedLeadingIconColor = CobaltPulseSoft,
        unfocusedLeadingIconColor = MutedFilmText,
        focusedTrailingIconColor = Color(0xFFE2E8F0),
        unfocusedTrailingIconColor = MutedFilmText,
        cursorColor = ShutterAmber
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianStudio)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CobaltPulse.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.2f, size.height * 0.14f),
                        radius = size.width * 0.7f
                    ),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.2f, size.height * 0.14f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ShutterAmber.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.25f),
                        radius = size.width * 0.6f
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(size.width * 0.85f, size.height * 0.25f)
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("auth_screen")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
            ) {
                if (onBackToChoice != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Surface(
                            color = ObsidianSurface,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, SlateHairline, RoundedCornerShape(50))
                                .clickable {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    onBackToChoice()
                                }
                                .testTag("auth_back_to_choice_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFFE2E8F0),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Back",
                                    color = Color(0xFFE2E8F0),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                // Obsidian Studio App Logo Emblem
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0A0F1A))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    CobaltPulse.copy(alpha = 0.85f),
                                    ShutterAmber.copy(alpha = 0.85f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("auth_app_logo_badge")
                ) {
                    Image(
                        painter = safePainterResource(R.drawable.img_launcher_icon_1794021768478),
                        contentDescription = "Pulse Studio Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isSignUpMode) "Create Creator Account" else "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entryBannerText ?: if (isSignUpMode) {
                        "Set up your creator profile, darkroom presets & portfolio"
                    } else {
                        "Sign in to access your studio, feed & saved collections"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Surface(
                    color = Color(0xFF111723).copy(alpha = 0.94f),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(22.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Login / Create Account segmented selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF090D14))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                                .padding(4.dp)
                        ) {
                            Surface(
                                color = if (!isSignUpMode) CobaltPulse else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onDismissError()
                                        isSignUpMode = false
                                    }
                                    .testTag("auth_tab_sign_in")
                            ) {
                                Text(
                                    text = "Sign In",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isSignUpMode) Color.White else Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                            Surface(
                                color = if (isSignUpMode) CobaltPulse else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onDismissError()
                                        isSignUpMode = true
                                    }
                                    .testTag("auth_tab_sign_up")
                            ) {
                                Text(
                                    text = "New Account",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSignUpMode) Color.White else Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (errorMessage != null) {
                            Surface(
                                color = SignalCrimson.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                                    .border(
                                        1.dp,
                                        SignalCrimson.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = errorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFCA5A5),
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = onDismissError) {
                                        Text("Dismiss", color = Color.White)
                                    }
                                }
                            }
                        }

                        if (isSignUpMode) {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Display Name") },
                                placeholder = { Text("Sora Takahashi", color = Color(0xFF475569)) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Filled.Person, contentDescription = null)
                                },
                                singleLine = true,
                                colors = textFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_display_name_input")
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username Handle") },
                                placeholder = { Text("sora.studio", color = Color(0xFF475569)) },
                                leadingIcon = {
                                    Text(
                                        text = "@",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ShutterAmber,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                },
                                singleLine = true,
                                colors = textFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_username_input")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            placeholder = { Text("creator@pulse.studio", color = Color(0xFF475569)) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Filled.Email, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Filled.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) {
                                            Icons.Filled.VisibilityOff
                                        } else {
                                            Icons.Filled.Visibility
                                        },
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                if (isSignUpMode) {
                                    onSignUp(email, password, username, displayName)
                                } else {
                                    onSignIn(email, password)
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CobaltPulse,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 15.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_submit_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Text(
                                text = if (isSignUpMode) "Create Account" else "Sign In",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isSignUpMode) {
                                    "Already have an account?"
                                } else {
                                    "New to Pulse?"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                            TextButton(
                                onClick = {
                                    focusManager.clearFocus(force = true)
                                    onDismissError()
                                    isSignUpMode = !isSignUpMode
                                },
                                modifier = Modifier.testTag("auth_toggle_mode_button")
                            ) {
                                Text(
                                    text = if (isSignUpMode) "Sign In" else "Create Account",
                                    color = ShutterAmber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (onCancel != null) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    onCancel()
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_guest_continue_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = ShutterAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Explore Curated Feed",
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
