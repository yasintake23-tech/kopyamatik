package com.kopyamatik.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.kopyamatik.app.ui.theme.BorderLight
import com.kopyamatik.app.ui.theme.CharcoalDark
import com.kopyamatik.app.ui.theme.ErrorRed
import com.kopyamatik.app.ui.theme.PureWhite
import com.kopyamatik.app.ui.theme.SlateGray
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val imageUris by viewModel.selectedImageUris.collectAsState()
    val processState by viewModel.processState.collectAsState()
    val playerUiState by viewModel.playerUiState.collectAsState()

    // Temporary URI holder for camera capture
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            viewModel.addImage(tempCameraUri!!)
        }
    }

    Scaffold(
        containerColor = PureWhite,
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ayarlar",
                            tint = CharcoalDark,
                            modifier = Modifier.size(28.dp)
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
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // When no audio is ready, show image upload and actions
            if (processState !is ProcessState.AudioReady) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Soru Çözme & Seslendirme",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalDark,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Fotoğraf çekin veya ekran görüntüsü seçin; AI soruları anında çözüp seslendirsin.",
                    fontSize = 14.sp,
                    color = SlateGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons (SS Yükle and Fotoğraf Çek)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SS Yükle Button
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CharcoalDark,
                            contentColor = Color.White,
                            disabledContainerColor = CharcoalDark,
                            disabledContentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SS Yükle",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    // Fotoğraf Çek Button
                    OutlinedButton(
                        onClick = {
                            val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
                            val photoFile = File(imagesDir, "cam_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                photoFile
                            )
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CharcoalDark
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(CharcoalDark)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = CharcoalDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fotoğraf Çek",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = CharcoalDark
                        )
                    }
                }

                // Selected Images Preview Carousel
                if (imageUris.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Seçilen Fotoğraflar (${imageUris.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = SlateGray
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(imageUris) { uri ->
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Seçilen Soru",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeImage(uri) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(26.dp)
                                            .background(
                                                color = CharcoalDark.copy(alpha = 0.7f),
                                                shape = CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Kaldır",
                                            tint = PureWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Solve and Speak Trigger Button
                    val isProcessing = processState !is ProcessState.Idle && processState !is ProcessState.Error
                    Button(
                        onClick = { viewModel.startSolvingAndSpeech() },
                        enabled = !isProcessing,
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
                        Text(
                            text = "Soruları Çöz ve Seslendir",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Processing Indicator & Status Text
                AnimatedVisibility(
                    visible = processState is ProcessState.SolvingQuestions ||
                            processState is ProcessState.SynthesizingAudio ||
                            processState is ProcessState.Finalizing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            color = CharcoalDark,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(42.dp)
                        )

                        val statusMessage = when (val state = processState) {
                            is ProcessState.SolvingQuestions -> "Sorular çözülüyor..."
                            is ProcessState.SynthesizingAudio -> "Seslendiriliyor ${state.current}/${state.total}..."
                            is ProcessState.Finalizing -> "Ses dosyaları birleştiriliyor..."
                            else -> "İşleniyor..."
                        }

                        Text(
                            text = statusMessage,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CharcoalDark
                        )
                    }
                }

                // Error Message
                if (processState is ProcessState.Error) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Bir Hata Oluştu",
                                color = ErrorRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = (processState as ProcessState.Error).message,
                                color = Color(0xFF991B1B),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Audio Player Component (Visible when AudioReady)
            if (processState is ProcessState.AudioReady) {
                AudioPlayerCard(
                    playerUiState = playerUiState,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onSeekForward = { viewModel.seekForward() },
                    onSeekBackward = { viewModel.seekBackward() },
                    onSelectSpeed = { viewModel.setPlaybackSpeed(it) },
                    onReset = { viewModel.resetProcess() }
                )
            }
        }
    }
}

@Composable
fun AudioPlayerCard(
    playerUiState: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onReset: () -> Unit
) {
    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDraggingSlider by remember { mutableStateOf(false) }

    val currentPosition = if (isDraggingSlider) {
        sliderPosition.toLong()
    } else {
        playerUiState.currentPositionMs
    }

    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderLight),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Çözüm Ses Kaydı",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CharcoalDark
            )

            // Seekbar Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                val maxRange = playerUiState.durationMs.toFloat().coerceAtLeast(1f)
                Slider(
                    value = currentPosition.toFloat().coerceIn(0f, maxRange),
                    onValueChange = {
                        isDraggingSlider = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        onSeekTo(sliderPosition.toLong())
                    },
                    valueRange = 0f..maxRange,
                    colors = SliderDefaults.colors(
                        thumbColor = CharcoalDark,
                        activeTrackColor = CharcoalDark,
                        inactiveTrackColor = BorderLight
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Time labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        fontSize = 12.sp,
                        color = SlateGray
                    )
                    Text(
                        text = formatTime(playerUiState.durationMs),
                        fontSize = 12.sp,
                        color = SlateGray
                    )
                }
            }

            // Controls: Rewind, Play/Pause, Fast Forward
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // -10s
                IconButton(onClick = onSeekBackward) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "10sn Geri",
                        tint = CharcoalDark,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Play / Pause Button
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(64.dp)
                        .background(CharcoalDark, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if (playerUiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerUiState.isPlaying) "Durdur" else "Oynat",
                        tint = PureWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // +10s
                IconButton(onClick = onSeekForward) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "10sn İleri",
                        tint = CharcoalDark,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Playback Speed Options
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Oynatma Hızı",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = SlateGray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    speedOptions.forEach { speed ->
                        val isSelected = playerUiState.playbackSpeed == speed
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CharcoalDark else Color(0xFFF3F4F6))
                                .clickable { onSelectSpeed(speed) }
                        ) {
                            Text(
                                text = "${speed}x",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else CharcoalDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Reset / New Questions Button
            OutlinedButton(
                onClick = onReset,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateGray),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderLight)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Yeni Soru Yükle", fontSize = 14.sp)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
