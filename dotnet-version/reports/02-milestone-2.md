# Milestone 2 Report

Date: 2026-03-13

## Scope

- Added testable abstractions for file picking, preferences, navigation, toast notifications, confirmation dialogs, app shutdown, and app-local file storage.
- Implemented `AudioFileService` in `PhoneNotificator.Core` with JSON persistence in preferences and copied audio assets into `AppDataDirectory/audio`.
- Implemented `AudioPlayerService` in the MAUI app using `Plugin.Maui.Audio`.
- Replaced placeholder `SettingsViewModel` with working commands:
  - upload audio file
  - play/pause selected file
  - navigate to debtors page after selecting audio
  - confirm exit and quit application
- Replaced placeholder `SettingsPage.xaml` with the actual Settings screen layout.
- Added `ConfirmLogoutPopup` and a popup-backed confirmation service.

## Validation

- `dotnet restore dotnet-version/PhoneNotificator.sln -v minimal`
- `dotnet build dotnet-version/src/PhoneNotificator.Core/PhoneNotificator.Core.csproj --no-restore`
- `dotnet build dotnet-version/src/PhoneNotificator/PhoneNotificator.csproj --no-restore -f net9.0-windows10.0.19041.0`
- `dotnet build dotnet-version/tests/PhoneNotificator.Tests/PhoneNotificator.Tests.csproj --no-restore`

## Notes

- MAUI app build is currently validated against the Windows target framework because it is the locally runnable desktop target in this environment.
- The Settings page build still emits one XamlC warning for an item-template command binding with explicit `Source`; functionality builds and runs, but this binding can be refined later if a warning-free XAML build becomes a priority.
