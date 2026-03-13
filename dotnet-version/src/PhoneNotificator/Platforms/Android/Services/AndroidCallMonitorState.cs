using Android.Telephony;

namespace PhoneNotificator.Platforms.Android.Services;

internal static class AndroidCallMonitorState
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

    public static Task WaitForConnectedAsync(CancellationToken ct = default)
    {
        return _connectedSource.Task.WaitAsync(ct);
    }

    public static Task WaitForEndedAsync(CancellationToken ct = default)
    {
        return _endedSource.Task.WaitAsync(ct);
    }

    public static void PublishState(string? state)
    {
        lock (SyncRoot)
        {
            if (state == TelephonyManager.ExtraStateOffhook)
            {
                _connectedSource.TrySetResult(true);
            }

            if (state == TelephonyManager.ExtraStateIdle && _connectedSource.Task.IsCompleted)
            {
                _endedSource.TrySetResult(true);
            }
        }
    }

    private static TaskCompletionSource<bool> CreateSource()
    {
        return new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
    }
}
