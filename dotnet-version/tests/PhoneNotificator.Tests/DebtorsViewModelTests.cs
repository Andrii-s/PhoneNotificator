using FluentAssertions;
using Moq;
using PhoneNotificator.Core.Abstractions;
using PhoneNotificator.Core.Enums;
using PhoneNotificator.Core.Models;
using PhoneNotificator.Core.Services.Interfaces;
using PhoneNotificator.Core.ViewModels;
using PhoneNotificator.Tests.TestDoubles;

namespace PhoneNotificator.Tests;

public sealed class DebtorsViewModelTests
{
    [Fact]
    public async Task FetchDebtorsCommand_PopulatesCollection()
    {
        var apiService = new Mock<IApiService>();
        apiService.Setup(service => service.GetDebtorsAsync(It.IsAny<CancellationToken>()))
            .ReturnsAsync([
                new Debtor { PhoneNumber = "+380001112233", Name = "Іваненко" },
                new Debtor { PhoneNumber = "+380002223344" },
            ]);

        var viewModel = CreateViewModel(apiService: apiService.Object);

        await viewModel.FetchDebtorsCommand.ExecuteAsync(null);

        viewModel.Debtors.Should().HaveCount(2);
        viewModel.ErrorMessage.Should().BeEmpty();
    }

    [Fact]
    public async Task FetchDebtorsCommand_WhenApiFails_SetsErrorMessage()
    {
        var apiService = new Mock<IApiService>();
        apiService.Setup(service => service.GetDebtorsAsync(It.IsAny<CancellationToken>()))
            .ThrowsAsync(new HttpRequestException("boom"));
        var toast = new RecordingToastService();
        var viewModel = CreateViewModel(apiService: apiService.Object, toastService: toast);

        await viewModel.FetchDebtorsCommand.ExecuteAsync(null);

        viewModel.ErrorMessage.Should().NotBeEmpty();
        toast.Messages.Should().ContainSingle();
    }

    [Fact]
    public async Task StartCallsCommand_EmptyDebtors_ShowsToast()
    {
        var toast = new RecordingToastService();
        var viewModel = CreateViewModel(toastService: toast);

        await viewModel.StartCallsCommand.ExecuteAsync(null);

        toast.Messages.Should().ContainSingle(message => message.Contains("порожній"));
    }

    [Fact]
    public async Task StartSingleCallCommand_UsesEnteredPhoneNumber()
    {
        var callService = new Mock<ICallService>();
        callService
            .Setup(service => service.MakeCallsSequentialAsync(
                It.IsAny<IEnumerable<string>>(),
                It.IsAny<string>(),
                It.IsAny<Func<CallReport, Task>>(),
                It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);
        var session = new AppSession
        {
            SelectedAudioFile = new AudioFile { FileName = "message.mp3", FilePath = "file.mp3" },
        };
        var viewModel = CreateViewModel(callService: callService.Object, appSession: session);
        viewModel.SelectedMode = SendMode.Manual;
        viewModel.ManualPhoneNumber = "+380001112233";

        await viewModel.StartSingleCallCommand.ExecuteAsync(null);

        callService.Verify(
            service => service.MakeCallsSequentialAsync(
                It.Is<IEnumerable<string>>(phones => phones.Single() == "+380001112233"),
                "file.mp3",
                It.IsAny<Func<CallReport, Task>>(),
                It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task StartCallsFromListCommand_FiltersEmptyLines()
    {
        var callService = new Mock<ICallService>();
        callService
            .Setup(service => service.MakeCallsSequentialAsync(
                It.IsAny<IEnumerable<string>>(),
                It.IsAny<string>(),
                It.IsAny<Func<CallReport, Task>>(),
                It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);
        var session = new AppSession
        {
            SelectedAudioFile = new AudioFile { FileName = "message.mp3", FilePath = "file.mp3" },
        };
        var viewModel = CreateViewModel(callService: callService.Object, appSession: session);
        viewModel.SelectedMode = SendMode.List;
        viewModel.ListPhoneNumbers = "+380001112233\n\n  \n+380002223344";

        await viewModel.StartCallsFromListCommand.ExecuteAsync(null);

        callService.Verify(
            service => service.MakeCallsSequentialAsync(
                It.Is<IEnumerable<string>>(phones => phones.Count() == 2),
                "file.mp3",
                It.IsAny<Func<CallReport, Task>>(),
                It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task GoBackCommand_NavigatesBack()
    {
        var navigation = new FakeNavigationService();
        var viewModel = CreateViewModel(navigationService: navigation);

        await viewModel.GoBackCommand.ExecuteAsync(null);

        navigation.WentBack.Should().BeTrue();
    }

    [Fact]
    public async Task StartCallsFromListCommand_AfterCall_SendsReportAndAddsLog()
    {
        var apiService = new Mock<IApiService>();
        var callService = new Mock<ICallService>();
        callService
            .Setup(service => service.MakeCallsSequentialAsync(
                It.IsAny<IEnumerable<string>>(),
                It.IsAny<string>(),
                It.IsAny<Func<CallReport, Task>>(),
                It.IsAny<CancellationToken>()))
            .Returns(async (
                IEnumerable<string> _,
                string _,
                Func<CallReport, Task> onCallCompleted,
                CancellationToken _) =>
            {
                await onCallCompleted(new CallReport
                {
                    PhoneNumber = "+380001112233",
                    StartTime = new DateTime(2026, 3, 13, 10, 0, 0, DateTimeKind.Utc),
                    EndTime = new DateTime(2026, 3, 13, 10, 0, 30, DateTimeKind.Utc),
                });
            });

        var session = new AppSession
        {
            SelectedAudioFile = new AudioFile { FileName = "message.mp3", FilePath = "file.mp3" },
        };

        var viewModel = CreateViewModel(apiService: apiService.Object, callService: callService.Object, appSession: session);
        viewModel.SelectedMode = SendMode.List;
        viewModel.ListPhoneNumbers = "+380001112233";

        await viewModel.StartCallsFromListCommand.ExecuteAsync(null);

        apiService.Verify(service => service.SendCallReportAsync(It.IsAny<CallReport>(), It.IsAny<CancellationToken>()), Times.Once);
        viewModel.CallLog.Should().Contain(entry => entry.Contains("Звіт відправлено"));
    }

    private static DebtorsViewModel CreateViewModel(
        IApiService? apiService = null,
        ICallService? callService = null,
        INavigationService? navigationService = null,
        IToastService? toastService = null,
        IAppSession? appSession = null)
    {
        return new DebtorsViewModel(
            apiService ?? Mock.Of<IApiService>(),
            callService ?? Mock.Of<ICallService>(),
            navigationService ?? new FakeNavigationService(),
            toastService ?? new RecordingToastService(),
            appSession ?? new AppSession());
    }
}
