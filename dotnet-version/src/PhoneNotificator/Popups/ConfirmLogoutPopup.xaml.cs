using CommunityToolkit.Maui.Views;

namespace PhoneNotificator.Popups;

public partial class ConfirmLogoutPopup : Popup
{
    private readonly TaskCompletionSource<bool> _resultSource = new();

    public ConfirmLogoutPopup()
    {
        InitializeComponent();
        Closed += (_, _) => TrySetResult(false);
    }

    public Task<bool> WaitForResultAsync(CancellationToken ct = default)
    {
        return _resultSource.Task.WaitAsync(ct);
    }

    private void OnCancelClicked(object? sender, EventArgs e)
    {
        TrySetResult(false);
        Close();
    }

    private void OnConfirmClicked(object? sender, EventArgs e)
    {
        TrySetResult(true);
        Close();
    }

    private void TrySetResult(bool result)
    {
        if (!_resultSource.Task.IsCompleted)
        {
            _resultSource.SetResult(result);
        }
    }
}
