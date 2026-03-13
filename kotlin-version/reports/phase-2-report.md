# Звіт Фаза 2 — Налаштування аудіо

**Дата:** 2026-03-13  
**Статус:** ✅ Виконано

## Що зроблено

### Room База Даних
- `data/local/entity/AudioFileEntity.kt` — @Entity(tableName="audio_files"): id, fileName, filePath, addedAt
- `data/local/dao/AudioFileDao.kt` — getAllFiles(): Flow, insert(), delete(), deleteById()
- `data/local/AppDatabase.kt` — @Database з AudioFileEntity, CallLogEntity; exportSchema=true

### AudioRepository
- `domain/repository/AudioRepository.kt` — інтерфейс: getAllAudioFiles(), importAudioFile(), deleteAudioFile()
- `data/repository/AudioRepositoryImpl.kt`:
  - importAudioFile: копіює Uri → filesDir/audio/, sanitize назви (захист від path traversal)
  - getAllAudioFiles: Flow з маппінгом entity → domain model
  - deleteAudioFile: видалення з Room

### SettingsScreen та SettingsViewModel
- `ui/settings/SettingsViewModel.kt`:
  - SettingsUiState: audioFiles, selectedFileId, isPlaying, currentPosition, duration, isLoading, error
  - MediaPlayer management: prepare() на Dispatchers.IO, start/pause/release
  - Position polling coroutine (кожні 200мс)
  - onPlayPause(), onSeek(), onSkipBack(-10с), onSkipForward(+10с)
  - onCleared() → releaseMediaPlayer()
- `ui/settings/SettingsScreen.kt`:
  - TopAppBar "Налаштування" + кнопка "Вийти"
  - Accompanist Permission (READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE)
  - ActivityResultContracts.GetContent("audio/*") для вибору файлу
  - LazyColumn: RadioButton + назва + Play/Pause + Delete для кожного файлу
  - AudioPlayerSection: Slider + SkipBack/Play/SkipForward кнопки
  - "Далі →" (активна тільки якщо файл вибрано)

## Технічні рішення
- MediaPlayer.prepare() — блокуючий, запускається на Dispatchers.IO
- Accompanist для декларативної обробки дозволів в Compose
- Flow<List<AudioFile>> — реактивний список файлів
- sanitize fileName: File(fileName).name запобігає path traversal атакам
