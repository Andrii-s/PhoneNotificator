using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Services;

public sealed class NoOpAudioInjectionService : IAudioInjectionService
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
