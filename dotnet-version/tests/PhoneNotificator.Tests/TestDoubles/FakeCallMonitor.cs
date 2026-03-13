using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Tests.TestDoubles;

public sealed class FakeCallMonitor : ICallMonitor
{
    public Action? OnReset { get; set; }

    public Action? OnConnected { get; set; }

    public Action? OnEnded { get; set; }

    public TimeSpan ConnectedDelay { get; set; } = TimeSpan.Zero;

    public TimeSpan EndedDelay { get; set; } = TimeSpan.Zero;

    public void Reset()
    {
        OnReset?.Invoke();
    }

    public async Task WaitForConnectedAsync(CancellationToken ct = default)
    {
        if (ConnectedDelay > TimeSpan.Zero)
        {
            await Task.Delay(ConnectedDelay, ct);
        }

        OnConnected?.Invoke();
    }

    public async Task WaitForEndedAsync(CancellationToken ct = default)
    {
        if (EndedDelay > TimeSpan.Zero)
        {
            await Task.Delay(EndedDelay, ct);
        }

        OnEnded?.Invoke();
    }
}
