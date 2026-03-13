using PhoneNotificator.Core.Services.Interfaces;
using Plugin.Maui.Audio;

namespace PhoneNotificator.Services;

public sealed class AudioPlayerService : IAudioPlayerService
{
    private readonly IAudioManager _audioManager;

    private IAudioPlayer? _audioPlayer;
    private Stream? _audioStream;
    private CancellationTokenSource? _playbackMonitorCancellationTokenSource;

    public AudioPlayerService(IAudioManager audioManager)
    {
        _audioManager = audioManager;
    }

    public bool IsPlaying => _audioPlayer?.IsPlaying ?? false;

    public event EventHandler? PlaybackCompleted;

    public async Task PlayAsync(string filePath, CancellationToken ct = default)
    {
        if (!File.Exists(filePath))
        {
            throw new FileNotFoundException("Audio file was not found.", filePath);
        }

        await StopAsync(ct);

        _audioStream = File.OpenRead(filePath);
        _audioPlayer = _audioManager.CreatePlayer(_audioStream);
        _audioPlayer.Play();

        StartPlaybackMonitor();
    }

    public Task PauseAsync(CancellationToken ct = default)
    {
        _playbackMonitorCancellationTokenSource?.Cancel();
        _audioPlayer?.Pause();
        return Task.CompletedTask;
    }

    public Task StopAsync(CancellationToken ct = default)
    {
        _playbackMonitorCancellationTokenSource?.Cancel();
        _playbackMonitorCancellationTokenSource?.Dispose();
        _playbackMonitorCancellationTokenSource = null;

        _audioPlayer?.Stop();
        _audioPlayer?.Dispose();
        _audioPlayer = null;

        _audioStream?.Dispose();
        _audioStream = null;

        return Task.CompletedTask;
    }

    public double GetProgress()
    {
        if (_audioPlayer is null || _audioPlayer.Duration <= 0)
        {
            return 0;
        }

        return Math.Clamp(_audioPlayer.CurrentPosition / _audioPlayer.Duration, 0, 1);
    }

    public TimeSpan GetCurrentPosition()
    {
        return TimeSpan.FromSeconds(_audioPlayer?.CurrentPosition ?? 0);
    }

    public TimeSpan GetTotalDuration()
    {
        return TimeSpan.FromSeconds(_audioPlayer?.Duration ?? 0);
    }

    private void StartPlaybackMonitor()
    {
        _playbackMonitorCancellationTokenSource?.Cancel();
        _playbackMonitorCancellationTokenSource?.Dispose();

        var cancellationTokenSource = new CancellationTokenSource();
        _playbackMonitorCancellationTokenSource = cancellationTokenSource;
        _ = MonitorPlaybackCompletionAsync(cancellationTokenSource.Token);
    }

    private async Task MonitorPlaybackCompletionAsync(CancellationToken ct)
    {
        try
        {
            while (!ct.IsCancellationRequested && _audioPlayer is not null)
            {
                await Task.Delay(250, ct);

                if (_audioPlayer.Duration <= 0)
                {
                    continue;
                }

                if (!_audioPlayer.IsPlaying && _audioPlayer.CurrentPosition >= _audioPlayer.Duration)
                {
                    PlaybackCompleted?.Invoke(this, EventArgs.Empty);
                    break;
                }
            }
        }
        catch (OperationCanceledException)
        {
        }
    }
}
