using global::Android.Content;
using global::Android.Media;
using PhoneNotificator.Core.Services.Interfaces;

namespace PhoneNotificator.Platforms.Android.Services;

public sealed class AndroidAudioPlayerService : Java.Lang.Object, IAudioPlayerService
{
    private MediaPlayer? _mediaPlayer;

    public bool IsPlaying => _mediaPlayer?.IsPlaying ?? false;

    public event EventHandler? PlaybackCompleted;

    public Task PlayAsync(string filePath, CancellationToken ct = default)
    {
        if (!File.Exists(filePath))
        {
            throw new FileNotFoundException("Audio file was not found.", filePath);
        }

        ct.ThrowIfCancellationRequested();
        StopPlayer();

        var player = new MediaPlayer();
        player.SetAudioAttributes(CreateCallPlaybackAttributes());
        player.Completion += OnPlaybackCompleted;
        player.SetDataSource(filePath);
        player.Prepare();
        player.Start();

        _mediaPlayer = player;
        return Task.CompletedTask;
    }

    public Task PauseAsync(CancellationToken ct = default)
    {
        ct.ThrowIfCancellationRequested();

        if (_mediaPlayer?.IsPlaying == true)
        {
            _mediaPlayer.Pause();
        }

        return Task.CompletedTask;
    }

    public Task StopAsync(CancellationToken ct = default)
    {
        ct.ThrowIfCancellationRequested();
        StopPlayer();
        return Task.CompletedTask;
    }

    public double GetProgress()
    {
        if (_mediaPlayer is null || _mediaPlayer.Duration <= 0)
        {
            return 0;
        }

        return Math.Clamp((double)_mediaPlayer.CurrentPosition / _mediaPlayer.Duration, 0, 1);
    }

    public TimeSpan GetCurrentPosition()
    {
        return TimeSpan.FromMilliseconds(_mediaPlayer?.CurrentPosition ?? 0);
    }

    public TimeSpan GetTotalDuration()
    {
        return TimeSpan.FromMilliseconds(_mediaPlayer?.Duration ?? 0);
    }

    private static AudioAttributes CreateCallPlaybackAttributes()
    {
        var builder = new AudioAttributes.Builder();
        builder.SetContentType(AudioContentType.Speech);
        builder.SetUsage(AudioUsageKind.VoiceCommunication);
        builder.SetLegacyStreamType(global::Android.Media.Stream.VoiceCall);

        return builder.Build()
            ?? throw new InvalidOperationException("Android audio attributes could not be created.");
    }

    private void OnPlaybackCompleted(object? sender, EventArgs e)
    {
        PlaybackCompleted?.Invoke(this, EventArgs.Empty);
    }

    private void StopPlayer()
    {
        if (_mediaPlayer is null)
        {
            return;
        }

        _mediaPlayer.Completion -= OnPlaybackCompleted;

        if (_mediaPlayer.IsPlaying)
        {
            _mediaPlayer.Stop();
        }

        _mediaPlayer.Release();
        _mediaPlayer.Dispose();
        _mediaPlayer = null;
    }
}
