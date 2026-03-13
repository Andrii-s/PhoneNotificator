using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Platforms.iOS.Services;

public sealed class IosAudioInjectionService : IAudioInjectionService
{
    public Task PrepareForCallAudioAsync(CancellationToken ct = default)
    {
        return Task.CompletedTask;
    }

    public Task RestoreAfterCallAsync(CancellationToken ct = default)
    {
        return Task.CompletedTask;
    }
}
