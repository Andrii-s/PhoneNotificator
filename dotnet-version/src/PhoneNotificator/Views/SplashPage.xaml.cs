namespace PhoneNotificator.Views;

public partial class SplashPage : ContentPage
{
    private bool _hasNavigated;

    public SplashPage()
    {
        InitializeComponent();
    }

    protected override async void OnAppearing()
    {
        base.OnAppearing();

        if (_hasNavigated)
        {
            return;
        }

        _hasNavigated = true;

        await Task.Delay(TimeSpan.FromMilliseconds(1500));
        await Shell.Current.GoToAsync("//SettingsPage");
    }
}
