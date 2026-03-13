# Звіт Фаза 1 — Фундамент

**Дата:** 2026-03-13  
**Статус:** ✅ Виконано

## Що зроблено

### Gradle налаштування
- `settings.gradle.kts` — налаштування репозиторіїв і підпроектів
- `build.gradle.kts` (project-level) — декларація плагінів через alias
- `gradle/libs.versions.toml` — version catalog з усіма залежностями
- `app/build.gradle.kts` — конфігурація модуля: minSdk 26, targetSdk 35, compileSdk 35
- `gradle/wrapper/gradle-wrapper.properties` — Gradle 8.9

### Маніфест
- `AndroidManifest.xml` — усі необхідні дозволи: CALL_PHONE, READ_PHONE_STATE, MANAGE_OWN_CALLS, READ_MEDIA_AUDIO, INTERNET, FOREGROUND_SERVICE, FOREGROUND_SERVICE_PHONE_CALL, MODIFY_AUDIO_SETTINGS
- MainActivity та AutoDialerService зареєстровані

### Application клас
- `AutoDialerApplication.kt` — @HiltAndroidApp + Timber ініціалізація (DebugTree для debug, ReleaseTree для release)

### MainActivity
- `MainActivity.kt` — @AndroidEntryPoint, edge-to-edge, запит дозволів при старті

### Навігація
- `navigation/AppNavGraph.kt` — sealed class Screen з маршрутами Splash/Settings/Debtors/ConfirmLogout
- NavHost з передачею audioFilePath параметра до DebtorsScreen

### SplashScreen (Phase 1 UI)
- `ui/splash/SplashScreen.kt` — анімована лінія завантаження, Phone іконка, назва, версія
- LaunchedEffect: затримка 2.5 с, потім перехід на Settings

### Тема
- `ui/theme/Color.kt` — Material3 кольорова палітра
- `ui/theme/Theme.kt` — AutoDialerTheme з dynamic color підтримкою
- `ui/theme/Type.kt` — повна типографічна шкала M3

### Ресурси
- `res/values/strings.xml` — усі рядки (українська мова)
- `res/values/themes.xml` — Material3 тема
- `res/values/colors.xml` — XML кольори
- `res/xml/backup_rules.xml` — правила резервного копіювання
- `res/xml/data_extraction_rules.xml` — правила вилучення даних

## Технічні рішення
- Gradle 8.9 (не 9.x) — стабільна сумісність з AGP 8.5.2
- Sealed class Screen — єдине джерело для маршрутів
- Kotlin 2.0 + kotlin.compose plugin — замість kotlinCompilerExtensionVersion
- START_NOT_STICKY для сервісу — не перезапускається після знищення
