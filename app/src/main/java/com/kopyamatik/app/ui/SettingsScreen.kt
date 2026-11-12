package com.kopyamatik.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kopyamatik.app.ui.theme.AccentBlue
import com.kopyamatik.app.ui.theme.BorderLight
import com.kopyamatik.app.ui.theme.CardBackground
import com.kopyamatik.app.ui.theme.CharcoalDark
import com.kopyamatik.app.ui.theme.ErrorRed
import com.kopyamatik.app.ui.theme.PureWhite
import com.kopyamatik.app.ui.theme.SlateGray
import com.kopyamatik.app.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settingsState.collectAsState()
    val uiState by viewModel.settingsUiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    var geminiKey by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var fishKey by remember(settings.fishAudioApiKey) { mutableStateOf(settings.fishAudioApiKey) }
    var voiceId by remember(settings.fishAudioVoiceId) { mutableStateOf(settings.fishAudioVoiceId) }

    var isGeminiKeyVisible by remember { mutableStateOf(false) }
    var isFishKeyVisible by remember { mutableStateOf(false) }

    // Toggle for Visual Guide
    var isGuideExpanded by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState) {
        when (uiState) {
            is SettingsUiState.Success -> {
                snackbarHostState.showSnackbar("Tamamlandı")
                viewModel.resetSettingsUiState()
                onNavigateBack()
            }
            is SettingsUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as SettingsUiState.Error).message)
                scrollState.animateScrollTo(scrollState.maxValue)
            }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = PureWhite,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ayarlar & API Anahtarları",
                        fontWeight = FontWeight.Bold,
                        color = CharcoalDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = CharcoalDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PureWhite)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ================== VISUAL GUIDE SECTION ==================
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isGuideExpanded = !isGuideExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "📖 Neyi Nasıl Alacaksınız? (Rehber)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = CharcoalDark
                                )
                                Text(
                                    text = "Adım adım anahtar alma rehberi ve doğrudan linkler",
                                    fontSize = 12.sp,
                                    color = SlateGray
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isGuideExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = SlateGray
                        )
                    }

                    AnimatedVisibility(
                        visible = isGuideExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Guide Item 1: Gemini API
                            GuideStepCard(
                                stepNumber = "1",
                                title = "Gemini API Key (Google AI Studio)",
                                badge = "ÜCRETSİZ",
                                badgeColor = SuccessGreen,
                                icon = Icons.Default.Key,
                                steps = listOf(
                                    "Google AI Studio sayfasına gidin (Google hesabınızla giriş yapın).",
                                    "Mavi renkli 'Get API Key' veya 'Create API Key' butonuna tıklayın.",
                                    "Oluşan 'AIzaSy...' ile başlayan kodu kopyalayıp aşağıdaki ilk alana yapıştırın."
                                ),
                                actionText = "Google AI Studio'yu Aç",
                                onActionClick = {
                                    uriHandler.openUri("https://aistudio.google.com/app/apikey")
                                }
                            )

                            // Guide Item 2: Fish Audio API Key
                            GuideStepCard(
                                stepNumber = "2",
                                title = "Fish Audio API Key",
                                badge = "TTS Motoru",
                                badgeColor = AccentBlue,
                                icon = Icons.Default.Key,
                                steps = listOf(
                                    "Fish Audio web sitesine gidin ve hesabınıza giriş yapın.",
                                    "'API Keys' sekmesine gidin.",
                                    "'Create New Key' butonuna basarak API anahtarınızı üretin ve kopyalayın."
                                ),
                                actionText = "Fish Audio API Sayfasını Aç",
                                onActionClick = {
                                    uriHandler.openUri("https://fish.audio/app/api-keys")
                                }
                            )

                            // Guide Item 3: Voice Model ID
                            GuideStepCard(
                                stepNumber = "3",
                                title = "Fish Audio Model ID (reference_id)",
                                badge = "Ses Seçimi",
                                badgeColor = Color(0xFF8B5CF6),
                                icon = Icons.Default.RecordVoiceOver,
                                steps = listOf(
                                    "Fish Audio 'Models' sekmesinden dilediğiniz bir sesi açın (Örn: Türkçe spiker sesleri).",
                                    "Model sayfasındaki adres çubuğunun (URL) sonundaki 32 haneli kodu kopyalayın.",
                                    "Örnek: fish.audio/m/7f92f8afb8ec43bf81429cc1c9199cb1 linkindeki '7f92f8afb8ec43bf81429cc1c9199cb1' kısmı Model ID'nizdir."
                                ),
                                actionText = "Ses Modellerine Göz At",
                                onActionClick = {
                                    uriHandler.openUri("https://fish.audio/models")
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = "Bilgileri Girin",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CharcoalDark
            )

            // Gemini API Key Field
            OutlinedTextField(
                value = geminiKey,
                onValueChange = { geminiKey = it },
                label = { Text("Gemini API Key") },
                placeholder = { Text("AIzaSy...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (isGeminiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isGeminiKeyVisible = !isGeminiKeyVisible }) {
                        Icon(
                            imageVector = if (isGeminiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Göster/Gizle",
                            tint = SlateGray
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CharcoalDark,
                    unfocusedBorderColor = Color(0xFFD1D5DB),
                    focusedLabelColor = CharcoalDark,
                    cursorColor = CharcoalDark
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Fish Audio API Key Field
            OutlinedTextField(
                value = fishKey,
                onValueChange = { fishKey = it },
                label = { Text("Fish Audio API Key") },
                placeholder = { Text("Bearer token / API anahtarı") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (isFishKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isFishKeyVisible = !isFishKeyVisible }) {
                        Icon(
                            imageVector = if (isFishKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Göster/Gizle",
                            tint = SlateGray
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CharcoalDark,
                    unfocusedBorderColor = Color(0xFFD1D5DB),
                    focusedLabelColor = CharcoalDark,
                    cursorColor = CharcoalDark
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Fish Audio Voice Model ID (reference_id)
            OutlinedTextField(
                value = voiceId,
                onValueChange = { voiceId = it },
                label = { Text("Fish Audio Voice Model ID (reference_id)") },
                placeholder = { Text("Örn: 7f92f8afb8ec43bf81429cc1c9199cb1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CharcoalDark,
                    unfocusedBorderColor = Color(0xFFD1D5DB),
                    focusedLabelColor = CharcoalDark,
                    cursorColor = CharcoalDark
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Error display card
            if (uiState is SettingsUiState.Error) {
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Doğrulama Sonucu:",
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = (uiState as SettingsUiState.Error).message,
                            color = Color(0xFF991B1B),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Button 1: Kontrol Et ve Kaydet
            val isChecking = uiState is SettingsUiState.Checking
            Button(
                onClick = {
                    viewModel.validateAndSaveSettings(geminiKey, fishKey, voiceId)
                },
                enabled = !isChecking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CharcoalDark,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF6B7280),
                    disabledContentColor = Color.White
                )
            ) {
                if (isChecking) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Hızlıca Doğrulanıyor...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Kontrol Et ve Kaydet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Action Button 2: Direct Save without validation (Escape hatch)
            OutlinedButton(
                onClick = {
                    viewModel.saveSettingsDirectly(geminiKey, fishKey, voiceId)
                },
                enabled = !isChecking && geminiKey.isNotBlank() && fishKey.isNotBlank() && voiceId.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CharcoalDark
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderLight)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    tint = CharcoalDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Test Etmeden Doğrudan Kaydet",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = CharcoalDark
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun GuideStepCard(
    stepNumber: String,
    title: String,
    badge: String,
    badgeColor: Color,
    icon: ImageVector,
    steps: List<String>,
    actionText: String,
    onActionClick: () -> Unit
) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(CharcoalDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CharcoalDark
                    )
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${index + 1}.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray,
                        modifier = Modifier.width(18.dp)
                    )
                    Text(
                        text = step,
                        fontSize = 12.sp,
                        color = CharcoalDark,
                        lineHeight = 17.sp
                    )
                }
            }

            // Direct Open URL Button
            OutlinedButton(
                onClick = onActionClick,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AccentBlue
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(AccentBlue.copy(alpha = 0.5f))
                )
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = actionText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentBlue
                )
            }
        }
    }
}
