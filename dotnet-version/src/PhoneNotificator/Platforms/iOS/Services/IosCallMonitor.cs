using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Platforms.iOS.Services;

public sealed class IosCallMonitor : ICallMonitor
{
    private readonly CallObserver _callObserver;

    public IosCallMonitor(CallObserver callObserver)
    {
        _callObserver = callObserver;
    }

    public void Reset()
    {
        _ = _callObserver;
        IosCallMonitorState.Reset();
    }

    public Task WaitForConnectedAsync(CancellationToken ct = default)
    {
        return IosCallMonitorState.WaitForConnectedAsync(ct);
    }

    public Task WaitForEndedAsync(CancellationToken ct = default)
    {
        return IosCallMonitorState.WaitForEndedAsync(ct);
    }
}
