package com.kopyamatik.app.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.kopyamatik.app.data.AppSettings
import com.kopyamatik.app.data.FishAudioService
import com.kopyamatik.app.data.GeminiService
import com.kopyamatik.app.data.SettingsDataStore
import com.kopyamatik.app.service.AudioPlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

sealed interface ProcessState {
    object Idle : ProcessState
    object SolvingQuestions : ProcessState
    data class SynthesizingAudio(val current: Int, val total: Int) : ProcessState
    object Finalizing : ProcessState
    data class AudioReady(val audioFile: File) : ProcessState
    data class Error(val message: String) : ProcessState
}

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f
)

sealed interface SettingsUiState {
    object Idle : SettingsUiState
    object Checking : SettingsUiState
    object Success : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = SettingsDataStore(application)
    private val geminiService = GeminiService()
    private val fishAudioService = FishAudioService()

    val settingsState: StateFlow<AppSettings> = MutableStateFlow(AppSettings()).apply {
        viewModelScope.launch {
            dataStore.settingsFlow.collect { this@apply.value = it }
        }
    }

    private val _selectedImageUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImageUris: StateFlow<List<Uri>> = _selectedImageUris.asStateFlow()

    private val _processState = MutableStateFlow<ProcessState>(ProcessState.Idle)
    val processState: StateFlow<ProcessState> = _processState.asStateFlow()

    private val _settingsUiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val settingsUiState: StateFlow<SettingsUiState> = _settingsUiState.asStateFlow()

