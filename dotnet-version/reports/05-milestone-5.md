# Milestone 5 Report

Date: 2026-03-13

## Scope

- Added iOS call-observation scaffolding based on `CXCallObserver`:
  - `CallObserver`
  - `IosCallMonitor`
  - `IosCallMonitorState`
- Added iOS permission shim and audio-injection shim implementations for the shared telephony abstractions.
- Wired the iOS branch in DI to use the shared `CallService` together with iOS-specific monitor/observer services.

## Validation

- Windows-target build still succeeds after the iOS additions:
  - `dotnet build dotnet-version/src/PhoneNotificator/PhoneNotificator.csproj --no-restore -f net9.0-windows10.0.19041.0`

## Notes

- iOS direct audio injection into native PSTN calls is still constrained by platform rules. The current iOS implementation focuses on observing call lifecycle and opening the native phone dialer path; a production-grade audio-injection story would require a VoIP-oriented approach and additional entitlements.
- These files were not device-validated in this environment because local build verification is limited to the Windows target framework.
