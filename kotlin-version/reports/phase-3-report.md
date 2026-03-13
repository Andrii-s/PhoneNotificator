# Звіт Фаза 3 — Мережа та боржники

**Дата:** 2026-03-13  
**Статус:** ✅ Виконано

## Що зроблено

### NetworkModule (Hilt)
- `di/NetworkModule.kt`:
  - OkHttpClient з 30с таймаутами та HttpLoggingInterceptor (BODY для debug, NONE для release)
  - Retrofit з BASE_URL = "https://itproject.com/api/"
  - GsonConverterFactory
- `data/remote/ApiService.kt`:
  - `@POST("debetors")` → `Response<List<DebtorDto>>`
  - `@POST("debetor_report")` → `Response<Unit>`

### DTO класи
- `data/remote/dto/DebtorDto.kt` — Gson @SerializedName для: id, name, phone, debt
- `data/remote/dto/CallReportDto.kt` — phone, start_time, end_time, duration_seconds, duration_formatted

### DebtorRepository
- `domain/repository/DebtorRepository.kt` — інтерфейс: fetchDebtors(): List<Debtor>
- `data/repository/DebtorRepositoryImpl.kt`:
  - POST /api/debetors з порожнім тілом
  - Маппінг DebtorDto → Debtor
  - HTTP error codes обробляються як Exception

### DebtorsScreen — режим "Боржники"
- `ui/debtors/DebtorsViewModel.kt`:
  - DebtorsUiState: selectedMode, debtors, isLoadingDebtors, debtorsError, callLogs, isDialing
  - fetchDebtors(): Loading → Success/Error states
  - DialMode enum: DEBTORS, MANUAL, LIST
- `ui/debtors/DebtorsScreen.kt`:
  - TabRow: "Боржники" | "Ввести номер" | "Список номерів"
  - Tab DEBTORS: "Отримати список" → таблиця №/ПІБ/Номер/Борг
  - CircularProgressIndicator під час завантаження
  - Snackbar для відображення помилок
  - "Почати дзвінки" (активна коли є список)

## Технічні рішення
- `@POST` з пустим Map<String, Any> — коректно серіалізується Gson як {}
- DI через @Binds в AppModule — ефективніший підхід ніж @Provides для інтерфейсів
- Result<T> для sendCallReport — явна обробка помилок без throws
