using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Tests.TestDoubles;

public sealed class PhoneDialerSpy : IPhoneDialerService
{
    public List<string> DialedPhoneNumbers { get; } = [];

    public Task DialAsync(string phoneNumber, CancellationToken ct = default)
    {
        DialedPhoneNumbers.Add(phoneNumber);
        return Task.CompletedTask;
    }
}
