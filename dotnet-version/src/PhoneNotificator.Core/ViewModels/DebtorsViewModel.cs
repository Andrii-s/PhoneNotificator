using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using PhoneNotificator.Core.Abstractions;
using PhoneNotificator.Core.Enums;
using PhoneNotificator.Core.Models;
using PhoneNotificator.Core.Services.Interfaces;

namespace PhoneNotificator.Core.ViewModels;

public partial class DebtorsViewModel : ObservableObject
{
    private readonly IApiService _apiService;
    private readonly ICallService _callService;
    private readonly INavigationService _navigationService;
    private readonly IToastService _toastService;
    private readonly IAppSession _appSession;

    public DebtorsViewModel(
        IApiService apiService,
        ICallService callService,
        INavigationService navigationService,
        IToastService toastService,
        IAppSession appSession)
    {
        _apiService = apiService;
        _callService = callService;
        _navigationService = navigationService;
        _toastService = toastService;
        _appSession = appSession;

        Title = "Боржники";
        SendModes = ["Боржники", "Ввести номер вручну", "Введення номерів списком"];
        Debtors = new ObservableCollection<Debtor>();
        CallLog = new ObservableCollection<string>();
        SelectedAudioFileName = _appSession.SelectedAudioFile?.FileName ?? "Немає вибраного файлу";
    }

    public ObservableCollection<Debtor> Debtors { get; }

    public ObservableCollection<string> CallLog { get; }

    public IReadOnlyList<string> SendModes { get; }

    public bool IsDebtorsMode => SelectedMode == SendMode.Debtors;

    public bool IsManualMode => SelectedMode == SendMode.Manual;

    public bool IsListMode => SelectedMode == SendMode.List;

    [ObservableProperty]
    private string title = string.Empty;

    [ObservableProperty]
    private SendMode selectedMode = SendMode.Debtors;

    [ObservableProperty]
    private int selectedModeIndex;

    [ObservableProperty]
    private string manualPhoneNumber = string.Empty;

    [ObservableProperty]
    private string listPhoneNumbers = string.Empty;

    [ObservableProperty]
    private bool isCallsRunning;

    [ObservableProperty]
    private bool isBusy;

    [ObservableProperty]
    private string errorMessage = string.Empty;

    [ObservableProperty]
    private string selectedAudioFileName = string.Empty;

    partial void OnSelectedModeIndexChanged(int value)
    {
        SelectedMode = value switch
        {
            1 => SendMode.Manual,
            2 => SendMode.List,
            _ => SendMode.Debtors,
        };
    }

    partial void OnSelectedModeChanged(SendMode value)
    {
        SelectedModeIndex = (int)value;
        OnPropertyChanged(nameof(IsDebtorsMode));
        OnPropertyChanged(nameof(IsManualMode));
        OnPropertyChanged(nameof(IsListMode));
    }

    [RelayCommand]
    private async Task FetchDebtorsAsync()
    {
        if (IsBusy)
        {
            return;
        }

        try
        {
            IsBusy = true;
            ErrorMessage = string.Empty;

            var debtors = await _apiService.GetDebtorsAsync();
            Debtors.Clear();
            foreach (var debtor in debtors)
            {
                Debtors.Add(debtor);
            }

            CallLog.Add($"[{DateTime.Now:HH:mm}] Отримано {Debtors.Count} номер(ів) із сервера.");
        }
        catch (Exception)
        {
            ErrorMessage = "Не вдалося отримати список боржників.";
            CallLog.Add($"[{DateTime.Now:HH:mm}] Помилка отримання списку боржників.");
            await _toastService.ShowAsync(ErrorMessage);
        }
        finally
        {
            IsBusy = false;
        }
    }

    [RelayCommand]
    private async Task StartCallsAsync()
    {
        if (Debtors.Count == 0)
        {
            await _toastService.ShowAsync("Список боржників порожній.");
            return;
        }

        await StartCallSequenceAsync(Debtors.Select(debtor => debtor.PhoneNumber));
    }

    [RelayCommand]
    private async Task StartSingleCallAsync()
    {
        var phoneNumber = ManualPhoneNumber.Trim();
        if (string.IsNullOrWhiteSpace(phoneNumber))
        {
            await _toastService.ShowAsync("Введіть номер телефону.");
            return;
        }

        await StartCallSequenceAsync([phoneNumber]);
    }

    [RelayCommand]
    private async Task StartCallsFromListAsync()
    {
        var phoneNumbers = ListPhoneNumbers
            .Split(["\r\n", "\n"], StringSplitOptions.None)
            .Select(phoneNumber => phoneNumber.Trim())
            .Where(phoneNumber => !string.IsNullOrWhiteSpace(phoneNumber))
            .Distinct(StringComparer.Ordinal)
            .ToList();

        if (phoneNumbers.Count == 0)
        {
            await _toastService.ShowAsync("Додайте хоча б один номер у список.");
            return;
        }

        await StartCallSequenceAsync(phoneNumbers);
    }

    [RelayCommand]
    private Task GoBackAsync()
    {
        return _navigationService.GoBackAsync();
    }

    private async Task StartCallSequenceAsync(IEnumerable<string> phoneNumbers)
    {
        var selectedAudioFile = _appSession.SelectedAudioFile;
        if (selectedAudioFile is null)
        {
            await _toastService.ShowAsync("Поверніться до налаштувань і оберіть аудіофайл.");
            return;
        }

        var phoneNumberList = phoneNumbers
            .Select(phoneNumber => phoneNumber.Trim())
            .Where(phoneNumber => !string.IsNullOrWhiteSpace(phoneNumber))
            .ToList();

        if (phoneNumberList.Count == 0)
        {
            await _toastService.ShowAsync("Немає номерів для обзвону.");
            return;
        }

        try
        {
            IsBusy = true;
            IsCallsRunning = true;
            ErrorMessage = string.Empty;
            SelectedAudioFileName = selectedAudioFile.FileName;
            CallLog.Add($"[{DateTime.Now:HH:mm}] Старт обзвону: {phoneNumberList.Count} номер(ів).");

            await _callService.MakeCallsSequentialAsync(
                phoneNumberList,
                selectedAudioFile.FilePath,
                HandleCallCompletedAsync);
        }
        catch (Exception)
        {
            ErrorMessage = "Не вдалося виконати дзвінки.";
            CallLog.Add($"[{DateTime.Now:HH:mm}] Зупинка через помилку.");
            await _toastService.ShowAsync(ErrorMessage);
        }
        finally
        {
            IsBusy = false;
            IsCallsRunning = false;
        }
    }

    private async Task HandleCallCompletedAsync(CallReport report)
    {
        CallLog.Add($"[{report.EndTime:HH:mm}] {report.PhoneNumber} - {report.DurationFormatted}");

        try
        {
            await _apiService.SendCallReportAsync(report);
            CallLog.Add($"[{DateTime.Now:HH:mm}] Звіт відправлено для {report.PhoneNumber}.");
        }
        catch (Exception)
        {
            CallLog.Add($"[{DateTime.Now:HH:mm}] Не вдалося відправити звіт для {report.PhoneNumber}.");
        }
    }
}
