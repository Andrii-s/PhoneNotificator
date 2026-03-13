using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Platforms.iOS.Services;

public sealed class IosCallPermissionService : ICallPermissionService
{
    public Task<bool> EnsureGrantedAsync(CancellationToken ct = default)
    {
        return Task.FromResult(true);
    }
}
