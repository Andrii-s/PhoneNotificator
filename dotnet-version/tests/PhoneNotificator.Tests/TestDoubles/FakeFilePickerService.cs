using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Tests.TestDoubles;

public sealed class FakeFilePickerService : IFilePickerService
{
    public PickedFile? FileToReturn { get; set; }

    public Task<PickedFile?> PickAudioFileAsync(CancellationToken ct = default)
    {
        return Task.FromResult(FileToReturn);
    }
}
