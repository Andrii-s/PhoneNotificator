using CommunityToolkit.Mvvm.ComponentModel;
using PhoneNotificator.Core.Enums;

namespace PhoneNotificator.Core.ViewModels;

public partial class DebtorsViewModel : ObservableObject
{
    [ObservableProperty]
    private string title = "Боржники";

    [ObservableProperty]
    private SendMode selectedMode = SendMode.Debtors;

    [ObservableProperty]
    private string statusMessage = "Етап 3 додасть API, режими обдзвону та журнал виконання.";
}
