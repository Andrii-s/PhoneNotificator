namespace PhoneNotificator.Core.Abstractions;

public interface IConfirmationService
{
    Task<bool> ConfirmExitAsync(CancellationToken ct = default);
}
