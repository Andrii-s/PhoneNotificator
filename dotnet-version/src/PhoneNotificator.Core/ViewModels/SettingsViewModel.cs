using CommunityToolkit.Mvvm.ComponentModel;

namespace PhoneNotificator.Core.ViewModels;

public partial class SettingsViewModel : ObservableObject
{
    [ObservableProperty]
    private string title = "Налаштування";

    [ObservableProperty]
    private string statusMessage = "Етап 2 додасть імпорт файлів, аудіоплеєр і керування вибраним повідомленням.";
}