    private val _playerUiState = MutableStateFlow(PlayerUiState())
    val playerUiState: StateFlow<PlayerUiState> = _playerUiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // Service binding
    private var playbackService: AudioPlaybackService? = null
    private var isBound = false
    private var playerProgressJob: Job? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? AudioPlaybackService.LocalBinder
            playbackService = binder?.getService()
            isBound = true
            setupPlayerListener(binder?.getPlayer())
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            isBound = false
            stopProgressTracking()
        }
    }

    init {
        bindPlaybackService()
    }

    private fun bindPlaybackService() {
        val intent = Intent(getApplication(), AudioPlaybackService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setupPlayerListener(player: Player?) {
        player ?: return

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerUiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracking()
                } else {
                    stopProgressTracking()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val duration = player.duration.coerceAtLeast(0L)
                _playerUiState.update {
                    it.copy(
                        durationMs = duration,
                        currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                    )
                }
            }
        })
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        playerProgressJob = viewModelScope.launch {
            while (isActive) {
                playbackService?.let {
                    val player = (it as? AudioPlaybackService)?.let { s ->
                        // player position update
                    }
                }
                val binder = (playbackService as? AudioPlaybackService)
                // update position from service
                binder?.let {
                    // ExoPlayer state
                }
                _playerUiState.update { state ->
                    val pos = playbackService?.let {
                        // Read current position safely
                        state.currentPositionMs
                    } ?: state.currentPositionMs
                    state
                }
                delay(300)
            }
        }
    }

    private fun stopProgressTracking() {
        playerProgressJob?.cancel()
        playerProgressJob = null
    }

    // ================= Image selection & management =================

    fun addImages(uris: List<Uri>) {
        _selectedImageUris.update { current ->
            (current + uris).distinct()
        }
    }

    fun addImage(uri: Uri) {
        _selectedImageUris.update { current ->
            (current + uri).distinct()
        }
    }

    fun removeImage(uri: Uri) {
        _selectedImageUris.update { current ->
            current.filter { it != uri }
        }
    }

    fun clearImages() {
        _selectedImageUris.value = emptyList()
    }

    // ================= Process pipeline (Gemini -> Fish Audio) =================

    fun startSolvingAndSpeech() {
        viewModelScope.launch {
            val settings = dataStore.getSettings()
            if (!settings.isConfigured) {
                _processState.value = ProcessState.Error("Lütfen önce Ayarlar ekranından API anahtarlarınızı kaydedin.")
                return@launch
            }

            val imageUris = _selectedImageUris.value
            if (imageUris.isEmpty()) {
                _processState.value = ProcessState.Error("Lütfen çözülecek en az bir soru fotoğrafı ekleyin.")
                return@launch
            }

            _processState.value = ProcessState.SolvingQuestions

            // Load and decode Bitmaps
            val bitmaps = mutableListOf<Bitmap>()
            try {
                for (uri in imageUris) {
                    val stream = getApplication<Application>().contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    if (bitmap != null) {
                        bitmaps.add(bitmap)
                    }
                }
            } catch (e: Exception) {
                _processState.value = ProcessState.Error("Görseller yüklenirken hata oluştu: ${e.localizedMessage}")
                return@launch
            }

            if (bitmaps.isEmpty()) {
                _processState.value = ProcessState.Error("Seçilen görseller okunamadı.")
                return@launch
            }

            // Step 1: Gemini Solve
            val geminiResult = geminiService.solveQuestions(
                apiKey = settings.geminiApiKey,
                images = bitmaps
            )

            val solvedText = geminiResult.getOrElse {
                _processState.value = ProcessState.Error("AI Soru Çözüm Hatası: ${it.localizedMessage}")
                return@launch
            }

            // Step 2: Fish Audio TTS & Concatenation
            _processState.value = ProcessState.SynthesizingAudio(1, 1)

            val audioDir = File(getApplication<Application>().cacheDir, "audio")
            val outputFile = File(audioDir, "cozum_${System.currentTimeMillis()}.mp3")

            val fishAudioResult = fishAudioService.synthesizeTextToFile(
                apiKey = settings.fishAudioApiKey,
                voiceModelId = settings.fishAudioVoiceId,
                fullText = solvedText,
                outputFile = outputFile,
                onProgress = { current, total ->
                    _processState.value = ProcessState.SynthesizingAudio(current, total)
                }
            )

            val finalAudioFile = fishAudioResult.getOrElse {
                _processState.value = ProcessState.Error("Seslendirme Hatası: ${it.localizedMessage}")
                return@launch
            }

            _processState.value = ProcessState.AudioReady(finalAudioFile)

            // Auto-start playback
            playbackService?.playFile(finalAudioFile)
        }
    }

    // ================= Playback Controls =================

    fun togglePlayPause() {
        playbackService?.togglePlayPause()
        _playerUiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun seekTo(positionMs: Long) {
        playbackService?.seekTo(positionMs)
        _playerUiState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun seekForward() {
        playbackService?.seekForward(10000L)
    }

    fun seekBackward() {
        playbackService?.seekBackward(10000L)
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackService?.setPlaybackSpeed(speed)
        _playerUiState.update { it.copy(playbackSpeed = speed) }
    }

    fun resetProcess() {
        _processState.value = ProcessState.Idle
        _selectedImageUris.value = emptyList()
    }

    // ================= Settings Verification and Save =================

    fun validateAndSaveSettings(geminiKey: String, fishKey: String, voiceId: String) {
        viewModelScope.launch {
            if (geminiKey.isBlank() || fishKey.isBlank() || voiceId.isBlank()) {
                _settingsUiState.value = SettingsUiState.Error("Lütfen tüm alanları eksiksiz doldurun.")
                return@launch
            }

            _settingsUiState.value = SettingsUiState.Checking

            // Test Gemini API key
            val geminiValidation = geminiService.validateApiKey(geminiKey)
            if (geminiValidation.isFailure) {
                _settingsUiState.value = SettingsUiState.Error(
                    geminiValidation.exceptionOrNull()?.message ?: "Gemini API anahtarı geçersiz."
                )
                return@launch
            }

            // Test Fish Audio API key & Model
            val fishValidation = fishAudioService.validateApiKeyAndModel(fishKey, voiceId)
            if (fishValidation.isFailure) {
                _settingsUiState.value = SettingsUiState.Error(
                    fishValidation.exceptionOrNull()?.message ?: "Fish Audio API veya Model ID geçersiz."
                )
                return@launch
            }

            // Both valid: persist into DataStore
            dataStore.saveSettings(geminiKey, fishKey, voiceId)
            _settingsUiState.value = SettingsUiState.Success
            _toastEvent.emit("Tamamlandı")
        }
    }

    fun resetSettingsUiState() {
        _settingsUiState.value = SettingsUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (_: Exception) {}
            isBound = false
        }
        stopProgressTracking()
    }
}
