# AutoDialer — План розробки Android-додатку на Kotlin

## 1. Огляд проекту

**AutoDialer** — мобільний Android-додаток для автоматичного обдзвону абонентів.
Користувач завантажує mp3-файл (голосове повідомлення), обирає список або окремий номер,
запускає обдзвін. Після з'єднання через 1 секунду абоненту програється обраний mp3-файл.
Кожен дзвінок звітує про час початку, кінця та тривалість, а звіт надсилається на сервер.

---

## 2. Технічний стек

| Компонент            | Технологія                                      |
|----------------------|-------------------------------------------------|
| Мова                 | Kotlin                                          |
| UI                   | Jetpack Compose + Material 3                    |
| Навігація            | Navigation Compose                              |
| Архітектура          | MVVM + Clean Architecture (UI → ViewModel → Repository → DataSource) |
| DI                   | Hilt (Dagger Hilt)                              |
| HTTP-клієнт          | Retrofit 2 + OkHttp3 + Gson/Moshi               |
| Асинхронність        | Kotlin Coroutines + Flow                        |
| Збереження файлів    | MediaStore / Internal Storage                  |
| Відтворення аудіо    | MediaPlayer (стандарт Android)                 |
| Телефонія / Дзвінки  | TelecomManager + ConnectionService              |
| Збереження стану     | ViewModel + StateFlow                           |
| Persistence (DB)     | Room (для списку завантажених файлів і логів)   |
| Логування            | Timber                                          |
| Мінімальна версія    | Android 8.0 (API 26)                            |
| Цільова версія       | Android 15 (API 35)                             |

---

## 3. Необхідні дозволи (AndroidManifest.xml)

```xml
<!-- Здійснення дзвінків -->
<uses-permission android:name="android.permission.CALL_PHONE" />

<!-- Читання стану дзвінка -->
<uses-permission android:name="android.permission.READ_PHONE_STATE" />

<!-- Керування дзвінками (API 26+) -->
<uses-permission android:name="android.permission.MANAGE_OWN_CALLS" />

<!-- Читання зовнішнього сховища (для вибору mp3) -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

<!-- Мережа -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Фонова робота під час дзвінків -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_PHONE_CALL" />

<!-- Відтворення аудіо під час дзвінка (AudioManager) -->
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

> **Важливо:** `CALL_PHONE` і `READ_PHONE_STATE` — небезпечні дозволи, запитуються у рантаймі
> через `ActivityResultContracts.RequestMultiplePermissions`.

---

## 4. Структура проекту

```
app/
└── src/main/
    ├── AndroidManifest.xml
    ├── java/com/example/autodialer/
    │   ├── MainActivity.kt                  // Єдина Activity, хост для Compose
    │   │
    │   ├── navigation/
    │   │   └── AppNavGraph.kt               // NavHost + маршрути
    │   │
    │   ├── ui/
    │   │   ├── splash/
    │   │   │   └── SplashScreen.kt          // Заставка 2.5 с
    │   │   ├── settings/
    │   │   │   ├── SettingsScreen.kt        // Екран налаштувань
    │   │   │   └── SettingsViewModel.kt
    │   │   ├── debtors/
    │   │   │   ├── DebtorsScreen.kt         // Екран боржників
    │   │   │   └── DebtorsViewModel.kt
    │   │   └── confirmlogout/
    │   │       └── ConfirmLogoutScreen.kt   // Підтвердження виходу
    │   │
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── AudioFile.kt             // Модель аудіофайлу
    │   │   │   ├── Debtor.kt                // Модель боржника
    │   │   │   └── CallReport.kt            // Модель звіту дзвінка
    │   │   └── repository/
    │   │       ├── AudioRepository.kt       // Інтерфейс
    │   │       ├── DebtorRepository.kt      // Інтерфейс
    │   │       └── CallRepository.kt        // Інтерфейс
    │   │
    │   ├── data/
    │   │   ├── remote/
    │   │   │   ├── ApiService.kt            // Retrofit endpoints
    │   │   │   ├── dto/
    │   │   │   │   ├── DebtorDto.kt
    │   │   │   │   └── CallReportDto.kt
    │   │   │   └── NetworkModule.kt         // Hilt модуль Retrofit
    │   │   ├── local/
    │   │   │   ├── AppDatabase.kt           // Room database
    │   │   │   ├── dao/
    │   │   │   │   ├── AudioFileDao.kt
    │   │   │   │   └── CallLogDao.kt
    │   │   │   └── entity/
    │   │   │       ├── AudioFileEntity.kt
    │   │   │       └── CallLogEntity.kt
    │   │   └── repository/
    │   │       ├── AudioRepositoryImpl.kt
    │   │       ├── DebtorRepositoryImpl.kt
    │   │       └── CallRepositoryImpl.kt
    │   │
    │   ├── service/
    │   │   ├── AutoDialerService.kt         // ForegroundService — керує чергою дзвінків
    │   │   └── AutoDialerConnection.kt      // ConnectionService для TelecomManager
    │   │
    │   └── di/
    │       ├── AppModule.kt
    │       ├── NetworkModule.kt
    │       └── DatabaseModule.kt
    │
    └── res/
        └── ...                              // drawable, strings, themes
