# Milestone 3 Report

Date: 2026-03-13

## Scope

- Added `IApiService` and `ApiService` with:
  - debtor retrieval via `POST /api/debetors`
  - call report submission via `POST /api/debetor_report`
- Added `ICallService` contract and a temporary `PreviewCallService` implementation to exercise the Debtors flow before platform telephony is wired in Milestone 4.
- Replaced placeholder `DebtorsViewModel` with a working implementation for all 3 modes:
  - debtors list
  - manual phone number
  - phone numbers from multiline list
- Added execution log handling and API report submission after each completed call.
- Replaced placeholder `DebtorsPage.xaml` with the full screen layout from the plan/reference UI.
- Registered typed `HttpClient` + retry policy in `MauiProgram.cs`.

## Validation

- `dotnet build dotnet-version/src/PhoneNotificator.Core/PhoneNotificator.Core.csproj --no-restore`
- `dotnet build dotnet-version/src/PhoneNotificator/PhoneNotificator.csproj --no-restore -f net9.0-windows10.0.19041.0`

## Notes

- `PreviewCallService` is intentionally non-platform-specific. It simulates sequential calls and produces `CallReport` objects so that the Debtors UI, logging, and API-report pipeline are already integrated.
- One existing XamlC warning remains in `SettingsPage.xaml` for the item-template command binding with explicit `Source`.
