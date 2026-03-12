# Звіт Фаза 5 — Звіти та лог дзвінків

**Дата:** 2026-03-13  
**Статус:** ✅ Виконано

## Що зроблено

### CallRepository
- `domain/repository/CallRepository.kt` — інтерфейс: getCallLogs(), saveCallLog(), sendCallReport()
- `data/repository/CallRepositoryImpl.kt`:
  - getCallLogs(): Flow<List<CallLog>> з Room через маппінг entity → domain
  - saveCallLog(): insert до Room на Dispatchers.IO
  - sendCallReport(): POST /api/debetor_report
    - Unix timestamp → ISO 8601 UTC (SimpleDateFormat)
    - Result<Unit> — явна обробка помилок
    - HTTP коди: isSuccessful → Result.success, інше → Result.failure

### Лог дзвінків у DebtorsScreen
- `ui/debtors/DebtorsScreen.kt` — секція логу:
  - "Лог дзвінків" заголовок
  - LazyColumn з CallLog entries (знизу вгору — reversed)
  - Кожен запис показує:
    - Номер телефону (жирний)
    - Час початку та кінця (HH:mm:ss формат)
    - Тривалість (м:сс — CallLog.durationFormatted)
    - Статус: ANSWERED=зелений, NO_ANSWER=жовтий, FAILED=червоний

### Форматування часу
- `CallReport.durationFormatted`: "%d:%02d".format(min, sec) — "1:35"
- `CallLog.durationFormatted`: аналогічно
- Час дзвінків форматується через `SimpleDateFormat("HH:mm:ss")` у Screen

### Room збереження
- `data/local/entity/CallLogEntity.kt` — status зберігається як String (ім'я enum значення)
- `data/local/dao/CallLogDao.kt` — getAllLogs(): Flow, insert(), clearAll()

## Технічні рішення
- ISO 8601 UTC формат для API запитів (стандарт RFC 3339)
- Flow-based реактивний лог — UI автоматично оновлюється при новому дзвінку
- Result<T> замість throws — явна обробка успіху/помилки в caller коді
- CallStatus.entries.firstOrNull — безпечна десеріалізація з бази даних