```

---

## 5. Навігація

```
SplashScreen  (2.5 с)
      │
      ▼
SettingsScreen  ─────────────────────────────────────────────┐
      │                                                       │
  [Далі →]                                               [Вийти]
      │                                                       │
      ▼                                                       ▼
DebtorsScreen                                      ConfirmLogoutScreen
      │                                              [Так] ──► SplashScreen
  [Повернутись]                                      [Ні]  ──► SettingsScreen
      │
      ▼
SettingsScreen
```

**Маршрути (sealed class):**
```kotlin
sealed class Screen(val route: String) {
    object Splash       : Screen("splash")
    object Settings     : Screen("settings")
    object Debtors      : Screen("debtors")
    object ConfirmLogout: Screen("confirm_logout")
}
```

---

## 6. Екрани — детальний опис

### 6.1 SplashScreen

- Відображає іконку телефону, назву "AutoDialer", версію "v1.0.0".
- Анімована лінія завантаження (LinearProgressIndicator).
- Після 2.5 секунд автоматично переходить на `SettingsScreen`.
- Реалізація: `LaunchedEffect(Unit) { delay(2500); navController.navigate(...) }`.

---

### 6.2 SettingsScreen

**Функції:**

#### a) Завантаження mp3-файлу
- Кнопка "Завантажити аудіофайл" відкриває `ActivityResultContracts.GetContent("audio/*")`.
- Обраний файл копіюється у внутрішнє сховище додатку (`context.filesDir/audio/`).
- Запис додається у Room (таблиця `audio_files`): `id`, `fileName`, `filePath`, `addedAt`.
- При кожному запуску список підвантажується з Room.

#### b) Список файлів
- `LazyColumn` або `Column` зі списком рядків.
- Кожен рядок: RadioButton (вибір), назва файлу, кнопка "▶ / ⏸".
- При натисканні ▶ — `MediaPlayer` відтворює файл у превью-режимі.
- `RadioGroup` — лише один файл вибрано.

#### c) Програвач аудіо
- Показує назву обраного файлу.
- `Slider` для перемотки (відслідковується `MediaPlayer.currentPosition`).
- Кнопки: SkipBack (перемотка на 10 с), Play/Pause, SkipForward (перемотка на 10 с).
- Гучність через `AudioManager`.

#### d) Навігація
- "Далі →" — активна тільки якщо обраний файл. Переходить на `DebtorsScreen`.
- "Вийти" — переходить на `ConfirmLogoutScreen`.

**SettingsViewModel:**
```kotlin
data class SettingsUiState(
    val audioFiles: List<AudioFile> = emptyList(),
    val selectedFileId: Long? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Int = 0,
    val duration: Int = 0
)
```

---

### 6.3 DebtorsScreen

**Режими (Tab / Select):** `Боржники` | `Ввести номер вручну` | `Введення номерів списком`

#### Режим "Боржники"
1. Кнопка **"Отримати список боржників"**:
   - Виконує `POST https://itproject.com/api/debetors` (тіло запиту: порожній JSON `{}`).
   - Отриманий список відображається у таблиці: № | ПІБ | Номер телефону | Сума боргу.
   - Стан: `Loading` / `Success(list)` / `Error(message)`.
