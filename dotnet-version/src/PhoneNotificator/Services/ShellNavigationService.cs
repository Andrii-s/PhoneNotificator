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
        if (Shell.Current.Navigation.NavigationStack.Count > 1)
        {
            return Shell.Current.GoToAsync("..");
        }

        return Shell.Current.GoToAsync("//SettingsPage");
    }
}
