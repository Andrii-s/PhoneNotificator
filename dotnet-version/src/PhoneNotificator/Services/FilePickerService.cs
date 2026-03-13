using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Services;

public sealed class FilePickerService : IFilePickerService
{
    private static readonly FilePickerFileType AudioFileTypes = new(new Dictionary<DevicePlatform, IEnumerable<string>>
    {
        [DevicePlatform.WinUI] = [".mp3", ".wav", ".aac"],
        [DevicePlatform.Android] = ["audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/aac", "audio/*"],
        [DevicePlatform.iOS] = ["public.audio"],
        [DevicePlatform.MacCatalyst] = ["public.audio"],
    });

    public async Task<PickedFile?> PickAudioFileAsync(CancellationToken ct = default)
    {
        var result = await FilePicker.Default.PickAsync(new PickOptions
        {
            PickerTitle = "Оберіть аудіофайл",
            FileTypes = AudioFileTypes,
        });

        if (result is null)
        {
            return null;
        }

        return new PickedFile
        {
            FileName = result.FileName,
            ContentType = result.ContentType,
            OpenReadAsync = _ => result.OpenReadAsync(),
        };
    }
}
