# Звіт Фаза 6 — Режими вручну та список

**Дата:** 2026-03-13  
**Статус:** ✅ Виконано

## Що зроблено

### DebtorsScreen — режим "Ввести номер вручну" (Tab 1)
- `OutlinedTextField` з label "Номер телефону"
- Валідація: кнопка "Почати дзвінок" активна тільки якщо рядок непустий
- PermissionAwareDialButton:
  - Перевіряє дозволи CALL_PHONE + READ_PHONE_STATE через `rememberMultiplePermissionsState`
  - Якщо дозволи надані → `viewModel.startDialing(listOf(manualNumber), audioFilePath)`
  - Якщо не надані → `launchMultiplePermissionRequest()`

### DebtorsScreen — режим "Введення номерів списком" (Tab 2)
- `OutlinedTextField` (multiline, minLines=5) з label "Список номерів (по одному на рядок)"
- Парсинг: `listNumbers.lines().map { it.trim() }.filter { it.isNotBlank() }`
- Кнопка "Почати дзвінки" активна тільки якщо є хоча б один номер
- Той самий PermissionAwareDialButton

### Зв'язок з AutoDialerService
- `DebtorsViewModel.startDialing(numbers: List<String>, audioFilePath: String)`:
  - `context.startForegroundService(intent)` з ACTION_START
  - `EXTRA_NUMBERS` = ArrayList<String>
  - `EXTRA_AUDIO_FILE_PATH` = шлях до аудіофайлу

### Зупинка обдзвону
- Кнопка "Зупинити дзвінки" показується коли `isDialing == true`
- `viewModel.stopDialing()` → `context.startService(ACTION_STOP intent)`

### Стан isDialing
- Ставиться в `true` при startDialing()
- Показує dialingProgress текст ("Розпочинаємо обдзвін X номерів...")
- Зупиняється через кнопку або автоматично (сервіс зупиняється сам)

## Технічні рішення
- `rememberMultiplePermissionsState` — декларативна перевірка дозволів
- lines().filter — стандартний парсинг списку номерів
- audioFilePath передається через навігацію (query parameter) від SettingsScreen
