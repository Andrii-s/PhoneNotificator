using PhoneNotificator.Core.Models;

namespace PhoneNotificator.Core.Services.Interfaces;

public interface ICallService
{
    Task MakeCallAsync(
        string phoneNumber,
        string audioFilePath,
        Func<CallReport, Task> onCallCompleted,
        CancellationToken ct = default);

    Task MakeCallsSequentialAsync(
        IEnumerable<string> phoneNumbers,
        string audioFilePath,
        Func<CallReport, Task> onEachCallCompleted,
        CancellationToken ct = default);
}
