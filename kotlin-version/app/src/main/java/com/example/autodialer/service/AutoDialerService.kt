package com.example.autodialer.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.autodialer.MainActivity
import com.example.autodialer.R
import com.example.autodialer.domain.model.CallLog
import com.example.autodialer.domain.model.CallReport
import com.example.autodialer.domain.model.CallStatus
import com.example.autodialer.domain.repository.CallRepository
import com.example.autodialer.data.local.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.LinkedList
import java.util.Queue
import javax.inject.Inject

/**
 * Foreground service that manages the full auto-dialing lifecycle:
 *
 *  1. Receives a list of phone numbers via [ACTION_START].
 *  2. Dials each number in sequence using [Intent.ACTION_CALL].
 *  3. When the call connects (OFFHOOK) it waits 1 second then starts the
 *     pre-recorded audio message through [MediaPlayer].
 *  4. When the call ends (IDLE after OFFHOOK) it persists a [CallLog],
 *     sends a [CallReport] to the server, and dials the next number.
 *  5. Stops itself (or can be stopped by [ACTION_STOP]) once the queue is empty.
 */
@AndroidEntryPoint
class AutoDialerService : Service() {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    companion object {
        const val CHANNEL_ID = "autodialer_phone_call"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.autodialer.ACTION_START"
        const val ACTION_STOP  = "com.example.autodialer.ACTION_STOP"

        /** ArrayList<String> of phone numbers to dial. */
        const val EXTRA_NUMBERS = "numbers"
        /** Absolute path to the audio file to play when a call connects. */
        const val EXTRA_AUDIO_FILE_PATH = "audioFilePath"
    }

    // -------------------------------------------------------------------------
    // Dialing state (observable from outside via a bound / shared ViewModel)
    // -------------------------------------------------------------------------

    data class DialingState(
        val currentNumber: String = "",
        val progress: Int = 0,
        val total: Int = 0,
        val isDialing: Boolean = false,
    )

    private val _dialingState = MutableStateFlow(DialingState())
    val dialingState: StateFlow<DialingState> = _dialingState.asStateFlow()

    // -------------------------------------------------------------------------
    // Injected dependencies
    // -------------------------------------------------------------------------

    @Inject
    lateinit var callRepository: CallRepository

    @Inject
    lateinit var appPreferences: AppPreferences

    // -------------------------------------------------------------------------
    // Internal state
    // -------------------------------------------------------------------------

    private val numberQueue: Queue<String> = LinkedList()
    private var selectedAudioFilePath: String = ""

    private var telephonyManager: TelephonyManager? = null
    private var audioManager: AudioManager? = null
    private var mediaPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Call-tracking fields
    private var isCallPlaced = false
    private var wasOffHook = false
    private var wasAudioStarted = false
    private var callStartTime = 0L
    private var currentPhone = ""

    // -------------------------------------------------------------------------
    // Phone state listener — API < 31
    // -------------------------------------------------------------------------

