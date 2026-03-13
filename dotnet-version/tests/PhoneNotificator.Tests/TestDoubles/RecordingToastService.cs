using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Tests.TestDoubles;

public sealed class RecordingToastService : IToastService
{
    public List<string> Messages { get; } = [];

    public Task ShowAsync(string message, CancellationToken ct = default)
    {
        Messages.Add(message);
        return Task.CompletedTask;
    }
}
