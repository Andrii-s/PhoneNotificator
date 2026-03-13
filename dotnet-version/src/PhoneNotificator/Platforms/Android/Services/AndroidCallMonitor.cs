using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Platforms.Android.Services;

public sealed class AndroidCallMonitor : ICallMonitor
{
    public void Reset()
    {
        AndroidCallMonitorState.Reset();
    }

    public Task WaitForConnectedAsync(CancellationToken ct = default)
    {
        return AndroidCallMonitorState.WaitForConnectedAsync(ct);
    }

    public Task WaitForEndedAsync(CancellationToken ct = default)
    {
        return AndroidCallMonitorState.WaitForEndedAsync(ct);
    }
}
