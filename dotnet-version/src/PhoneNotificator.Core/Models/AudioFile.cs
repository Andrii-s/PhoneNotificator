namespace PhoneNotificator.Core.Models;

public sealed class AudioFile
{
    public string Id { get; set; } = Guid.NewGuid().ToString();

    public string FileName { get; set; } = string.Empty;

    public string FilePath { get; set; } = string.Empty;
}
