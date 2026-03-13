using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Services;

public sealed class AppCloser : IAppCloser
{
    public void Quit()
    {
        Application.Current?.Quit();
    }
}