2. Кнопка **"Почати дзвінки"**:
   - Передає список номерів у `AutoDialerService`.
   - Сервіс дзвонить почергово (наступний — після завершення попереднього).
   - Лог дзвінків оновлюється у реальному часі.

#### Режим "Ввести номер вручну"
1. `TextField` для введення одного номера телефону.
2. Кнопка **"Почати дзвінок"**:
   - Запускає один дзвінок через `AutoDialerService`.

#### Режим "Введення номерів списком"
1. `TextField` (multiline) — по одному номеру на рядок.
2. Кнопка **"Почати дзвінки"**:
   - Парсить номери (розбити по `\n`, очистити пусті рядки).
   - Передає список у `AutoDialerService`.

#### Лог виконання
- Scrollable список `CallLogEntry`:
  - Номер телефону
  - Час початку (HH:mm:ss)
  - Час завершення (HH:mm:ss)
  - Тривалість (мм:сс)
  - Статус: успішно / без відповіді / помилка

**DebtorsViewModel:**
```kotlin
data class DebtorsUiState(
    val selectedMode: DialMode = DialMode.DEBTORS,
    val debtors: List<Debtor> = emptyList(),
    val isLoadingDebtors: Boolean = false,
    val debtorsError: String? = null,
    val manualNumber: String = "",
    val listNumbers: String = "",
    val callLogs: List<CallLog> = emptyList(),
    val isDialing: Boolean = false
)

enum class DialMode { DEBTORS, MANUAL, LIST }
```

---

### 6.4 ConfirmLogoutScreen

- Картка по центру: "Ви дійсно готові вийти?"
- "Ні" → `navController.popBackStack()` (повернення на SettingsScreen).
- "Так" → скидає стан (ViewModel.reset()), навігація до Splash.

---

## 7. Сервіс дзвінків (AutoDialerService)

### Архітектура

```
DebtorsViewModel
    │
    ├── bindService / startForegroundService
    │
    └── AutoDialerService (ForegroundService)
            │
            ├── Черга номерів: Queue<String>
            ├── Поточний стан: StateFlow<DialingState>
            │
            ├── dialNext()
            │     ├── TelecomManager.placeCall(uri, extras)
            │     │       або
            │     │   Intent(Intent.ACTION_CALL, tel:NUMBER)
            │     │
            │     └── PhoneStateReceiver / CallStateCallback
            │           ├── OFFHOOK → запустити таймер + запланувати відтворення mp3 через 1 с
            │           ├── mp3 playback → MediaPlayer.start() через Handler.postDelayed(1000)
            │           └── IDLE → зафіксувати час кінця, порахувати тривалість,
            │                       відправити звіт на API, викликати dialNext()
            └── Foreground Notification: "Дзвінок X з Y: +380..."
```

### Логіка одного дзвінка

```
1. Зафіксувати startTime = System.currentTimeMillis()
2. Здійснити дзвінок через TelecomManager або startActivity(ACTION_CALL)
3. PhoneStateListener.onCallStateChanged:
   - OFFHOOK (з'єднані):
       Handler.postDelayed(1000) {
           MediaPlayer.create(context, selectedAudioUri).start()
       }
   - IDLE (завершено):
       val endTime = System.currentTimeMillis()
       val duration = endTime - startTime
       saveCallLog(startTime, endTime, duration)
       sendReport(startTime, endTime, duration)  // POST /api/debetor_report
       if (queue.isNotEmpty()) dialNext()
4. Відправити звіт (CallReportDto) на сервер
```

> **Примітка:** Починаючи з Android 10+, для відтворення аудіо під час дзвінка може знадобитися
> `AudioManager.setMode(MODE_IN_CALL)` і `AudioManager.setSpeakerphoneOn(true)`.

---

## 8. API-інтеграція (Retrofit)

### Endpoint 1: Отримати список боржників

```
POST https://itproject.com/api/debetors
Content-Type: application/json
Body: {}

Response: [
  { "id": 1, "name": "Іван Петров", "phone": "+380501234567", "debt": 1500.00 },
  ...
]
```

