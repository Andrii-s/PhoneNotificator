using Android;
using Microsoft.Maui.ApplicationModel;
using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Platforms.Android.Services;

public sealed class AndroidCallPermissionService : ICallPermissionService
{
    public async Task<bool> EnsureGrantedAsync(CancellationToken ct = default)
    {
        var status = await Permissions.RequestAsync<PhoneCallPermission>();
        return status == PermissionStatus.Granted;
    }

    private sealed class PhoneCallPermission : Permissions.BasePlatformPermission
    {
        public override (string androidPermission, bool isRuntime)[] RequiredPermissions =>
        [
            (Manifest.Permission.CallPhone, true),
            (Manifest.Permission.ReadPhoneState, true),
            (Manifest.Permission.ReadCallLog, true),
        ];
    }
}
