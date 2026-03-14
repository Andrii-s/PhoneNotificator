# 10. Call Flow And Android Audio Routing

## Summary

- Switched Android dialing to a direct `ACTION_CALL` flow with explicit permission handling.
- Removed the unused Android `READ_CALL_LOG` runtime requirement and manifest permission.
- Added a connection timeout and clearer user-facing errors when a call does not transition to the connected state.
- Added an Android-specific best-effort call audio player and modern communication-device routing.
- Bumped the Android app version to `1.1.2` (`build 4`) so the generated APK name reflects the new behavior.

## Files

- `src/PhoneNotificator/Platforms/Android/Services/AndroidPhoneDialerService.cs`
- `src/PhoneNotificator/Platforms/Android/Services/AndroidCallPermissionService.cs`
- `src/PhoneNotificator/Platforms/Android/Services/AndroidAudioPlayerService.cs`
- `src/PhoneNotificator/Platforms/Android/Services/AndroidAudioInjector.cs`
- `src/PhoneNotificator/Platforms/Android/AndroidManifest.xml`
- `src/PhoneNotificator/MauiProgram.cs`
- `src/PhoneNotificator/PhoneNotificator.csproj`
- `src/PhoneNotificator.Core/Services/CallService.cs`
- `src/PhoneNotificator.Core/ViewModels/DebtorsViewModel.cs`
- `tests/PhoneNotificator.Tests/CallServiceTests.cs`

## Verification

- `dotnet test dotnet-version/tests/PhoneNotificator.Tests/PhoneNotificator.Tests.csproj --no-restore --filter "Category!=Integration" -v minimal`
  - Passed: `36/36`
- `dotnet build dotnet-version/src/PhoneNotificator/PhoneNotificator.csproj -f net9.0-android -c Debug --no-restore /t:SignAndroidPackage -p:AndroidPackageFormat=apk -v minimal`
  - Succeeded
  - Remaining warning: `XC0025` in `src/PhoneNotificator/Views/SettingsPage.xaml`

## Notes

- The generated signed APK is `src/PhoneNotificator/bin/Debug/net9.0-android/com.andriis.phonenotificator-v1.1.2-build4-Signed.apk`.
- Emulator installation succeeded during verification, but the emulator disconnected before the final post-install launch check completed.
- Android public APIs do not provide a supported way for a regular third-party app to inject arbitrary file audio directly into the GSM uplink. The current implementation is the strongest best-effort routing/playback path available without privileged/system telephony access.
