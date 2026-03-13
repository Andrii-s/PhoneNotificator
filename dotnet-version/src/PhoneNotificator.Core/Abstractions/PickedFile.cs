namespace PhoneNotificator.Core.Abstractions;

public sealed class PickedFile
{
    public required string FileName { get; init; }

    public string? ContentType { get; init; }

    public required Func<CancellationToken, Task<Stream>> OpenReadAsync { get; init; }
}
