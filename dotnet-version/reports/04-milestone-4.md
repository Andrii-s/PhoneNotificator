# Milestone 4 Report

Date: 2026-03-13

## Scope

- Added shared telephony abstractions:
  - `IPhoneDialerService`
  - `ICallMonitor`
  - `IAudioInjectionService`
  - `ICallPermissionService`
- Implemented shared `CallService` in `PhoneNotificator.Core`.
- Added Android runtime permission handling for `CALL_PHONE`, `READ_PHONE_STATE`, and `READ_CALL_LOG`.
- Added Android `CallReceiver` + state hub for monitoring call connection and completion.
- Added Android audio-routing helper for in-call playback preparation.
- Updated `AndroidManifest.xml` with required call-related permissions.
- Wired DI so Android can use the real telephony stack while non-mobile targets still use preview/no-op implementations.

## Validation

- `dotnet build dotnet-version/src/PhoneNotificator.Core/PhoneNotificator.Core.csproj --no-restore`
- `dotnet build dotnet-version/src/PhoneNotificator/PhoneNotificator.csproj --no-restore -f net9.0-windows10.0.19041.0`

## Notes

- Windows validation still uses the preview/non-mobile telephony path; Android-specific runtime behavior will require device or emulator verification later.
- iOS call observation and platform constraints are committed separately in Milestone 5.
