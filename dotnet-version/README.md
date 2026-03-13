# dotnet-version

`.NET MAUI` implementation of `PhoneNotificator` based on the project plan in [PLAN.md](./PLAN.md).

## Structure

- `src/PhoneNotificator` - MAUI application
- `src/PhoneNotificator.Core` - testable core logic, services, models, and viewmodels
- `tests/PhoneNotificator.Tests` - unit and integration tests
- `reports/` - per-milestone reports and final summary

## Restore

```powershell
$env:DOTNET_CLI_HOME='C:\Users\Andrii\PhoneNotificator\dotnet-version\.dotnet-cli-home'
dotnet restore dotnet-version/PhoneNotificator.sln
```

## Build

```powershell
$env:DOTNET_CLI_HOME='C:\Users\Andrii\PhoneNotificator\dotnet-version\.dotnet-cli-home'
dotnet build dotnet-version/src/PhoneNotificator/PhoneNotificator.csproj --no-restore -f net9.0-windows10.0.19041.0
```

## Tests

```powershell
$env:DOTNET_CLI_HOME='C:\Users\Andrii\PhoneNotificator\dotnet-version\.dotnet-cli-home'
dotnet test dotnet-version/tests/PhoneNotificator.Tests/PhoneNotificator.Tests.csproj --no-restore --filter "Category!=Integration"
dotnet test dotnet-version/tests/PhoneNotificator.Tests/PhoneNotificator.Tests.csproj --no-restore --filter "Category=Integration"
```

## Notes

- The Windows MAUI build is currently the validated desktop target in this environment.
- Android and iOS telephony layers are implemented, but real device validation is still required.
