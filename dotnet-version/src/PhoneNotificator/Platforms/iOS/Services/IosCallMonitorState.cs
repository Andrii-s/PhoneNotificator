namespace PhoneNotificator.Platforms.iOS.Services;

internal static class IosCallMonitorState
{
    private static readonly object SyncRoot = new();
    private static TaskCompletionSource<bool> _connectedSource = CreateSource();
    private static TaskCompletionSource<bool> _endedSource = CreateSource();

    public static void Reset()
    {
        lock (SyncRoot)
        {
            _connectedSource = CreateSource();
            _endedSource = CreateSource();
        }
    }

    public static void MarkConnected()
    {
        lock (SyncRoot)
        {
            _connectedSource.TrySetResult(true);
        }
    }

    public static void MarkEnded()
    {
        lock (SyncRoot)
        {
            _endedSource.TrySetResult(true);
        }
    }

    public static Task WaitForConnectedAsync(CancellationToken ct = default)
    {
        return _connectedSource.Task.WaitAsync(ct);
    }

    public static Task WaitForEndedAsync(CancellationToken ct = default)
    {
        return _endedSource.Task.WaitAsync(ct);
    }

    private static TaskCompletionSource<bool> CreateSource()
    {
        return new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
    }
}