### Endpoint 2: Звіт про дзвінок

```
POST https://itproject.com/api/debetor_report
Content-Type: application/json
Body: {
  "phone": "+380501234567",
  "start_time": "2026-03-13T10:00:00Z",
  "end_time": "2026-03-13T10:01:35Z",
  "duration_seconds": 95,
  "duration_formatted": "1:35"
}
```

### ApiService.kt (Retrofit interface)

```kotlin
interface ApiService {
    @POST("debetors")
    suspend fun getDebtors(): Response<List<DebtorDto>>

    @POST("debetor_report")
    suspend fun sendCallReport(@Body report: CallReportDto): Response<Unit>
}
```

### NetworkModule.kt (Hilt)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val BASE_URL = "https://itproject.com/api/"

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
```

---

## 9. База даних Room

### AudioFileEntity

```kotlin
@Entity(tableName = "audio_files")
data class AudioFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val filePath: String,        // абсолютний шлях у filesDir
    val addedAt: Long            // Unix timestamp
)
```

### CallLogEntity

```kotlin
@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phone: String,
    val startTime: Long,         // Unix timestamp (ms)
    val endTime: Long,           // Unix timestamp (ms)
    val durationSeconds: Long,
    val status: String           // "answered", "no_answer", "failed"
)
```

---

## 10. Моделі домену

```kotlin
data class AudioFile(
    val id: Long,
    val fileName: String,
    val filePath: String
)

data class Debtor(
    val id: Long,
    val name: String,
    val phone: String,
    val debt: Double
)

data class CallReport(
    val phone: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long
) {
    val durationFormatted: String
        get() {
            val min = durationSeconds / 60
            val sec = durationSeconds % 60
            return "%d:%02d".format(min, sec)
        }
}
```

---

## 11. Gradle-залежності (app/build.gradle.kts)

```kotlin
// Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.09.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.activity:activity-compose:1.9.2")

// Navigation
implementation("androidx.navigation:navigation-compose:2.8.0")

// Hilt
implementation("com.google.dagger:hilt-android:2.51.1")
kapt("com.google.dagger:hilt-android-compiler:2.51.1")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

// Retrofit
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-gson:2.11.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

// Lifecycle / ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")

// Timber (логування)
implementation("com.jakewharton.timber:timber:5.0.1")

// Accompanist (дозволи у Compose)
implementation("com.google.accompanist:accompanist-permissions:0.34.0")
```

---

## 12. Обробка дозволів під час виконання

```kotlin
// У DebtorsScreen або MainActivity перед початком дзвінків:
val permissionsState = rememberMultiplePermissionsState(
    listOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE
    )
)

