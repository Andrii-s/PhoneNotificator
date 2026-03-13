using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using PhoneNotificator.Core.Abstractions;
using PhoneNotificator.Core.Models;
using PhoneNotificator.Core.Services.Interfaces;

namespace PhoneNotificator.Core.ViewModels;

public partial class SettingsViewModel : ObservableObject
{
    private readonly IAudioFileService _audioFileService;
    private readonly IAudioPlayerService _audioPlayerService;
    private readonly IFilePickerService _filePickerService;
    private readonly INavigationService _navigationService;
    private readonly IToastService _toastService;
    private readonly IConfirmationService _confirmationService;
    private readonly IAppCloser _appCloser;
    private readonly IAppSession _appSession;

    private CancellationTokenSource? _progressMonitorCancellationTokenSource;

    public SettingsViewModel(
        IAudioFileService audioFileService,
        IAudioPlayerService audioPlayerService,
        IFilePickerService filePickerService,
        INavigationService navigationService,
        IToastService toastService,
        IConfirmationService confirmationService,
        IAppCloser appCloser,
        IAppSession appSession)
    {
        _audioFileService = audioFileService;
        _audioPlayerService = audioPlayerService;
        _filePickerService = filePickerService;
        _navigationService = navigationService;
        _toastService = toastService;
        _confirmationService = confirmationService;
        _appCloser = appCloser;
        _appSession = appSession;

        Title = "Налаштування";
        PlaybackTimeLabel = "0:00 / 0:00";
        AudioFiles = new ObservableCollection<AudioFile>(_audioFileService.GetAllFiles());
        SelectedFile = AudioFiles.FirstOrDefault();

        _audioPlayerService.PlaybackCompleted += OnPlaybackCompleted;
    }

    public ObservableCollection<AudioFile> AudioFiles { get; }

    [ObservableProperty]
    private string title = string.Empty;

    [ObservableProperty]
    private AudioFile? selectedFile;

    [ObservableProperty]
    private AudioFile? playingFile;

    [ObservableProperty]
    private double playbackProgress;

    [ObservableProperty]
    private string playbackTimeLabel = string.Empty;

    [ObservableProperty]
    private bool isBusy;

    [RelayCommand]
    private async Task UploadFileAsync()
    {
        if (IsBusy)
        {
            return;
        }

        try
        {
            IsBusy = true;

            var pickedFile = await _filePickerService.PickAudioFileAsync();
            if (pickedFile is null)
            {
                return;
            }

            var importedFile = await _audioFileService.ImportFileAsync(pickedFile);
            AudioFiles.Add(importedFile);
            SelectedFile = importedFile;

            await _toastService.ShowAsync($"Додано: {importedFile.FileName}");
        }
        catch (Exception)
        {
            await _toastService.ShowAsync("Не вдалося імпортувати аудіофайл.");
        }
        finally
        {
            IsBusy = false;
        }
    }

    [RelayCommand]
    private async Task TogglePlayAsync(AudioFile? file)
    {
        if (file is null)
        {
            return;
        }

        if (PlayingFile?.Id == file.Id && _audioPlayerService.IsPlaying)
        {
            await _audioPlayerService.PauseAsync();
            StopProgressMonitor();
            PlayingFile = null;
            RefreshPlaybackState();
            return;
        }

        try
        {
            await _audioPlayerService.StopAsync();
            await _audioPlayerService.PlayAsync(file.FilePath);

            SelectedFile = file;
            PlayingFile = file;
            RefreshPlaybackState();
            StartProgressMonitor();
        }
        catch (Exception)
        {
            await _toastService.ShowAsync("Не вдалося відтворити файл.");
        }
    }

    [RelayCommand]
    private async Task NavigateToDebtorsAsync()
    {
        if (SelectedFile is null)
        {
            await _toastService.ShowAsync("Спочатку оберіть аудіофайл.");
            return;
        }

        _appSession.SelectedAudioFile = SelectedFile;
        await _navigationService.GoToAsync("//DebtorsPage");
    }

    [RelayCommand]
    private async Task ShowExitConfirmationAsync()
    {
        if (await _confirmationService.ConfirmExitAsync())
        {
            _appCloser.Quit();
        }
    }

    private void StartProgressMonitor()
    {
        StopProgressMonitor();
        var cancellationTokenSource = new CancellationTokenSource();
        _progressMonitorCancellationTokenSource = cancellationTokenSource;
        _ = MonitorPlaybackAsync(cancellationTokenSource.Token);
    }

    private async Task MonitorPlaybackAsync(CancellationToken ct)
    {
        try
        {
            using var timer = new PeriodicTimer(TimeSpan.FromMilliseconds(250));
            while (!ct.IsCancellationRequested && await timer.WaitForNextTickAsync(ct))
            {
                RefreshPlaybackState();
                if (!_audioPlayerService.IsPlaying)
                {
                    break;
                }
            }
        }
        catch (OperationCanceledException)
        {
        }
    }

    private void StopProgressMonitor()
    {
        _progressMonitorCancellationTokenSource?.Cancel();
        _progressMonitorCancellationTokenSource?.Dispose();
        _progressMonitorCancellationTokenSource = null;
    }

    private void RefreshPlaybackState()
    {
        PlaybackProgress = _audioPlayerService.GetProgress();
        var currentPosition = _audioPlayerService.GetCurrentPosition();
        var totalDuration = _audioPlayerService.GetTotalDuration();
        PlaybackTimeLabel = $"{FormatTime(currentPosition)} / {FormatTime(totalDuration)}";
    }

    private void OnPlaybackCompleted(object? sender, EventArgs e)
    {
        StopProgressMonitor();
        PlayingFile = null;
        RefreshPlaybackState();
    }

    private static string FormatTime(TimeSpan timeSpan)
    {
        return timeSpan.TotalHours >= 1
            ? timeSpan.ToString(@"h\:mm\:ss")
            : timeSpan.ToString(@"m\:ss");
    }
}
