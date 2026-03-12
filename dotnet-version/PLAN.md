# PhoneNotificator — План розробки мобільного додатку (.NET MAUI)

> Складено на основі аналізу ескізу `front-template` (React/TypeScript).  
> Дата: 2026-03-13

---

## 1. Огляд проєкту

Мобільний додаток для автоматичного обдзвону боржників.  
Після вибору MP3-повідомлення в налаштуваннях, оператор запускає масовий або ручний дзвінок.  
При з'єднанні програє обрану аудіозапис адресату. Кожен дзвінок фіксується та надсилається на сервер.

---

## 2. Технологічний стек

| Шар             | Технологія                                                |
|-----------------|-----------------------------------------------------------|
| UI Framework    | .NET MAUI (Multi-platform App UI)                         |
| Мова            | C# 12 / .NET 9                                            |
| Архітектура     | MVVM + CommunityToolkit.Mvvm                              |
| DI              | Microsoft.Extensions.DependencyInjection (вбудований MAUI)|
| HTTP клієнт     | `IHttpClientFactory` + `System.Net.Http.HttpClient`       |
| JSON            | `System.Text.Json`                                        |
| Аудіо           | Plugin.Maui.Audio (відтворення локально)                  |
| Вибір файлу     | `FilePicker` (MAUI Essentials)                            |
| Збереження файлів | `FileSystem.AppDataDirectory` (локальне сховище)        |
| Телефонія       | `PhoneDialer` (MAUI Essentials) + Platform-specific (Android `TelephonyManager`, iOS `CallKit`) |
| Навігація       | Shell (MAUI Shell Navigation)                             |
| Тестування      | xUnit + Moq (unit), Appium (UI)                           |

---

## 3. Архітектура системи

```
┌─────────────────────────────────────────────────────────┐
│                     .NET MAUI App                       │
│                                                         │
│  ┌─────────────┐   ┌─────────────┐   ┌───────────────┐ │
│  │    Views    │   │ ViewModels  │   │   Services    │ │
│  │  (.xaml)   │◄──│ (MVVM)      │──►│ (Interfaces)  │ │
│  └─────────────┘   └─────────────┘   └───────────────┘ │
│                                              │          │
│                          ┌───────────────────┤          │
│                          ▼                   ▼          │
│                    ┌──────────┐      ┌─────────────┐   │
│                    │  Models  │      │  Platform   │   │
│                    │  (POCOs) │      │  (Android/  │   │
│                    └──────────┘      │   iOS impl) │   │
│                                      └─────────────┘   │
└─────────────────────────────────────────────────────────┘
         │                              │
         ▼                              ▼
  Local FileSystem              REST API Server
  (mp3 files in                (itproject.com)
   AppData)
```

