# Android Startup Fix

Date: 2026-03-13

## Problem

The Android `Debug` APK installed successfully but crashed immediately on launch on a phone/emulator.

## Root Cause

The packaged app started without the required managed assemblies available to the Android runtime. In addition, the Android project reference flow propagated build properties that interfered with correct Android packaging.

## Changes

- Enabled `EmbedAssembliesIntoApk` for `Debug` Android builds in `PhoneNotificator.csproj`.
- Constrained the `PhoneNotificator.Core` project reference to `net9.0` and removed `RuntimeIdentifier` / `SelfContained` propagation from the MAUI Android build.
- Kept the Android receiver attribute fix in `CallReceiver.cs` required for a successful Android package build.

## Validation

- `dotnet build dotnet-version/src/PhoneNotificator/PhoneNotificator.csproj -f net9.0-android -c Debug --no-restore /t:SignAndroidPackage -p:AndroidPackageFormat=apk`
- `adb uninstall com.andriis.phonenotificator`
- `adb install -r dotnet-version/src/PhoneNotificator/bin/Debug/net9.0-android/com.andriis.phonenotificator-Signed.apk`
- `adb shell am start -W -n com.andriis.phonenotificator/crc64a314207719197bd6.MainActivity`

## Result

The application now installs and starts successfully on Android without crashing at launch.
