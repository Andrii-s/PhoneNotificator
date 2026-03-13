namespace PhoneNotificator.Core.Services.Interfaces;

public interface IAudioPlayerService
{
    bool IsPlaying { get; }

    Task PlayAsync(string filePath, CancellationToken ct = default);

    Task PauseAsync(CancellationToken ct = default);

    Task StopAsync(CancellationToken ct = default);

    double GetProgress();

    TimeSpan GetCurrentPosition();

    TimeSpan GetTotalDuration();

    event EventHandler? PlaybackCompleted;
}
