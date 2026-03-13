using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Tests.TestDoubles;

public sealed class FakeCallPermissionService : ICallPermissionService
{
    public bool IsGranted { get; set; } = true;

    public Task<bool> EnsureGrantedAsync(CancellationToken ct = default)
    {
        return Task.FromResult(IsGranted);
    }
}
