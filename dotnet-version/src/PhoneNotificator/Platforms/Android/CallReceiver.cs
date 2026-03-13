using Android.App;
using Android.Content;
using Android.Telephony;
using PhoneNotificator.Platforms.Android.Services;

namespace PhoneNotificator.Platforms.Android;

[BroadcastReceiver(Enabled = true, Exported = false)]
[IntentFilterAttribute(new[] { TelephonyManager.ActionPhoneStateChanged })]
public sealed class CallReceiver : BroadcastReceiver
{
    public override void OnReceive(Context? context, Intent? intent)
    {
        AndroidCallMonitorState.PublishState(intent?.GetStringExtra(TelephonyManager.ExtraState));
    }
}
