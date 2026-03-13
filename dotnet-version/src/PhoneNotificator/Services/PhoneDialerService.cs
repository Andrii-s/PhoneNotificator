using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Services;

public sealed class PhoneDialerService : IPhoneDialerService
{
    public Task DialAsync(string phoneNumber, CancellationToken ct = default)
    {
        if (!PhoneDialer.Default.IsSupported)
        {
            throw new NotSupportedException("Phone dialer is not supported on this platform.");
        }

        PhoneDialer.Default.Open(phoneNumber);
        return Task.CompletedTask;
    }
}
