package com.kopyamatik.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "kopyamatik_settings")

data class AppSettings(
    val geminiApiKey: String = "",
    val fishAudioApiKey: String = "",
    val fishAudioVoiceId: String = ""
) {
    val isConfigured: Boolean
        get() = geminiApiKey.isNotBlank() && fishAudioApiKey.isNotBlank() && fishAudioVoiceId.isNotBlank()
}

class SettingsDataStore(private val context: Context) {

    companion object {
        private val KEY_GEMINI_API = stringPreferencesKey("gemini_api_key")
        private val KEY_FISH_AUDIO_API = stringPreferencesKey("fish_audio_api_key")
        private val KEY_FISH_AUDIO_VOICE_ID = stringPreferencesKey("fish_audio_voice_id")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            geminiApiKey = preferences[KEY_GEMINI_API] ?: "",
            fishAudioApiKey = preferences[KEY_FISH_AUDIO_API] ?: "",
            fishAudioVoiceId = preferences[KEY_FISH_AUDIO_VOICE_ID] ?: ""
        )
    }

    suspend fun getSettings(): AppSettings {
        return settingsFlow.first()
    }

    suspend fun saveSettings(geminiKey: String, fishKey: String, voiceId: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GEMINI_API] = geminiKey.trim()
            preferences[KEY_FISH_AUDIO_API] = fishKey.trim()
            preferences[KEY_FISH_AUDIO_VOICE_ID] = voiceId.trim()
        }
    }
}
