namespace PhoneNotificator.Core.Abstractions;

public interface IFilePickerService
{
    Task<PickedFile?> PickAudioFileAsync(CancellationToken ct = default);
}
