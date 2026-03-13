using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Tests.TestDoubles;

public sealed class FakeNavigationService : INavigationService
{
    public string? LastRoute { get; private set; }

    public bool WentBack { get; private set; }

    public Task GoToAsync(string route)
    {
        LastRoute = route;
        return Task.CompletedTask;
    }

    public Task GoBackAsync()
    {
        WentBack = true;
        return Task.CompletedTask;
    }
}
