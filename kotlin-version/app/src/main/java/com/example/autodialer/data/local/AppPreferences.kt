package com.example.autodialer.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around [SharedPreferences] for persisting user-configurable
 * app settings that are not domain data (i.e. not stored in Room).
 *
 * !! ВАЖЛИВО: Не видаляти та не скидати цей клас !!
 * Він використовується:
 *  - SettingsViewModel  — для відображення/зміни затримки у налаштуваннях
 *  - AutoDialerService  — для читання затримки перед стартом аудіо в TX-каналі
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("autodialer_prefs", Context.MODE_PRIVATE)

    /**
     * Delay in **seconds** before audio playback starts after a call connects (OFFHOOK).
     *
     * Default = 15 s — достатньо, щоб абонент встиг підняти трубку до початку відтворення.
     * Значення ≥ 0; зберігається між запусками.
     */
    var audioDelaySeconds: Int
        get() = prefs.getInt(KEY_AUDIO_DELAY_SECONDS, DEFAULT_AUDIO_DELAY_SECONDS)
        set(value) { prefs.edit().putInt(KEY_AUDIO_DELAY_SECONDS, value).apply() }

    companion object {
        const val DEFAULT_AUDIO_DELAY_SECONDS = 15
        private const val KEY_AUDIO_DELAY_SECONDS = "audio_delay_seconds"
    }
}
