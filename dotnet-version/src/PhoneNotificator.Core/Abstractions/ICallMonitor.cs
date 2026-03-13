namespace PhoneNotificator.Core.Abstractions;

public interface ICallMonitor
{
    void Reset();

    Task WaitForConnectedAsync(CancellationToken ct = default);

    Task WaitForEndedAsync(CancellationToken ct = default);
}
