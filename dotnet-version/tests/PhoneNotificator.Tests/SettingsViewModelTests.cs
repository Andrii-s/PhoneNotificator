using FluentAssertions;
using Moq;
using PhoneNotificator.Core.Abstractions;
using PhoneNotificator.Core.Configuration;
using PhoneNotificator.Core.Models;
using PhoneNotificator.Core.Services.Interfaces;
using PhoneNotificator.Core.ViewModels;
using PhoneNotificator.Tests.TestDoubles;

namespace PhoneNotificator.Tests;

public sealed class SettingsViewModelTests
{
    [Fact]
    public async Task UploadFileCommand_ValidFile_AddsFileToCollection()
    {
        var audioFileService = new Mock<IAudioFileService>();
        audioFileService.Setup(service => service.GetAllFiles()).Returns([]);
        audioFileService
            .Setup(service => service.ImportFileAsync(It.IsAny<PickedFile>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(new AudioFile { FileName = "message.mp3", FilePath = "C:\\audio\\message.mp3" });

        var filePicker = new FakeFilePickerService
        {
            FileToReturn = new PickedFile
            {
                FileName = "message.mp3",
                OpenReadAsync = _ => Task.FromResult<Stream>(new MemoryStream([1, 2, 3])),
            },
        };

        var viewModel = CreateViewModel(audioFileService.Object, filePicker: filePicker);

        await viewModel.UploadFileCommand.ExecuteAsync(null);

        viewModel.AudioFiles.Should().ContainSingle(file => file.FileName == "message.mp3");
    }

    [Fact]
    public async Task UploadFileCommand_NoFileSelected_DoesNothing()
    {
        var audioFileService = new Mock<IAudioFileService>();
        audioFileService.Setup(service => service.GetAllFiles()).Returns([]);
        var viewModel = CreateViewModel(audioFileService.Object, filePicker: new FakeFilePickerService());

        await viewModel.UploadFileCommand.ExecuteAsync(null);

        viewModel.AudioFiles.Should().BeEmpty();
    }

    [Fact]
    public async Task TogglePlayCommand_WhenSameFileIsPlaying_PausesPlayback()
    {
        var file = new AudioFile { Id = "1", FileName = "message.mp3", FilePath = "file.mp3" };
        var audioFileService = new Mock<IAudioFileService>();
        audioFileService.Setup(service => service.GetAllFiles()).Returns([file]);

        var audioPlayer = new Mock<IAudioPlayerService>();
        audioPlayer.Setup(player => player.IsPlaying).Returns(true);

        var viewModel = CreateViewModel(audioFileService.Object, audioPlayer: audioPlayer.Object);
        viewModel.PlayingFile = file;

        await viewModel.TogglePlayCommand.ExecuteAsync(file);

        audioPlayer.Verify(player => player.PauseAsync(It.IsAny<CancellationToken>()), Times.Once);
        viewModel.PlayingFile.Should().BeNull();
    }

    [Fact]
    public async Task TogglePlayCommand_WhenFileIsNotPlaying_StartsPlaybackAndSelectsFile()
    {
        var file = new AudioFile { Id = "1", FileName = "message.mp3", FilePath = "file.mp3" };
        var audioFileService = new Mock<IAudioFileService>();
        audioFileService.Setup(service => service.GetAllFiles()).Returns([file]);

        var audioPlayer = new Mock<IAudioPlayerService>();
        audioPlayer.Setup(player => player.GetCurrentPosition()).Returns(TimeSpan.FromSeconds(15));
        audioPlayer.Setup(player => player.GetTotalDuration()).Returns(TimeSpan.FromSeconds(90));
        audioPlayer.Setup(player => player.GetProgress()).Returns(0.166);

        var viewModel = CreateViewModel(audioFileService.Object, audioPlayer: audioPlayer.Object);

        await viewModel.TogglePlayCommand.ExecuteAsync(file);

        audioPlayer.Verify(player => player.PlayAsync("file.mp3", It.IsAny<CancellationToken>()), Times.Once);
        viewModel.SelectedFile.Should().BeSameAs(file);
        viewModel.PlayingFile.Should().BeSameAs(file);
        viewModel.PlaybackTimeLabel.Should().Be("0:15 / 1:30");
    }

    [Fact]
    public async Task NavigateToDebtorsCommand_WithoutSelectedFile_ShowsToast()
    {
        var audioFileService = new Mock<IAudioFileService>();
        audioFileService.Setup(service => service.GetAllFiles()).Returns([]);
        var toast = new RecordingToastService();
        var navigation = new FakeNavigationService();
        var session = new AppSession();
        var viewModel = CreateViewModel(audioFileService.Object, toastService: toast, navigationService: navigation, appSession: session);

        await viewModel.NavigateToDebtorsCommand.ExecuteAsync(null);

        toast.Messages.Should().ContainSingle();
        navigation.LastRoute.Should().BeNull();
        session.SelectedAudioFile.Should().BeNull();
    }

    [Fact]
    public async Task NavigateToDebtorsCommand_WithSelectedFile_NavigatesAndStoresSelection()
    {
        var file = new AudioFile { Id = "1", FileName = "message.mp3", FilePath = "file.mp3" };
        var audioFileService = new Mock<IAudioFileService>();
        audioFileService.Setup(service => service.GetAllFiles()).Returns([file]);
        var navigation = new FakeNavigationService();
        var session = new AppSession();
        var viewModel = CreateViewModel(audioFileService.Object, navigationService: navigation, appSession: session);
        viewModel.SelectedFile = file;

        await viewModel.NavigateToDebtorsCommand.ExecuteAsync(null);

        navigation.LastRoute.Should().Be("//DebtorsPage");
        session.SelectedAudioFile.Should().BeSameAs(file);
    }

    [Fact]
    public void Constructor_LoadsSavedDelayIntoSessionAndInput()
    {
        var audioFileService = new Mock<IAudioFileService>();
        audioFileService.Setup(service => service.GetAllFiles()).Returns([]);
        var preferences = new FakePreferencesService();
        preferences.SetInt(AppPreferenceKeys.CallAudioDelaySeconds, 21);
        var session = new AppSession();

        var viewModel = CreateViewModel(audioFileService.Object, appSession: session, preferencesService: preferences);

        viewModel.AudioStartDelayInput.Should().Be("21");
        session.CallAudioDelaySeconds.Should().Be(21);
    }

    [Fact]
    public void AudioStartDelayInput_WhenValid_PersistsValue()
    {
        var audioFileService = new Mock<IAudioFileService>();
        audioFileService.Setup(service => service.GetAllFiles()).Returns([]);
        var preferences = new FakePreferencesService();
        var session = new AppSession();
        var viewModel = CreateViewModel(audioFileService.Object, appSession: session, preferencesService: preferences);

        viewModel.AudioStartDelayInput = "9";

        preferences.GetInt(AppPreferenceKeys.CallAudioDelaySeconds).Should().Be(9);
        session.CallAudioDelaySeconds.Should().Be(9);
        viewModel.HasAudioStartDelayError.Should().BeFalse();
    }

    [Fact]
    public async Task NavigateToDebtorsCommand_WithInvalidDelay_ShowsToastAndDoesNotNavigate()
    {
        var file = new AudioFile { Id = "1", FileName = "message.mp3", FilePath = "file.mp3" };
        var audioFileService = new Mock<IAudioFileService>();
        audioFileService.Setup(service => service.GetAllFiles()).Returns([file]);
        var toast = new RecordingToastService();
        var navigation = new FakeNavigationService();
        var viewModel = CreateViewModel(audioFileService.Object, toastService: toast, navigationService: navigation);
        viewModel.SelectedFile = file;
        viewModel.AudioStartDelayInput = "abc";

        await viewModel.NavigateToDebtorsCommand.ExecuteAsync(null);

        toast.Messages.Should().ContainSingle(message => message.Contains("затримки"));
        navigation.LastRoute.Should().BeNull();
        viewModel.HasAudioStartDelayError.Should().BeTrue();
    }

    [Fact]
    public async Task ShowExitConfirmationCommand_WhenConfirmed_QuitsApp()
    {
        var audioFileService = new Mock<IAudioFileService>();
        audioFileService.Setup(service => service.GetAllFiles()).Returns([]);
        var appCloser = new AppCloserSpy();
        var confirmation = new FakeConfirmationService { Result = true };
        var viewModel = CreateViewModel(audioFileService.Object, appCloser: appCloser, confirmationService: confirmation);

        await viewModel.ShowExitConfirmationCommand.ExecuteAsync(null);

        appCloser.WasQuitCalled.Should().BeTrue();
    }

    [Fact]
    public async Task UploadFileCommand_WhenImportFails_ShowsToast()
    {
        var audioFileService = new Mock<IAudioFileService>();
        audioFileService.Setup(service => service.GetAllFiles()).Returns([]);
        audioFileService
            .Setup(service => service.ImportFileAsync(It.IsAny<PickedFile>(), It.IsAny<CancellationToken>()))
            .ThrowsAsync(new InvalidOperationException("boom"));

        var toast = new RecordingToastService();
        var filePicker = new FakeFilePickerService
        {
            FileToReturn = new PickedFile
            {
                FileName = "message.mp3",
                OpenReadAsync = _ => Task.FromResult<Stream>(new MemoryStream([1, 2, 3])),
            },
        };

        var viewModel = CreateViewModel(audioFileService.Object, filePicker: filePicker, toastService: toast);

        await viewModel.UploadFileCommand.ExecuteAsync(null);

        toast.Messages.Should().ContainSingle(message => message.Contains("Не вдалося"));
    }

    private static SettingsViewModel CreateViewModel(
        IAudioFileService audioFileService,
        IAudioPlayerService? audioPlayer = null,
        IFilePickerService? filePicker = null,
        INavigationService? navigationService = null,
        IToastService? toastService = null,
        IConfirmationService? confirmationService = null,
        IAppCloser? appCloser = null,
        IAppSession? appSession = null,
        IPreferencesService? preferencesService = null)
    {
        return new SettingsViewModel(
            audioFileService,
            audioPlayer ?? Mock.Of<IAudioPlayerService>(),
            filePicker ?? new FakeFilePickerService(),
            navigationService ?? new FakeNavigationService(),
            toastService ?? new RecordingToastService(),
            confirmationService ?? new FakeConfirmationService(),
            appCloser ?? new AppCloserSpy(),
            appSession ?? new AppSession(),
            preferencesService ?? new FakePreferencesService());
    }
}
