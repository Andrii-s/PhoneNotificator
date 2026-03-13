using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Tests.TestDoubles;

public sealed class AppCloserSpy : IAppCloser
{
    public bool WasQuitCalled { get; private set; }

    public void Quit()
    {
        WasQuitCalled = true;
    }
}
