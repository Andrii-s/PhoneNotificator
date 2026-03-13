using PhoneNotificator.Core.Abstractions;
using PhoneNotificator.Core.Models;

namespace PhoneNotificator.Core.Services.Interfaces;

public interface IAudioFileService
{
    Task<AudioFile> ImportFileAsync(PickedFile file, CancellationToken ct = default);

    IReadOnlyList<AudioFile> GetAllFiles();

    void DeleteFile(AudioFile file);
}
