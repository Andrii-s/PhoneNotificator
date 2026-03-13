using PhoneNotificator.Core.ViewModels;

namespace PhoneNotificator.Views;

public partial class DebtorsPage : ContentPage
{
    public DebtorsPage(DebtorsViewModel viewModel)
    {
        InitializeComponent();
        BindingContext = viewModel;
    }
}