if (!permissionsState.allPermissionsGranted) {
    permissionsState.launchMultiplePermissionRequest()
}
```

---

## 13. Покрокова реалізація (Фази)

### Фаза 1 — Фундамент (~ 2 дні)
- [ ] Створення Android Studio проекту (Empty Compose Activity).
- [ ] Налаштування Gradle: Compose, Hilt, Room, Retrofit.
- [ ] Налаштування AppNavGraph, sealed class Screen.
- [ ] Реалізація SplashScreen з таймером 2.5 с.

### Фаза 2 — Налаштування аудіо (~ 2 дні)
- [ ] Room: `AudioFileEntity`, `AudioFileDao`, `AppDatabase`.
- [ ] `AudioRepositoryImpl`: копіювання файлу у filesDir, збереження в Room.
- [ ] `SettingsScreen` + `SettingsViewModel`: завантаження, список, вибір файлу.
- [ ] MediaPlayer: превью відтворення, програвач (Slider + кнопки).
- [ ] Запит дозволу `READ_MEDIA_AUDIO`.

### Фаза 3 — Мережа та боржники (~ 1 день)
- [ ] `NetworkModule` (Hilt), `ApiService`, DTO-класи.
- [ ] `DebtorRepositoryImpl`: POST `/api/debetors`, маппінг у `Debtor`.
- [ ] `DebtorsScreen` — режим "Боржники": кнопка → список у таблиці.
- [ ] Обробка станів Loading / Success / Error (CircularProgressIndicator, Snackbar).

### Фаза 4 — Сервіс дзвінків (~ 3 дні)
- [ ] `AutoDialerService` (ForegroundService) з чергою номерів.
- [ ] Запит дозволу `CALL_PHONE` + `READ_PHONE_STATE`.
- [ ] Інтеграція `PhoneStateListener` / `TelephonyCallback` (API 31+).
- [ ] Відтворення mp3 через `MediaPlayer` через 1 с після OFFHOOK.
- [ ] Таймер: фіксація startTime / endTime / duration.
- [ ] Room: збереження логів дзвінків (`CallLogEntity`).

### Фаза 5 — Звіти та лог (~ 1 день)
- [ ] `CallRepositoryImpl`: POST `/api/debetor_report` після кожного дзвінка.
- [ ] Відображення логу дзвінків у `DebtorsScreen` (LazyColumn).
- [ ] Форматування часу: `HH:mm:ss` та `м:сс` для тривалості.

### Фаза 6 — Режими вручну та список (~ 1 день)
- [ ] `DebtorsScreen` — режим "Ввести номер вручну": TextField + валідація + виклик.
- [ ] `DebtorsScreen` — режим "Введення номерів списком": Textarea + парсинг + черга.
- [ ] Зв'язок обох режимів з `AutoDialerService`.

### Фаза 7 — Полішінг та тестування (~ 2 дні)
- [ ] `ConfirmLogoutScreen`: скидання стану, навігація.
- [ ] Edge cases: дзвінок відхилено, немає мережі, пустий список.
- [ ] Сповіщення Foreground Service: прогрес "Дзвінок 3 з 10".
- [ ] Кнопка "Зупинити дзвінки" у нотифікації та на екрані.
- [ ] Тести ViewModel (JUnit + Turbine для Flow).
- [ ] UI-тести ключових екранів (Compose Testing).

---

## 14. Важливі технічні нюанси

### Дзвінки на Android 10+
- `ACTION_CALL` потребує дозволу `CALL_PHONE`.
- `TelecomManager.placeCall()` — рекомендований спосіб для Android 9+.
- Вбудований `ConnectionService` дає більше контролю (SAFE_CALL режим).

### Відтворення аудіо під час дзвінка
- Необхідно переключити `AudioManager` в режим `MODE_IN_CALL`.
- `AudioFocusRequest` для коректного повернення звуку після дзвінка.
- На деяких виробниках (Xiaomi, Samsung) потрібна перевірка MIUI/One UI специфіки.

### Черга дзвінків
- Між дзвінками рекомендована пауза 2-3 секунди (щоб система встигла обробити IDLE-стан).
- Використовувати `Handler.postDelayed` або `delay()` у корутинах.

### Безпека мережі
- Якщо сервер `itproject.com` використовує HTTPS із самопідписаним сертифікатом —
  необхідно налаштувати `network_security_config.xml`.
- Credentials / токени зберігати у `EncryptedSharedPreferences`, не в коді.

### Android 14+ обмеження
- Implicit broadcasts обмежені → використовувати явні BroadcastReceiver або TelephonyCallback.
- `startForegroundService` вимагає `ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL`.

---

## 15. Схема потоку даних

```
User Action
    │
    ▼
Compose UI  ──── events ────►  ViewModel
                                   │
                    ┌──────────────┼──────────────┐
                    ▼              ▼               ▼
              AudioRepository  DebtorRepository  CallRepository
                    │              │               │
              Room (local)    Retrofit (remote)  Retrofit (remote)
              filesDir        /api/debetors      /api/debetor_report
                                                      ▲
                                              AutoDialerService
                                              PhoneStateListener
                                              MediaPlayer
```

---

## 16. Структура файлів Gradle

```
PhoneNotificator/
└── kotlin-version/
    ├── settings.gradle.kts
    ├── build.gradle.kts           (project-level)
    └── app/
        ├── build.gradle.kts       (app-level)
        ├── proguard-rules.pro
        └── src/
            ├── main/
            │   ├── AndroidManifest.xml
            │   └── java/ ...
            └── test/ ...
```

---

*Документ створено: 13 березня 2026*
*Версія плану: 1.0*
