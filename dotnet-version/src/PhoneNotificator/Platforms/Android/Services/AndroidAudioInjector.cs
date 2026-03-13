using Android.Content;
using Android.Media;
using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Platforms.Android.Services;

public sealed class AndroidAudioInjector : IAudioInjectionService
{
    private readonly AudioManager? _audioManager;
    private Mode _previousMode;
    private bool _previousSpeakerphoneState;

    public AndroidAudioInjector()
    {
        _audioManager = Platform.AppContext.GetSystemService(Context.AudioService) as AudioManager;
    }

    public Task PrepareForCallAudioAsync(CancellationToken ct = default)
    {
        if (_audioManager is null)
        {
            return Task.CompletedTask;
        }

        _previousMode = _audioManager.Mode;
        _previousSpeakerphoneState = _audioManager.SpeakerphoneOn;

        _audioManager.Mode = Mode.InCommunication;
        _audioManager.SpeakerphoneOn = false;
        return Task.CompletedTask;
    }

    public Task RestoreAfterCallAsync(CancellationToken ct = default)
    {
        if (_audioManager is null)
        {
            return Task.CompletedTask;
        }

        _audioManager.Mode = _previousMode;
        _audioManager.SpeakerphoneOn = _previousSpeakerphoneState;
        return Task.CompletedTask;
    }
}
