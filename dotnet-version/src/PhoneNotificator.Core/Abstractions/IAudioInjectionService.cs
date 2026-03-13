namespace PhoneNotificator.Core.Abstractions;

public interface IAudioInjectionService
{
    Task PrepareForCallAudioAsync(CancellationToken ct = default);

    Task RestoreAfterCallAsync(CancellationToken ct = default);
}
