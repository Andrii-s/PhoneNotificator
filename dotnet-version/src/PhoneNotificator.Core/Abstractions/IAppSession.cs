using PhoneNotificator.Core.Models;

namespace PhoneNotificator.Core.Abstractions;

public interface IAppSession
{
    AudioFile? SelectedAudioFile { get; set; }

    int CallAudioDelaySeconds { get; set; }
}