    @Suppress("DEPRECATION")
    private val phoneStateListener = object : PhoneStateListener() {
        @Deprecated("Deprecated in Java")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            handleCallState(state)
        }
    }

    // -------------------------------------------------------------------------
    // TelephonyCallback — API 31+
    // -------------------------------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.S)
    private inner class ApiSPhoneCallback : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            handleCallState(state)
        }
    }

    // Lazily initialised so the object isn't created on older APIs.
    private val telephonyCallback by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ApiSPhoneCallback() else null
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        Timber.d("AutoDialerService created")
        createNotificationChannel()
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        registerPhoneStateListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleActionStart(intent)
            ACTION_STOP  -> handleActionStop()
            else         -> Timber.w("Unknown action: ${intent?.action}")
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("AutoDialerService destroyed")
        unregisterPhoneStateListener()
        releaseMediaPlayer()
        audioManager?.mode = AudioManager.MODE_NORMAL
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // -------------------------------------------------------------------------
    // Intent handling
    // -------------------------------------------------------------------------

    private fun handleActionStart(intent: Intent) {
        val numbers = intent.getStringArrayListExtra(EXTRA_NUMBERS)
        if (numbers.isNullOrEmpty()) {
            Timber.w("ACTION_START received with empty numbers list — ignoring")
            return
        }
        selectedAudioFilePath = intent.getStringExtra(EXTRA_AUDIO_FILE_PATH).orEmpty()

        numberQueue.clear()
        numberQueue.addAll(numbers)

        val total = numbers.size
        _dialingState.update { it.copy(total = total, progress = 0, isDialing = true) }

        startForeground(NOTIFICATION_ID, buildNotification(0, total, ""))
        Timber.i("AutoDialerService started: $total numbers, audio=$selectedAudioFilePath")
        dialNext()
    }

    private fun handleActionStop() {
        Timber.i("AutoDialerService stopped by user")
        stopDialing()
    }

    // -------------------------------------------------------------------------
    // Dialing logic
    // -------------------------------------------------------------------------

    /** Picks the next number from the queue and places a call. */
    private fun dialNext() {
        if (numberQueue.isEmpty()) {
            Timber.i("Queue empty — stopping service")
            stopDialing()
            return
        }

        currentPhone = numberQueue.poll() ?: run { stopDialing(); return }
        val total = _dialingState.value.total
        val progress = total - numberQueue.size      // 1-based progress index

        _dialingState.update { it.copy(currentNumber = currentPhone, progress = progress) }
        updateNotification(progress, total, currentPhone)

        isCallPlaced = true
        wasOffHook = false
        wasAudioStarted = false
        callStartTime = 0L

        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$currentPhone")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(callIntent)
        Timber.d("Dialing $currentPhone ($progress/$total)")
    }

    /** Called when all numbers have been processed or the user stops the service. */
    private fun stopDialing() {
        numberQueue.clear()
        releaseMediaPlayer()
        _dialingState.update { DialingState() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // -------------------------------------------------------------------------
    // Telephony state handling
    // -------------------------------------------------------------------------

    private fun handleCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (isCallPlaced && !wasOffHook) {
                    wasOffHook = true
                    callStartTime = System.currentTimeMillis()
                    audioManager?.mode = AudioManager.MODE_IN_CALL
                    val delayMs = appPreferences.audioDelaySeconds * 1_000L
                    Timber.d("OFFHOOK — scheduling audio in ${appPreferences.audioDelaySeconds} s (TX mode: IN_CALL)")
                    mainHandler.postDelayed({ startAudioPlayback() }, delayMs)
                }
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                if (wasOffHook) {
                    mainHandler.removeCallbacksAndMessages(null)
                    wasOffHook = false
                    isCallPlaced = false
                    audioManager?.mode = AudioManager.MODE_NORMAL
                    val endTime = System.currentTimeMillis()
                    val durationSec = (endTime - callStartTime) / 1_000
                    Timber.d("IDLE after OFFHOOK — duration=${durationSec}s, audioStarted=$wasAudioStarted")
                    releaseMediaPlayer()
                    val phone = currentPhone
                    val audioStarted = wasAudioStarted
                    wasAudioStarted = false

                    serviceScope.launch {
                        saveAndReport(phone, callStartTime, endTime, durationSec, audioStarted)
                        // Brief pause between calls
                        delay(2_000L)
                        dialNext()
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Audio playback
    // -------------------------------------------------------------------------

    private fun startAudioPlayback() {
        if (selectedAudioFilePath.isBlank()) {
            Timber.d("No audio file configured — skipping playback")
            return
        }
        serviceScope.launch(Dispatchers.IO) {
            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    setDataSource(selectedAudioFilePath)
                    prepare()
                }
                withContext(Dispatchers.Main) {
                    mediaPlayer = mp
                    mp.start()
                    wasAudioStarted = true
                    Timber.d("Audio playback started")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start audio playback")
            }
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.run {
            try {
                if (isPlaying) stop()
                release()
            } catch (e: Exception) {
                Timber.e(e, "Error releasing MediaPlayer")
            }
        }
        mediaPlayer = null
    }

    // -------------------------------------------------------------------------
    // Persistence & reporting
    // -------------------------------------------------------------------------

    private suspend fun saveAndReport(
        phone: String,
        startTime: Long,
        endTime: Long,
        durationSec: Long,
        audioStarted: Boolean,
    ) {
        val status = when {
            audioStarted -> CallStatus.ANSWERED
            durationSec > 0 -> CallStatus.NO_ANSWER
            else -> CallStatus.FAILED
        }

        val log = CallLog(
            phone = phone,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = durationSec,
            status = status,
        )
        try {
            callRepository.saveCallLog(log)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save call log for $phone")
        }

        val report = CallReport(
            phone = phone,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = durationSec,
        )
        callRepository.sendCallReport(report).onFailure { e ->
            Timber.w(e, "Failed to send call report for $phone")
        }
    }

    // -------------------------------------------------------------------------
    // Telephony listener registration
    // -------------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun registerPhoneStateListener() {
        val tm = telephonyManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cb = telephonyCallback ?: return
            tm.registerTelephonyCallback(mainExecutor, cb as TelephonyCallback)
        } else {
            tm.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    @Suppress("DEPRECATION")
    private fun unregisterPhoneStateListener() {
        val tm = telephonyManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cb = telephonyCallback ?: return
            tm.unregisterTelephonyCallback(cb as TelephonyCallback)
        } else {
            tm.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        }
    }

    // -------------------------------------------------------------------------
    // Notification helpers
    // -------------------------------------------------------------------------

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(current: Int, total: Int, number: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, AutoDialerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE,
        )

        val contentText = if (current > 0 && number.isNotEmpty()) {
            getString(R.string.notification_calling, current, total, number)
        } else {
            getString(R.string.notification_text)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                android.R.drawable.ic_delete,
                getString(R.string.notification_stop_action),
                stopIntent,
            )
            .build()
    }

    private fun updateNotification(current: Int, total: Int, number: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(current, total, number))
    }
}