### Патерн MVVM
- **View** — XAML-сторінки (прив'язка через `x:DataType`)
- **ViewModel** — успадковує `ObservableObject`, команди через `[RelayCommand]`
- **Model** — POCO-класи (AudioFile, Debtor, CallReport)
- **Service** — бізнес-логіка, ін'єкція через конструктор

---

## 4. Структура проєкту

```
PhoneNotificator/
├── PhoneNotificator.sln
│
├── src/
│   └── PhoneNotificator/                  # Main MAUI project
│       ├── MauiProgram.cs                 # DI registration, app bootstrap
│       ├── AppShell.xaml                  # Shell navigation routes
│       │
│       ├── Models/
│       │   ├── AudioFile.cs               # { Id, FileName, FilePath }
│       │   ├── Debtor.cs                  # { PhoneNumber, Name? }
│       │   └── CallReport.cs             # { PhoneNumber, StartTime, EndTime, Duration }
│       │
│       ├── Services/
│       │   ├── Interfaces/
│       │   │   ├── IAudioFileService.cs
│       │   │   ├── IAudioPlayerService.cs
│       │   │   ├── ICallService.cs
│       │   │   └── IApiService.cs
│       │   ├── AudioFileService.cs        # Збереження/зчитування mp3
│       │   ├── AudioPlayerService.cs      # Відтворення через Plugin.Maui.Audio
│       │   ├── CallService.cs             # Дзвінки + моніторинг з'єднання
│       │   └── ApiService.cs             # HTTP запити до сервера
│       │
│       ├── ViewModels/
│       │   ├── SettingsViewModel.cs
│       │   └── DebtorsViewModel.cs
│       │
│       ├── Views/
│       │   ├── SplashPage.xaml / .cs
│       │   ├── SettingsPage.xaml / .cs
│       │   ├── DebtorsPage.xaml / .cs
│       │   └── ConfirmLogoutPopup.xaml / .cs   # CommunityToolkit.Maui Popup
│       │
│       ├── Platforms/
│       │   ├── Android/
│       │   │   ├── CallReceiver.cs        # BroadcastReceiver: слухає стан дзвінка
│       │   │   └── AndroidAudioInjector.cs # AudioManager: ін'єкція аудіо в дзвінок
│       │   └── iOS/
│       │       └── CallObserver.cs        # CXCallObserver: статус дзвінка
│       │
│       └── Resources/
│           ├── Fonts/
│           ├── Images/
│           └── Styles/
│
└── tests/
    └── PhoneNotificator.Tests/
        ├── AudioFileServiceTests.cs
        ├── DebtorsViewModelTests.cs
        └── ApiServiceTests.cs
```

---

## 5. Сторінки та ViewModels

### 5.1 SplashPage
- **Призначення:** Заставка при запуску
- **Логіка:** Затримка 1.5с → Shell.GoToAsync(`//SettingsPage`)
- **ViewModel:** не потрібна (code-behind)

---

### 5.2 SettingsPage + SettingsViewModel

**UI елементи (аналог front-template):**

| Елемент               | MAUI контрол                          |
|-----------------------|---------------------------------------|
| Заголовок             | `Label` в `Grid` (header bar)         |
| Кнопка "Завантажити аудіофайл" | `Button` → `UploadFileCommand`  |
| Таблиця файлів        | `CollectionView` + `RadioButton`      |
| Кнопка Play/Pause     | `ImageButton` поруч з кожним файлом  |
| Аудіо-контролер       | `Slider` + `Label` (таймер)           |
| Кнопка "Далі →"       | `Button` → `NavigateToDebtorsCommand` |
| Кнопка "Вийти"        | `Button` → `ExitCommand`              |

**SettingsViewModel (властивості та команди):**

```csharp
// Властивості
ObservableCollection<AudioFile> AudioFiles
AudioFile? SelectedFile
AudioFile? PlayingFile
double PlaybackProgress        // 0.0–1.0
string PlaybackTimeLabel       // "0:45 / 2:30"

// Команди
[RelayCommand] Task UploadFileAsync()
[RelayCommand] Task TogglePlayAsync(AudioFile file)
[RelayCommand] void NavigateToDebtors()
[RelayCommand] Task ShowExitConfirmationAsync()
```

**Логіка UploadFileAsync:**
```
1. FilePicker.PickAsync() → фільтр .mp3, .wav, .aac
2. Скопіювати файл у FileSystem.AppDataDirectory/audio/
3. Додати AudioFile до ObservableCollection
4. Зберегти список у Preferences (JSON-серіалізований список шляхів)
```

**Логіка ShowExitConfirmationAsync:**
```
1. Показати CommunityToolkit.Maui Popup (ConfirmLogoutPopup)
2. Якщо результат == true → Application.Current.Quit()
3. Якщо результат == false → закрити Popup, залишитись на сторінці
```

---

### 5.3 ConfirmLogoutPopup

- Реалізація через `CommunityToolkit.Maui.Views.Popup`
- Повертає `bool` результат (Так/Ні)
- Кнопка "Так" → `CloseAsync(true)`
- Кнопка "Ні" → `CloseAsync(false)`

---

### 5.4 DebtorsPage + DebtorsViewModel

**UI елементи:**

| Елемент                         | MAUI контрол                          |
|---------------------------------|---------------------------------------|
| Заголовок                       | `Label` в header `Grid`              |
| Вибір режиму                    | `Picker` або `RadioButtonGroup`       |
| **Режим "Боржники":** кнопка "Отримати список" | `Button` → `FetchDebtorsCommand` |
| Список боржників                | `CollectionView`                      |
| Кнопка "Почати дзвінки"        | `Button` → `StartCallsCommand`       |
| **Режим "Вручну":** поле номера | `Entry` (type=Telephone)             |
| Кнопка "Почати дзвінок"        | `Button` → `StartSingleCallCommand`  |
| **Режим "Список":** textarea    | `Editor` (multiline)                  |
| Кнопка "Почати дзвінки"        | `Button` → `StartCallsFromListCommand`|
| Журнал виконання                | `CollectionView` (лог-рядки)          |
| Кнопка "Повернутись"           | `Button` → `GoBackCommand`           |

**Enum режимів:**
```csharp
public enum SendMode { Debtors, Manual, List }
```

**DebtorsViewModel (властивості та команди):**

```csharp
// Властивості
SendMode SelectedMode
ObservableCollection<Debtor> Debtors
string ManualPhoneNumber
string ListPhoneNumbers
bool IsCallsRunning
ObservableCollection<string> CallLog

// Команди
[RelayCommand] Task FetchDebtorsAsync()
[RelayCommand] Task StartCallsAsync()
[RelayCommand] Task StartSingleCallAsync()
[RelayCommand] Task StartCallsFromListAsync()
[RelayCommand] void GoBack()
```

---

## 6. Моделі даних

```csharp
// Models/AudioFile.cs
public class AudioFile
{
    public string Id { get; set; } = Guid.NewGuid().ToString();
    public string FileName { get; set; } = string.Empty;
    public string FilePath { get; set; } = string.Empty;
}

// Models/Debtor.cs
public class Debtor
{
    public string PhoneNumber { get; set; } = string.Empty;
    public string? Name { get; set; }
}

// Models/CallReport.cs
public class CallReport
{
    public string PhoneNumber { get; set; } = string.Empty;
    public DateTime StartTime { get; set; }
    public DateTime EndTime { get; set; }
    public TimeSpan Duration => EndTime - StartTime;
    public string DurationFormatted => $"{(int)Duration.TotalMinutes}хв {Duration.Seconds}сек";
}
```

---

## 7. Сервіси

### 7.1 IAudioFileService

```csharp
public interface IAudioFileService
{
    Task<AudioFile> ImportFileAsync(FileResult file);
    IReadOnlyList<AudioFile> GetAllFiles();
    void DeleteFile(AudioFile file);
}
```

**Реалізація:**
- Копіювання у `FileSystem.AppDataDirectory/audio/`
- Список файлів зберігається у `Preferences["audio_files"]` (JSON)

---

### 7.2 IAudioPlayerService

```csharp
public interface IAudioPlayerService
{
    Task PlayAsync(string filePath);
    Task PauseAsync();
    Task StopAsync();
    double GetProgress();           // 0.0–1.0
    TimeSpan GetCurrentPosition();
    TimeSpan GetTotalDuration();
    event EventHandler PlaybackCompleted;
}
```

**Реалізація:** `Plugin.Maui.Audio.IAudioManager`

---

### 7.3 ICallService

```csharp
public interface ICallService
{
    Task MakeCallAsync(string phoneNumber, string audioFilePath,
                       Action<CallReport> onCallCompleted,
                       CancellationToken ct = default);

    Task MakeCallsSequentialAsync(IEnumerable<string> phoneNumbers,
                                  string audioFilePath,
                                  Action<CallReport> onEachCallCompleted,
                                  CancellationToken ct = default);
}
```

**Логіка одного дзвінка:**
```
1. Зафіксувати StartTime = DateTime.Now
2. Ініціювати дзвінок через PhoneDialer / Platform Intent
3. Слухати стан телефонії:
   - Android: BroadcastReceiver (TelephonyManager.EXTRA_STATE_OFFHOOK)
   - iOS: CXCallObserver.callChanged (hasConnected = true)
4. При з'єднанні → await Task.Delay(1000)
5. Відтворити mp3 через AudioPlayer (у режимі VOICE_CALL на Android)
6. Слухати завершення дзвінка (EXTRA_STATE_IDLE / hasEnded = true)
7. Зафіксувати EndTime = DateTime.Now
8. Побудувати CallReport і викликати callback onCallCompleted
```

> ⚠️ **Обмеження платформ:**  
> - Android вимагає дозволів `CALL_PHONE`, `READ_CALL_LOG`, `READ_PHONE_STATE`  
> - Ін'єкція аудіо в потік дзвінка потребує `AudioManager.setMode(MODE_IN_CALL)` і відтворення через earpiece/speaker — адресат чує через мікрофон пристрою  
> - iOS: автовиклик неможливий без VoIP entitlement; реальний дзвінок відкриває Phone.app  
> - Альтернатива для production: замінити на VoIP SDK (Twilio / Vonage) для прямої ін'єкції аудіо

---

### 7.4 IApiService

```csharp
public interface IApiService
{
    Task<IReadOnlyList<Debtor>> GetDebtorsAsync(CancellationToken ct = default);
    Task SendCallReportAsync(CallReport report, CancellationToken ct = default);
}
```

**Endpoints:**

| Метод | URL                                        | Body / Response                     |
|-------|--------------------------------------------|-------------------------------------|
| POST  | `https://itproject.com/api/debetors`       | `{}` → `[{ phoneNumber, name? }]`  |
| POST  | `https://itproject.com/api/debetor_report` | `{ phoneNumber, startTime, endTime, durationFormatted }` → 200 OK |

**Реалізація ApiService:**
- `IHttpClientFactory` з named client `"itproject"`
- `BaseAddress = "https://itproject.com"`
- Timeout: 30 секунд
- Retry: Polly (3 спроби з exponential backoff)

---

## 8. API Контракти (JSON)

### GET деборів (відповідь сервера)
```json
[
  { "phoneNumber": "+380931231212", "name": "Іваненко І.І." },
  { "phoneNumber": "+380671234567" }
]
```

### POST звіт про дзвінок
```json
{
  "phoneNumber": "+380931231212",
  "startTime": "2026-03-13T14:01:00Z",
  "endTime": "2026-03-13T14:03:30Z",
  "durationFormatted": "2хв 30сек"
}
```

---

## 9. Реєстрація в DI (MauiProgram.cs)

```csharp
builder.Services
    // Services
    .AddSingleton<IAudioFileService, AudioFileService>()
    .AddSingleton<IAudioPlayerService, AudioPlayerService>()
    .AddSingleton<ICallService, CallService>()
    .AddSingleton<IApiService, ApiService>()

    // HTTP
    .AddHttpClient("itproject", c =>
    {
        c.BaseAddress = new Uri("https://itproject.com");
        c.Timeout = TimeSpan.FromSeconds(30);
    })

    // ViewModels
    .AddTransient<SettingsViewModel>()
    .AddTransient<DebtorsViewModel>()

    // Pages
    .AddTransient<SettingsPage>()
    .AddTransient<DebtorsPage>();
```

---

## 10. Shell Navigation (AppShell.xaml)

```xml
<Shell>
    <ShellContent Route="SplashPage"   ContentTemplate="{DataTemplate views:SplashPage}" />
    <ShellContent Route="SettingsPage" ContentTemplate="{DataTemplate views:SettingsPage}" />
    <ShellContent Route="DebtorsPage"  ContentTemplate="{DataTemplate views:DebtorsPage}" />
</Shell>
```

Навігація:
- `SplashPage` → `Shell.GoToAsync("//SettingsPage")`
- `SettingsPage` → `Shell.GoToAsync("//DebtorsPage")`
- `DebtorsPage` → `Shell.GoToAsync("..")` (назад)
- Logout Popup: `CommunityToolkit.Maui.Views.Popup` (не Shell-маршрут)

---

## 11. Дозволи (Android)

**AndroidManifest.xml:**
```xml
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

**Runtime permission запит (перед першим дзвінком):**
```csharp
var status = await Permissions.RequestAsync<Permissions.Phone>();
if (status != PermissionStatus.Granted) { /* показати пояснення */ }
```

---

## 12. NuGet пакети

| Пакет                              | Версія  | Призначення                         |
|------------------------------------|---------|-------------------------------------|
| `CommunityToolkit.Mvvm`            | 8.x     | ObservableObject, RelayCommand      |
| `CommunityToolkit.Maui`            | 9.x     | Popup, Toast, MediaElement          |
| `Plugin.Maui.Audio`                | 3.x     | Відтворення аудіо                   |
| `Microsoft.Extensions.Http.Polly`  | 8.x     | Retry-політики HTTP                 |
| `System.Text.Json`                 | (вбудований) | JSON серіалізація            |
| `xunit`                            | 2.x     | Unit тести                          |
| `Moq`                              | 4.x     | Mock-об'єкти у тестах               |
| `FluentAssertions`                 | 6.x     | Читабельні перевірки (`Should().Be`)|
| `Microsoft.NET.Test.Sdk`           | 17.x    | Запуск тестів через `dotnet test`   |
| `WireMock.Net`                     | 1.x     | Мок HTTP-сервер для інтеграційних тестів |
| `coverlet.collector`               | 6.x     | Вимір покриття коду (coverage)      |

---

## 13. Потоки виконання

### Потік: "Почати дзвінки" (режим "Боржники")

```
DebtorsViewModel.StartCallsAsync()
    │
    ├─ перевірити SelectedFile != null
    ├─ перевірити Debtors.Count > 0
    ├─ запросити дозвіл CALL_PHONE
    │
    └─ CallService.MakeCallsSequentialAsync(
            phoneNumbers: Debtors.Select(d => d.PhoneNumber),
            audioFilePath: SelectedFile.FilePath,
            onEachCallCompleted: report =>
            {
                CallLog.Add($"[{report.StartTime:HH:mm}] {report.PhoneNumber} — {report.DurationFormatted}");
                ApiService.SendCallReportAsync(report);
            }
       )
```

### Потік: Один дзвінок всередині CallService

```
1. StartTime = DateTime.Now
2. Platform.InvokeOnMainThread(() => PhoneDialer.Open(number))
       │  (Android: Intent.ACTION_CALL запускає дзвінок одразу)
       │
3. BroadcastReceiver / CXCallObserver чекає на OFFHOOK signal
       │
4. await Task.Delay(1000)          // 1 секунда після з'єднання
       │
5. AudioPlayerService.PlayAsync(audioFilePath)
       │  (AudioManager.Mode = IN_CALL → звук у вухо → мікрофон → адресат чує)
       │
6. Чекаємо на IDLE / hasEnded
       │
7. EndTime = DateTime.Now
8. CallReport report = new(number, StartTime, EndTime)
9. onCallCompleted(report)
```

---

## 14. Тестування

### 14.1 Структура тестового проєкту

```
tests/
└── PhoneNotificator.Tests/
    ├── PhoneNotificator.Tests.csproj
    │
    ├── Services/
    │   ├── AudioFileServiceTests.cs
    │   ├── AudioPlayerServiceTests.cs
    │   ├── ApiServiceTests.cs
    │   └── CallServiceTests.cs
    │
    ├── ViewModels/
    │   ├── SettingsViewModelTests.cs
    │   └── DebtorsViewModelTests.cs
    │
    ├── Models/
    │   └── CallReportTests.cs
    │
    ├── Integration/
    │   └── ApiServiceIntegrationTests.cs
    │
    └── Helpers/
        ├── FakeFileSystem.cs           # stub для FileSystem.AppDataDirectory
        ├── FakePreferences.cs          # stub для Preferences
        ├── FakeFileResult.cs           # stub для FileResult
        ├── FakeCallMonitor.cs          # stub для стану телефонії
        └── HttpMessageHandlerStub.cs   # підміна HTTP-відповіді
```

---

### 14.2 Unit-тести: AudioFileService

**Файл:** `Services/AudioFileServiceTests.cs`

| # | Метод             | Сценарій                                          | Очікуваний результат                            |
|---|-------------------|---------------------------------------------------|-------------------------------------------------|
| 1 | `ImportFileAsync` | Передаємо валідний `.mp3` FileResult              | Файл скопійований у `AppData/audio/`            |
| 2 | `ImportFileAsync` | Передаємо файл з розширенням `.txt`               | Кидає `InvalidOperationException`               |
| 3 | `ImportFileAsync` | Передаємо файл розміром > 10MB                    | Кидає `FileTooLargeException`                   |
| 4 | `GetAllFiles`     | Після двох імпортів                               | Повертає список з 2-х файлів                    |
| 5 | `GetAllFiles`     | `Preferences` порожні (перший запуск)             | Повертає порожній список                        |
| 6 | `DeleteFile`      | Файл існує                                        | Фізично видаляє файл, прибирає із `Preferences` |
| 7 | `DeleteFile`      | Файл не існує                                     | Не кидає виняток (idempotent)                   |
| 8 | `ImportFileAsync` | Файл з такою ж назвою вже є                       | Зберігає з суфіксом `_1` (без перезапису)       |

```csharp
[Fact]
public async Task ImportFileAsync_ValidMp3_SavesFileToAppDataDirectory()
{
    // Arrange
    var fakeFs = new FakeFileSystem();
    var service = new AudioFileService(fakeFs, new FakePreferences());
    var fileResult = new FakeFileResult("test.mp3", sizeBytes: 1024);

    // Act
    var audioFile = await service.ImportFileAsync(fileResult);

    // Assert
    audioFile.FileName.Should().Be("test.mp3");
    audioFile.FilePath.Should().StartWith(fakeFs.AudioDirectory);
    fakeFs.FileExists(audioFile.FilePath).Should().BeTrue();
}

[Fact]
public async Task ImportFileAsync_InvalidExtension_ThrowsInvalidOperationException()
{
    var service = new AudioFileService(new FakeFileSystem(), new FakePreferences());
    var fileResult = new FakeFileResult("document.txt", sizeBytes: 512);

    await Assert.ThrowsAsync<InvalidOperationException>(
        () => service.ImportFileAsync(fileResult));
}
```

---

### 14.3 Unit-тести: ApiService

**Файл:** `Services/ApiServiceTests.cs`

| # | Метод                 | Сценарій                                       | Очікуваний результат                              |
|---|-----------------------|------------------------------------------------|---------------------------------------------------|
| 1 | `GetDebtorsAsync`     | Сервер повертає 200 + валідний JSON            | Список `Debtor` десеріалізовано правильно         |
| 2 | `GetDebtorsAsync`     | Сервер повертає 200 + порожній масив `[]`      | Повертає порожній список                          |
| 3 | `GetDebtorsAsync`     | Сервер повертає 500                            | Кидає `HttpRequestException`                      |
| 4 | `GetDebtorsAsync`     | Timeout (відповідь > 30с)                      | Кидає `TaskCanceledException`                     |
| 5 | `SendCallReportAsync` | Валідний `CallReport`                          | POST надіслано на `/api/debetor_report`           |
| 6 | `SendCallReportAsync` | Перевірка тіла запиту                          | JSON містить `phoneNumber`, `startTime`, `endTime`, `durationFormatted` |
| 7 | `SendCallReportAsync` | Сервер повертає 200                            | Метод завершується без винятку                    |
| 8 | `SendCallReportAsync` | Сервер повертає 4xx                            | Кидає `HttpRequestException`                      |

```csharp
[Fact]
public async Task GetDebtorsAsync_ValidJson_ReturnsDeserializedDebtors()
{
    // Arrange
    var json = """[{"phoneNumber":"+380931231212","name":"Іваненко"},{"phoneNumber":"+380671234567"}]""";
    var handler = new HttpMessageHandlerStub(HttpStatusCode.OK, json);
    var client = new HttpClient(handler) { BaseAddress = new Uri("https://itproject.com") };
    var service = new ApiService(client);

    // Act
    var debtors = await service.GetDebtorsAsync();

    // Assert
    debtors.Should().HaveCount(2);
    debtors[0].PhoneNumber.Should().Be("+380931231212");
    debtors[0].Name.Should().Be("Іваненко");
    debtors[1].Name.Should().BeNull();
}

[Fact]
public async Task SendCallReportAsync_ValidReport_PostsCorrectJsonBody()
{
    // Arrange
    string? capturedBody = null;
    var handler = new HttpMessageHandlerStub(HttpStatusCode.OK, "{}",
        onRequest: async req => capturedBody = await req.Content!.ReadAsStringAsync());
    var client = new HttpClient(handler) { BaseAddress = new Uri("https://itproject.com") };
    var service = new ApiService(client);
    var report = new CallReport
    {
        PhoneNumber = "+380931231212",
        StartTime   = new DateTime(2026, 3, 13, 14, 0, 0, DateTimeKind.Utc),
        EndTime     = new DateTime(2026, 3, 13, 14, 2, 30, DateTimeKind.Utc)
    };

    // Act
    await service.SendCallReportAsync(report);

    // Assert
    capturedBody.Should().Contain("\"phoneNumber\":\"+380931231212\"");
    capturedBody.Should().Contain("\"durationFormatted\":\"2хв 30сек\"");
}
```

---

### 14.4 Unit-тести: CallService

**Файл:** `Services/CallServiceTests.cs`

| # | Метод                      | Сценарій                                           | Очікуваний результат                              |
|---|----------------------------|----------------------------------------------------|---------------------------------------------------|
| 1 | `MakeCallAsync`            | Дзвінок підключився і завершився                   | `CallReport` має StartTime < EndTime              |
| 2 | `MakeCallAsync`            | Обчислення тривалості 2хв 30сек                    | `DurationFormatted == "2хв 30сек"`               |
| 3 | `MakeCallAsync`            | Після з'єднання чекає 1 секунду до аудіо           | `PlayAsync` викликається через ≥1000ms            |
| 4 | `MakeCallAsync`            | `CancellationToken` скасовано до з'єднання         | Метод завершується, дзвінок не відбувається       |
| 5 | `MakeCallsSequentialAsync` | Список із 3 номерів                                | `onCallCompleted` викликається 3 рази             |
| 6 | `MakeCallsSequentialAsync` | Список із 3 номерів                                | Дзвінки йдуть по черзі (не паралельно)            |
| 7 | `MakeCallAsync`            | `audioFilePath` не існує                           | Кидає `FileNotFoundException`                     |

```csharp
[Fact]
public async Task MakeCallAsync_AfterConnect_WaitsOneSecondBeforeAudio()
{
    // Arrange
    var audioPlayer = new Mock<IAudioPlayerService>();
    var phoneDialer = new Mock<IPhoneDialerService>();
    var callMonitor = new FakeCallMonitor(); // симулює миттєве підключення
    var service = new CallService(audioPlayer.Object, phoneDialer.Object, callMonitor);

    DateTime? playCalledAt = null;
    DateTime connectTime = DateTime.MinValue;

    callMonitor.OnConnected = () => connectTime = DateTime.Now;
    audioPlayer.Setup(p => p.PlayAsync(It.IsAny<string>()))
               .Callback(() => playCalledAt = DateTime.Now)
               .Returns(Task.CompletedTask);

    // Act
    await service.MakeCallAsync("+380001112233", "audio.mp3", _ => { });

    // Assert
    (playCalledAt!.Value - connectTime).TotalMilliseconds
        .Should().BeGreaterOrEqualTo(1000);
}

[Fact]
public async Task MakeCallsSequentialAsync_ThreeNumbers_CallsCompletedThreeTimes()
{
    // Arrange
    var audioPlayer = new Mock<IAudioPlayerService>();
    audioPlayer.Setup(p => p.PlayAsync(It.IsAny<string>())).Returns(Task.CompletedTask);
    var service = new CallService(audioPlayer.Object,
                                   new Mock<IPhoneDialerService>().Object,
                                   new FakeCallMonitor());
    var phones = new[] { "+380001112233", "+380002223344", "+380003334455" };
    var reportCount = 0;

    // Act
    await service.MakeCallsSequentialAsync(phones, "audio.mp3", _ => reportCount++);

    // Assert
    reportCount.Should().Be(3);
}
```

---

### 14.5 Unit-тести: SettingsViewModel

**Файл:** `ViewModels/SettingsViewModelTests.cs`

| # | Команда / Властивість         | Сценарій                                           | Очікуваний результат                              |
|---|-------------------------------|----------------------------------------------------|---------------------------------------------------|
| 1 | `UploadFileCommand`           | Вибирає валідний файл                              | `AudioFiles` збільшується на 1 елемент            |
| 2 | `UploadFileCommand`           | Користувач скасовує вибір (FilePicker → null)      | `AudioFiles` не змінюється                        |
| 3 | `TogglePlayCommand`           | Файл не грає → Play                                | `PlayingFile` == обраний файл                     |
| 4 | `TogglePlayCommand`           | Файл грає → Pause                                  | `PlayingFile` == null                             |
| 5 | `TogglePlayCommand`           | Граємо файл A → Play на файлі B                    | `PlayingFile` == B, файл A зупинений              |
| 6 | `NavigateToDebtorsCommand`    | `SelectedFile` == null                             | Навігація не відбувається, показується Toast      |
| 7 | `NavigateToDebtorsCommand`    | `SelectedFile` != null                             | `Shell.GoToAsync("//DebtorsPage")` викликано      |
| 8 | `ShowExitConfirmationCommand` | Popup повертає `true`                              | `Application.Quit()` викликано                    |
| 9 | `ShowExitConfirmationCommand` | Popup повертає `false`                             | `Application.Quit()` не викликано                 |

```csharp
[Fact]
public async Task UploadFileCommand_ValidFile_AddsToAudioFiles()
{
    // Arrange
    var audioFileService = new Mock<IAudioFileService>();
    var expectedFile = new AudioFile { Id = "1", FileName = "test.mp3" };
    audioFileService.Setup(s => s.ImportFileAsync(It.IsAny<FileResult>()))
                    .ReturnsAsync(expectedFile);

    var filePicker = new Mock<IFilePicker>();
    filePicker.Setup(p => p.PickAsync(It.IsAny<PickOptions>()))
              .ReturnsAsync(new FakeFileResult("test.mp3"));

    var vm = new SettingsViewModel(audioFileService.Object,
                                    Mock.Of<IAudioPlayerService>(),
                                    filePicker.Object,
                                    Mock.Of<INavigationService>());
    // Act
    await vm.UploadFileCommand.ExecuteAsync(null);

    // Assert
    vm.AudioFiles.Should().ContainSingle(f => f.FileName == "test.mp3");
}
```

---

### 14.6 Unit-тести: DebtorsViewModel

**Файл:** `ViewModels/DebtorsViewModelTests.cs`

| #  | Команда / Властивість       | Сценарій                                               | Очікуваний результат                               |
|----|-----------------------------|--------------------------------------------------------|----------------------------------------------------|
| 1  | `FetchDebtorsCommand`       | API повертає список боржників                          | `Debtors` заповнюється отриманими даними           |
| 2  | `FetchDebtorsCommand`       | API повертає 500                                       | `Debtors` порожній, `ErrorMessage` задано          |
| 3  | `FetchDebtorsCommand`       | `IsBusy` під час запиту                                | `IsBusy` true → виконання → false                  |
| 4  | `StartCallsCommand`         | `Debtors` порожній (режим "Боржники")                  | Дзвінки не запускаються, Toast з попередженням     |
| 5  | `StartCallsCommand`         | `Debtors` має 3 номери                                 | `MakeCallsSequentialAsync` викликано з 3 номерами  |
| 6  | `StartSingleCallCommand`    | `ManualPhoneNumber` порожній                           | Команда не запускає дзвінок                        |
| 7  | `StartSingleCallCommand`    | `ManualPhoneNumber` = "+380001112233"                  | `MakeCallAsync` викликано з цим номером            |
| 8  | `StartCallsFromListCommand` | `ListPhoneNumbers` містить 3 номери через `\n`         | `MakeCallsSequentialAsync` отримує 3 номери        |
| 9  | `StartCallsFromListCommand` | Рядки з пробілами та пустими лініями                   | Пусті рядки відфільтровані                         |
| 10 | `CallLog` (після дзвінка)   | Дзвінок завершився                                     | Запис містить номер, час початку, тривалість       |
| 11 | `GoBackCommand`             | Виклик                                                 | Shell навігація `..` виконана                      |

```csharp
[Fact]
public async Task StartCallsFromListCommand_FiltersEmptyLines()
{
    // Arrange
    var callService = new Mock<ICallService>();
    var vm = new DebtorsViewModel(Mock.Of<IApiService>(), callService.Object,
                                   Mock.Of<INavigationService>());
    vm.SelectedMode = SendMode.List;
    vm.ListPhoneNumbers = "+380001112233\n\n  \n+380002223344";
    vm.SelectedFile = new AudioFile { FilePath = "test.mp3" };

    // Act
    await vm.StartCallsFromListCommand.ExecuteAsync(null);

    // Assert
    callService.Verify(s => s.MakeCallsSequentialAsync(
        It.Is<IEnumerable<string>>(list => list.Count() == 2),
        "test.mp3",
        It.IsAny<Action<CallReport>>(),
        It.IsAny<CancellationToken>()), Times.Once);
}
```

---

### 14.7 Unit-тести: CallReport (Model)

**Файл:** `Models/CallReportTests.cs`

| # | Властивість         | StartTime → EndTime (різниця)  | Очікуваний `DurationFormatted` |
|---|---------------------|--------------------------------|-------------------------------|
| 1 | `DurationFormatted` | 30 секунд                      | `"0хв 30сек"`                 |
| 2 | `DurationFormatted` | 1 хвилина рівно                | `"1хв 0сек"`                  |
| 3 | `DurationFormatted` | 2 хвилини 30 секунд            | `"2хв 30сек"`                 |
| 4 | `DurationFormatted` | 10 хвилин 5 секунд             | `"10хв 5сек"`                 |
| 5 | `Duration`          | 150 секунд                     | `TimeSpan(0, 2, 30)`          |

---

### 14.8 Інтеграційні тести: ApiService

**Файл:** `Integration/ApiServiceIntegrationTests.cs`

> Використовують `WireMock.Net` для мок-сервера. Позначені `[Trait("Category", "Integration")]`.

| # | Тест                               | Опис                                                          |
|---|------------------------------------|---------------------------------------------------------------|
| 1 | Реальний JSON → `GetDebtorsAsync`  | WireMock повертає коректний JSON — десеріалізація             |
| 2 | Retry при 503                      | WireMock 503 двічі, потім 200 — Polly має відновитись         |
| 3 | Timeout                            | WireMock затримує 31с — очікуємо `TaskCanceledException`      |
| 4 | `SendCallReportAsync` → POST body  | WireMock перехоплює запит та перевіряє JSON-поля              |

---

### 14.9 Конфігурація тестового проєкту

**`PhoneNotificator.Tests.csproj`**
```xml
<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <TargetFramework>net9.0</TargetFramework>
    <IsTestProject>true</IsTestProject>
    <Nullable>enable</Nullable>
  </PropertyGroup>

  <ItemGroup>
    <PackageReference Include="Microsoft.NET.Test.Sdk"    Version="17.*" />
    <PackageReference Include="xunit"                     Version="2.*" />
    <PackageReference Include="xunit.runner.visualstudio" Version="2.*" />
    <PackageReference Include="Moq"                       Version="4.*" />
    <PackageReference Include="FluentAssertions"          Version="6.*" />
    <PackageReference Include="WireMock.Net"              Version="1.*" />
    <PackageReference Include="coverlet.collector"        Version="6.*" />
  </ItemGroup>

  <ItemGroup>
    <ProjectReference Include="..\..\src\PhoneNotificator\PhoneNotificator.csproj" />
  </ItemGroup>
</Project>
```

**Запуск тестів:**
```bash
# Всі тести
dotnet test tests/PhoneNotificator.Tests

# Тільки unit-тести (без інтеграційних)
dotnet test --filter "Category!=Integration"

# З покриттям коду
dotnet test --collect:"XPlat Code Coverage"
reportgenerator -reports:"**/coverage.cobertura.xml" -targetdir:"coverage-report"
```

---

### 14.10 Цільове покриття коду

| Шар                      | Ціль   |
|--------------------------|--------|
| Models                   | ≥ 100% |
| Services                 | ≥ 85%  |
| ViewModels               | ≥ 80%  |
| Platforms (Android/iOS)  | ≥ 50%  |

---

## 15. Кроки реалізації (Milestones)

### Milestone 1 — Скелет проєкту
- [ ] Створити MAUI solution (`dotnet new maui -n PhoneNotificator`)
- [ ] Додати NuGet пакети
- [ ] Налаштувати DI в `MauiProgram.cs`
- [ ] Налаштувати `AppShell.xaml` з маршрутами

### Milestone 2 — SettingsPage
- [ ] Реалізувати `AudioFileService` (import, save, list)
- [ ] Реалізувати `AudioPlayerService` (play, pause, progress)
- [ ] Реалізувати `SettingsViewModel`
- [ ] Верстка `SettingsPage.xaml`
- [ ] Реалізувати `ConfirmLogoutPopup`

### Milestone 3 — DebtorsPage (UI + API)
- [ ] Реалізувати `ApiService` (GetDebtors, SendReport)
- [ ] Реалізувати `DebtorsViewModel` (всі 3 режими)
- [ ] Верстка `DebtorsPage.xaml` (Picker, 3 панелі, лог)

### Milestone 4 — Телефонія (Android)
- [ ] Додати `CALL_PHONE` дозволи та runtime-запит
- [ ] Реалізувати `CallReceiver` (BroadcastReceiver для стану дзвінка)
- [ ] Реалізувати `CallService` (sequential calls + timing)
- [ ] Ін'єкція аудіо через `AudioManager.MODE_IN_CALL`
- [ ] Відправка `CallReport` після кожного дзвінка

### Milestone 5 — iOS підтримка
- [ ] Реалізувати `CallObserver` (CXCallObserver)
- [ ] Додати VoIP Entitlement якщо потрібна пряма ін'єкція аудіо
- [ ] Перевірити `PhoneDialer` на iOS обмеження

### Milestone 6 — Тести та полірування
- [ ] Налаштувати `PhoneNotificator.Tests.csproj` (xUnit + Moq + FluentAssertions + WireMock.Net + coverlet)
- [ ] Реалізувати допоміжні stub-класи (`FakeFileSystem`, `FakePreferences`, `FakeCallMonitor`, `HttpMessageHandlerStub`)
- [ ] Unit-тести `AudioFileService` (розділ 14.2, сценарії 1–8)
- [ ] Unit-тести `ApiService` (розділ 14.3, сценарії 1–8)
- [ ] Unit-тести `CallReport` model (розділ 14.7, сценарії 1–5)
- [ ] Unit-тести `CallService` (розділ 14.4, сценарії 1–7)
- [ ] Unit-тести `SettingsViewModel` (розділ 14.5, сценарії 1–9)
- [ ] Unit-тести `DebtorsViewModel` (розділ 14.6, сценарії 1–11)
- [ ] Інтеграційні тести `ApiService` + WireMock.Net (розділ 14.8, сценарії 1–4)
- [ ] Налаштувати `coverlet` → перевірити покриття (цілі з розділу 14.10)
- [ ] Обробка помилок (HTTP timeout, немає з'єднання, відмова у дозволі)
- [ ] UX: індикатор завантаження (`ActivityIndicator`), Toast-повідомлення
- [ ] Перевірка на Android 12+ обмежень CALL_PHONE

---

## 16. Відомі ризики і рішення

| Ризик                                        | Рішення                                                      |
|----------------------------------------------|--------------------------------------------------------------|
| Android 12+ блокує фонові дзвінки           | Запускати сервіс як Foreground Service                       |
| iOS не дозволяє автоматичні дзвінки          | Відкривати через `tel://` URL (відкриває Phone.app)          |
| Аудіо не чути адресату                       | Використати VoIP SDK (Twilio/Vonage) для production-рішення  |
| Нестабільний Z'єднання API                   | Polly retry + offline-cache відповіді                        |
| Список боржників великий (500+)             | Пагінація + virtualized `CollectionView`                     |
| Файли mp3 займають пам'ять                  | Обмеження розміру файлу при імпорті (макс. 10MB)            |
