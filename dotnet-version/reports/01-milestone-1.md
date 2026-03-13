# Milestone 1 Report

Date: 2026-03-13

## Scope

- Created `PhoneNotificator.sln` inside `dotnet-version`.
- Added MAUI app project `src/PhoneNotificator`.
- Added shared core library `src/PhoneNotificator.Core`.
- Added test project skeleton `tests/PhoneNotificator.Tests`.
- Replaced template `MainPage` with routed pages: `SplashPage`, `SettingsPage`, `DebtorsPage`.
- Configured base DI in `MauiProgram.cs`.
- Added initial domain models and session abstraction in the core library.

## Notes

- The solution structure intentionally separates MAUI UI from testable core logic.
- Page layouts are placeholders for the next milestones and already match the planned routing.

## Validation

- `dotnet restore` was attempted for the solution and projects.
- Restore is currently blocked by sandbox network restrictions on `https://api.nuget.org/v3/index.json`.
- No compile validation is possible until package restore is allowed.
