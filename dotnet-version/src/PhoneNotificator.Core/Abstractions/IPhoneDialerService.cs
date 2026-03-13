namespace PhoneNotificator.Core.Abstractions;

public interface IPhoneDialerService
{
    Task DialAsync(string phoneNumber, CancellationToken ct = default);
}
