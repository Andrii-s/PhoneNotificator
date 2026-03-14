using global::Android.Content;
using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Platforms.Android.Services;

public sealed class AndroidPhoneDialerService : IPhoneDialerService
{
    public Task DialAsync(string phoneNumber, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(phoneNumber))
        {
            throw new ArgumentException("Phone number is required.", nameof(phoneNumber));
        }

        var encodedPhoneNumber = global::Android.Net.Uri.Encode(phoneNumber.Trim());
        var dialUri = global::Android.Net.Uri.Parse($"tel:{encodedPhoneNumber}");
        var intent = new Intent(Intent.ActionCall, dialUri);
        intent.AddFlags(ActivityFlags.NewTask);

        var packageManager = Platform.AppContext.PackageManager
            ?? throw new NotSupportedException("Android package manager is not available.");

        if (intent.ResolveActivity(packageManager) is null)
        {
            throw new NotSupportedException("No Android call handler is available on this device.");
        }

        Platform.AppContext.StartActivity(intent);
        return Task.CompletedTask;
    }
}
