using PhoneNotificator.Core.Models;
using PhoneNotificator.Core.Services.Interfaces;

namespace PhoneNotificator.Core.Services;

public sealed class PreviewCallService : ICallService
{
    public async Task MakeCallAsync(
        string phoneNumber,
        string audioFilePath,
        Func<CallReport, Task> onCallCompleted,
        CancellationToken ct = default)
    {
        if (!File.Exists(audioFilePath))
        {
            throw new FileNotFoundException("Audio file was not found.", audioFilePath);
        }

        var startTime = DateTime.UtcNow;
        await Task.Delay(TimeSpan.FromMilliseconds(800), ct);
        var endTime = startTime.AddSeconds(3);

        await onCallCompleted(new CallReport
        {
            PhoneNumber = phoneNumber,
            StartTime = startTime,
            EndTime = endTime,
        });
    }

    public async Task MakeCallsSequentialAsync(
        IEnumerable<string> phoneNumbers,
        string audioFilePath,
        Func<CallReport, Task> onEachCallCompleted,
        CancellationToken ct = default)
    {
        foreach (var phoneNumber in phoneNumbers)
        {
            ct.ThrowIfCancellationRequested();
            await MakeCallAsync(phoneNumber, audioFilePath, onEachCallCompleted, ct);
        }
    }
}
