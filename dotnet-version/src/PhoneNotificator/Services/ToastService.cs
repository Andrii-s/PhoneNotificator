using CommunityToolkit.Maui.Alerts;
using CommunityToolkit.Maui.Core;
using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Services;

public sealed class ToastService : IToastService
{
    public Task ShowAsync(string message, CancellationToken ct = default)
    {
        return Toast.Make(message, ToastDuration.Short).Show(ct);
    }
}
