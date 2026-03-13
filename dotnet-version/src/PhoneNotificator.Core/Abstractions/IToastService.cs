namespace PhoneNotificator.Core.Abstractions;

public interface IToastService
{
    Task ShowAsync(string message, CancellationToken ct = default);
}
