using CommunityToolkit.Maui;
using Microsoft.Extensions.Logging;
using PhoneNotificator.Core.Abstractions;
using PhoneNotificator.Core.Services;
using PhoneNotificator.Core.Services.Interfaces;
using PhoneNotificator.Core.ViewModels;
using PhoneNotificator.Services;
using PhoneNotificator.Views;
using Plugin.Maui.Audio;
using Polly;

namespace PhoneNotificator;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();
        builder
            .UseMauiApp<App>()
            .UseMauiCommunityToolkit()
            .AddAudio()
            .ConfigureFonts(fonts =>
            {
                fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
                fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
            });

        builder.Services.AddSingleton<IAppSession, AppSession>();
        builder.Services.AddSingleton<IAppFileSystem, MauiFileSystem>();
        builder.Services.AddSingleton<IPreferencesService, PreferencesService>();
        builder.Services.AddSingleton<IAudioFileService, AudioFileService>();
        builder.Services.AddSingleton<IAudioPlayerService, AudioPlayerService>();
        builder.Services.AddSingleton<IFilePickerService, FilePickerService>();
        builder.Services.AddSingleton<INavigationService, ShellNavigationService>();
        builder.Services.AddSingleton<IToastService, ToastService>();
        builder.Services.AddSingleton<IConfirmationService, PopupConfirmationService>();
        builder.Services.AddSingleton<IAppCloser, AppCloser>();
        builder.Services.AddSingleton<ICallService, PreviewCallService>();
        builder.Services.AddSingleton<AppShell>();

        builder.Services
            .AddHttpClient<IApiService, ApiService>(client =>
            {
                client.BaseAddress = new Uri("https://itproject.com");
                client.Timeout = TimeSpan.FromSeconds(30);
            })
            .AddTransientHttpErrorPolicy(policyBuilder =>
                policyBuilder.WaitAndRetryAsync(3, attempt => TimeSpan.FromSeconds(Math.Pow(2, attempt))));

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
