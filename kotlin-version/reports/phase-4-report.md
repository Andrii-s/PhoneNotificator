# Звіт Фаза 4 — Сервіс дзвінків

**Дата:** 2026-03-13  
**Статус:** ✅ Виконано

## Що зроблено

### AutoDialerService (ForegroundService)
- `service/AutoDialerService.kt` — повна реалізація:
  - `@AndroidEntryPoint` для Hilt injection
  - Ін'єкція `CallRepository`
  - `Queue<String>` — черга номерів для обдзвону
  - `StateFlow<DialingState>` — поточний стан (currentNumber, progress, total, isDialing)

### Дозволи
- CALL_PHONE та READ_PHONE_STATE запитуються через Accompanist в DebtorsScreen
- FOREGROUND_SERVICE_PHONE_CALL оголошено в маніфесті
- foregroundServiceType="phoneCall" в декларації сервісу

### Відстеження стану дзвінка
- Подвійна API підтримка:
  - API < 31: `PhoneStateListener.onCallStateChanged()` (deprecated, але потрібна для min API 26)
  - API ≥ 31: `TelephonyCallback` + `TelephonyCallback.CallStateListener`
- OFFHOOK → Handler.postDelayed(1000) { startAudioPlayback() }
- IDLE (після OFFHOOK) → endTime, дривалість, збереження логу, відправка звіту

### Відтворення аудіо
- MediaPlayer з AudioAttributes (USAGE_VOICE_COMMUNICATION, CONTENT_TYPE_SPEECH)
- prepare() на Dispatchers.IO (блокуючий)
- start() на Main thread
- wasAudioStarted = true для визначення CallStatus.ANSWERED

### Таймер та логування
- callStartTime = System.currentTimeMillis() при OFFHOOK
- durationSec = (endTime - callStartTime) / 1000
- Статус: wasAudioStarted → ANSWERED, durationSec > 0 → NO_ANSWER, else → FAILED

### Сповіщення Foreground Service
- NotificationChannel IMPORTANCE_LOW (нема звуку)
- Динамічний текст: "Дзвінок X з Y: +380..."
- Кнопка "Зупинити" (PendingIntent → ACTION_STOP)
- setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)

### Черга дзвінків
- 2 секунди паузи між дзвінками (delay(2_000L) в корутині)
- CoroutineScope(SupervisorJob + Dispatchers.Main.immediate)
- scope.cancel() в onDestroy()

## Технічні рішення
- START_NOT_STICKY — сервіс не перезапускається після знищення
- mainHandler для Handler.postDelayed — гарантовано Main thread
- SupervisorJob — падіння одного корутина не відміняє інші
