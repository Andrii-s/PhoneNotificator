using global::Android.Content;
using global::Android.Media;
using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Platforms.Android.Services;

public sealed class AndroidAudioInjector : IAudioInjectionService
{
    private readonly AudioManager? _audioManager;
    private Mode _previousMode;
    private bool _previousSpeakerphoneState;
    private bool _previousMicrophoneMuteState;
    private bool _communicationDeviceApplied;

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
        _previousMicrophoneMuteState = _audioManager.MicrophoneMute;

        _audioManager.Mode = Mode.InCommunication;
        _audioManager.MicrophoneMute = false;
        ApplySpeakerRouting();
        return Task.CompletedTask;
    }

    public Task RestoreAfterCallAsync(CancellationToken ct = default)
    {
        if (_audioManager is null)
        {
            return Task.CompletedTask;
        }

        _audioManager.Mode = _previousMode;
        RestoreSpeakerRouting();
        _audioManager.MicrophoneMute = _previousMicrophoneMuteState;
        return Task.CompletedTask;
    }

    private void ApplySpeakerRouting()
    {
        if (_audioManager is null)
        {
            return;
        }

        if (OperatingSystem.IsAndroidVersionAtLeast(31))
        {
            foreach (var device in _audioManager.AvailableCommunicationDevices)
            {
                if (device.Type != AudioDeviceType.BuiltinSpeaker)
                {
                    continue;
                }

                _communicationDeviceApplied = _audioManager.SetCommunicationDevice(device);
                if (_communicationDeviceApplied)
                {
                    return;
                }
            }
        }

#pragma warning disable CA1422
        _previousSpeakerphoneState = _audioManager.SpeakerphoneOn;
        _audioManager.SpeakerphoneOn = true;
#pragma warning restore CA1422
    }

    private void RestoreSpeakerRouting()
    {
        if (_audioManager is null)
        {
            return;
        }

        if (_communicationDeviceApplied && OperatingSystem.IsAndroidVersionAtLeast(31))
        {
            _audioManager.ClearCommunicationDevice();
            _communicationDeviceApplied = false;
            return;
        }

#pragma warning disable CA1422
        _audioManager.SpeakerphoneOn = _previousSpeakerphoneState;
#pragma warning restore CA1422
    }
}
