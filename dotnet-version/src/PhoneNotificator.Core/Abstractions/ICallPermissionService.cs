namespace PhoneNotificator.Core.Abstractions;

public interface ICallPermissionService
{
    Task<bool> EnsureGrantedAsync(CancellationToken ct = default);
}
