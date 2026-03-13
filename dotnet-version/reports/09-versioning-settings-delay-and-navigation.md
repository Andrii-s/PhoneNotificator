# Versioning, Delay Setting, and Navigation Fixes

Date: 2026-03-13

## Scope

- Bumped the application version to `1.1.0` with build number `2`.
- Added versioned Android build artifacts in the output folder.
- Fixed text visibility for the notification mode picker and phone number inputs.
- Fixed the back navigation path from the debtors screen.
- Added a persisted integer setting for call audio start delay with default value `15`.
- Applied the configured delay before audio playback starts during a connected call.

## Implementation Notes

- `PhoneNotificator.csproj` now copies Android APK outputs to versioned names such as:
  - `com.andriis.phonenotificator-v1.1.0-build2-Signed.apk`
- `SettingsViewModel` now loads, validates, persists, and exposes the delay value.
- `CallService` now reads the delay from the shared app session and waits before starting playback.
- `ShellNavigationService` now falls back to `//SettingsPage` when Shell back stack navigation is unavailable.
- Android call audio preparation now uses a best-effort speakerphone route so the remote side can hear device playback through the microphone path.

## Validation

- `dotnet restore dotnet-version/PhoneNotificator.sln -v minimal`
- `dotnet test dotnet-version/tests/PhoneNotificator.Tests/PhoneNotificator.Tests.csproj --no-restore --filter "Category!=Integration"`
- `dotnet build dotnet-version/src/PhoneNotificator/PhoneNotificator.csproj -f net9.0-android -c Debug --no-restore /t:SignAndroidPackage -p:AndroidPackageFormat=apk`
- Installed `com.andriis.phonenotificator-v1.1.0-build2-Signed.apk` on emulator
- Launched with `adb shell am start -W -n com.andriis.phonenotificator/crc64a314207719197bd6.MainActivity`

## Result

- Unit tests passed: `35/35`
- Android app starts successfully on the emulator
- Installed package reports:
  - `versionName=1.1.0`
  - `versionCode=2`

## Limitation

The Android change improves real-device playback audibility during a phone call by routing playback through speakerphone, but it is still a best-effort workaround. A regular third-party app cannot guarantee direct injection of file audio into the carrier voice uplink for every device and Android version.
