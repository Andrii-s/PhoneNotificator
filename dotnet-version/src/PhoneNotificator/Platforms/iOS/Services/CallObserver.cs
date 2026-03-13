using CallKit;
using CoreFoundation;
using Foundation;

namespace PhoneNotificator.Platforms.iOS.Services;

public sealed class CallObserver : NSObject, ICXCallObserverDelegate
{
    private readonly CXCallObserver _callObserver = new();

    public CallObserver()
    {
        _callObserver.SetDelegate(this, DispatchQueue.MainQueue);
    }

    public void CallChanged(CXCallObserver callObserver, CXCall call)
    {
        if (call.HasConnected)
        {
            IosCallMonitorState.MarkConnected();
        }

        if (call.HasEnded)
        {
            IosCallMonitorState.MarkEnded();
        }
    }
}
