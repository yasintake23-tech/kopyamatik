package com.kopyamatik.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var geminiKey by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var fishKey by remember(settings.fishAudioApiKey) { mutableStateOf(settings.fishAudioApiKey) }
    var voiceId by remember(settings.fishAudioVoiceId) { mutableStateOf(settings.fishAudioVoiceId) }

    var isGeminiKeyVisible by remember { mutableStateOf(false) }
    var isFishKeyVisible by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (uiState) {
            is SettingsUiState.Success -> {
                snackbarHostState.showSnackbar("Tamamlandı")
                viewModel.resetSettingsUiState()
                onNavigateBack()
            }
            is SettingsUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as SettingsUiState.Error).message)
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
                        text = "Ayarlar",
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "API Anahtarları ve Yapılandırma",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = SlateGray
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
                placeholder = { Text("Bearer token / API key") },
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

            // Info text
            Text(
                text = "Girilen bilgiler 'Kontrol Et ve Kaydet' butonuna basıldığında test edilecek ve cihazda güvenle saklanacaktır.",
                fontSize = 12.sp,
                color = SlateGray,
                lineHeight = 16.sp
            )

            if (uiState is SettingsUiState.Error) {
                Text(
                    text = (uiState as SettingsUiState.Error).message,
                    color = ErrorRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Validate and Save Button
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
                            text = "Doğrulanıyor...",
                            fontSize = 16.sp,
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
