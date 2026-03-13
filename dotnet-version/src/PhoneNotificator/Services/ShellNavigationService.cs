using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Services;

public sealed class ShellNavigationService : INavigationService
{
    public Task GoToAsync(string route)
    {
        return Shell.Current.GoToAsync(route);
    }

    public Task GoBackAsync()
    {
        return Shell.Current.GoToAsync("..");
    }
}
