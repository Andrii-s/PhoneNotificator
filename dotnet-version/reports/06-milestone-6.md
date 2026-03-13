# Milestone 6 Report

Date: 2026-03-13

## Scope

- Added test doubles for filesystem, preferences, file picker, navigation, toast, confirmation, dialer, call monitor, and HTTP message handling.
- Implemented unit tests for:
  - `AudioFileService`
  - `ApiService`
  - `CallReport`
  - `CallService`
  - `PreviewCallService`
  - `SettingsViewModel`
  - `DebtorsViewModel`
- Implemented integration tests for `ApiService` with `WireMock.Net`.
- Verified `XPlat Code Coverage` for the core library.

## Validation

- `dotnet test dotnet-version/tests/PhoneNotificator.Tests/PhoneNotificator.Tests.csproj --no-restore --filter "Category!=Integration"`
- `dotnet test dotnet-version/tests/PhoneNotificator.Tests/PhoneNotificator.Tests.csproj --no-restore --filter "Category=Integration"`
- `dotnet test dotnet-version/tests/PhoneNotificator.Tests/PhoneNotificator.Tests.csproj --no-restore --filter "Category!=Integration" --collect:"XPlat Code Coverage"`
- `dotnet build dotnet-version/src/PhoneNotificator/PhoneNotificator.csproj --no-restore -f net9.0-windows10.0.19041.0`

## Metrics

- Unit tests passed: 32
- Integration tests passed: 3
- Overall core line coverage: 88.13%

## Notes

- Coverage is measured against `PhoneNotificator.Core`.
- The Windows-target MAUI build still emits one XamlC warning in `SettingsPage.xaml` for the explicit-source command binding inside the item template.
