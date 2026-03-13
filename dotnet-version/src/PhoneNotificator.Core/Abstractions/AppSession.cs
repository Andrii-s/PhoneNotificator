using PhoneNotificator.Core.Configuration;
using PhoneNotificator.Core.Models;

namespace PhoneNotificator.Core.Abstractions;

public sealed class AppSession : IAppSession
{
    public AudioFile? SelectedAudioFile { get; set; }

    public int CallAudioDelaySeconds { get; set; } = AppDefaults.CallAudioDelaySeconds;
}
