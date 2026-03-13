using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Services;

public sealed class AlwaysGrantedCallPermissionService : ICallPermissionService
{
    public Task<bool> EnsureGrantedAsync(CancellationToken ct = default)
    {
        return Task.FromResult(true);
    }
}
