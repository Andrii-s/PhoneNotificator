using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Tests.TestDoubles;

public sealed class FakeConfirmationService : IConfirmationService
{
    public bool Result { get; set; }

    public Task<bool> ConfirmExitAsync(CancellationToken ct = default)
    {
        return Task.FromResult(Result);
    }
}
