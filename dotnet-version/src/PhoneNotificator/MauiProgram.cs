using CommunityToolkit.Maui;
using Microsoft.Extensions.Logging;
using PhoneNotificator.Core.Abstractions;
using PhoneNotificator.Core.ViewModels;
using PhoneNotificator.Views;

namespace PhoneNotificator;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();
        builder
            .UseMauiApp<App>()
            .UseMauiCommunityToolkit()
            .ConfigureFonts(fonts =>
            {
                fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
                fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
            });

        builder.Services.AddSingleton<IAppSession, AppSession>();
        builder.Services.AddSingleton<AppShell>();
        builder.Services.AddTransient<SettingsViewModel>();
        builder.Services.AddTransient<DebtorsViewModel>();
        builder.Services.AddTransient<SplashPage>();
        builder.Services.AddTransient<SettingsPage>();
        builder.Services.AddTransient<DebtorsPage>();

#if DEBUG
        builder.Logging.AddDebug();
#endif

        return builder.Build();
    }
}
