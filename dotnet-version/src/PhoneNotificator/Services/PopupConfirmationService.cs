using PhoneNotificator.Core.Abstractions;
using PhoneNotificator.Popups;
using CommunityToolkit.Maui.Views;

namespace PhoneNotificator.Services;

public sealed class PopupConfirmationService : IConfirmationService
{
    public async Task<bool> ConfirmExitAsync(CancellationToken ct = default)
    {
        var page = Shell.Current?.CurrentPage ?? Application.Current?.Windows.FirstOrDefault()?.Page;
        if (page is null)
        {
            return false;
        }

        var popup = new ConfirmLogoutPopup();
        page.ShowPopup(popup);
        return await popup.WaitForResultAsync(ct);
    }
}
