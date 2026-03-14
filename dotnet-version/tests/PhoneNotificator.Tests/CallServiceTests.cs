using FluentAssertions;
using Moq;
using PhoneNotificator.Core.Abstractions;
using PhoneNotificator.Core.Models;
using PhoneNotificator.Core.Services;
using PhoneNotificator.Core.Services.Interfaces;
using PhoneNotificator.Tests.TestDoubles;

namespace PhoneNotificator.Tests;

public sealed class CallServiceTests
{
    [Fact]
    public async Task MakeCallAsync_CompletesReportAndDialsPhone()
    {
        var audioFilePath = Path.GetTempFileName();
        await File.WriteAllBytesAsync(audioFilePath, [1, 2, 3]);
        try
        {
            var audioPlayer = new Mock<IAudioPlayerService>();
            var phoneDialer = new PhoneDialerSpy();
            var callMonitor = new FakeCallMonitor();
            var service = new CallService(
                audioPlayer.Object,
                phoneDialer,
                callMonitor,
                new NoOpAudioInjectionServiceStub(),
                new FakeCallPermissionService(),
                new AppSession { CallAudioDelaySeconds = 0 });

            CallReport? report = null;
            await service.MakeCallAsync("+380001112233", audioFilePath, currentReport =>
            {
                report = currentReport;
                return Task.CompletedTask;
            });

            report.Should().NotBeNull();
            report!.PhoneNumber.Should().Be("+380001112233");
            report.EndTime.Should().BeAfter(report.StartTime);
            phoneDialer.DialedPhoneNumbers.Should().ContainSingle("+380001112233");
        }
        finally
        {
            File.Delete(audioFilePath);
        }
    }

    [Fact]
    public async Task MakeCallAsync_WaitsConfiguredDelayBeforePlayingAudio()
    {
        var audioFilePath = Path.GetTempFileName();
        await File.WriteAllBytesAsync(audioFilePath, [1, 2, 3]);
        try
        {
            DateTimeOffset? connectedAt = null;
            DateTimeOffset? playedAt = null;
            var audioPlayer = new Mock<IAudioPlayerService>();
            audioPlayer
                .Setup(player => player.PlayAsync(It.IsAny<string>(), It.IsAny<CancellationToken>()))
                .Callback(() => playedAt = DateTimeOffset.UtcNow)
                .Returns(Task.CompletedTask);

            var callMonitor = new FakeCallMonitor
            {
                OnConnected = () => connectedAt = DateTimeOffset.UtcNow,
                EndedDelay = TimeSpan.FromMilliseconds(1200),
            };

            var service = new CallService(
                audioPlayer.Object,
                new PhoneDialerSpy(),
                callMonitor,
                new NoOpAudioInjectionServiceStub(),
                new FakeCallPermissionService(),
                new AppSession { CallAudioDelaySeconds = 1 });

            await service.MakeCallAsync("+380001112233", audioFilePath, _ => Task.CompletedTask);

            playedAt.Should().NotBeNull();
            connectedAt.Should().NotBeNull();
            (playedAt!.Value - connectedAt!.Value).Should().BeGreaterThanOrEqualTo(TimeSpan.FromSeconds(1));
        }
        finally
        {
            File.Delete(audioFilePath);
        }
    }

    [Fact]
    public async Task MakeCallsSequentialAsync_CallsEveryPhoneNumber()
    {
        var audioFilePath = Path.GetTempFileName();
        await File.WriteAllBytesAsync(audioFilePath, [1, 2, 3]);
        try
        {
            var audioPlayer = new Mock<IAudioPlayerService>();
            var service = new CallService(
                audioPlayer.Object,
                new PhoneDialerSpy(),
                new FakeCallMonitor(),
                new NoOpAudioInjectionServiceStub(),
                new FakeCallPermissionService(),
                new AppSession { CallAudioDelaySeconds = 0 });

            var reportCount = 0;
            await service.MakeCallsSequentialAsync(
                ["+380001112233", "+380002223344", "+380003334455"],
                audioFilePath,
                _ =>
                {
                    reportCount++;
                    return Task.CompletedTask;
                });

            reportCount.Should().Be(3);
        }
        finally
        {
            File.Delete(audioFilePath);
        }
    }

    [Fact]
    public async Task MakeCallAsync_WhenCallDoesNotConnect_ThrowsTimeoutException()
    {
        var audioFilePath = Path.GetTempFileName();
        await File.WriteAllBytesAsync(audioFilePath, [1, 2, 3]);
        try
        {
            var service = new CallService(
                Mock.Of<IAudioPlayerService>(),
                new PhoneDialerSpy(),
                new NeverConnectingCallMonitor(),
                new NoOpAudioInjectionServiceStub(),
                new FakeCallPermissionService(),
                new AppSession { CallAudioDelaySeconds = 0 },
                TimeSpan.FromMilliseconds(100));

            var act = () => service.MakeCallAsync("+380001112233", audioFilePath, _ => Task.CompletedTask);

            await act.Should().ThrowAsync<TimeoutException>();
        }
        finally
        {
            File.Delete(audioFilePath);
        }
    }

    [Fact]
    public async Task MakeCallAsync_MissingAudioFile_ThrowsFileNotFoundException()
    {
        var service = new CallService(
            Mock.Of<IAudioPlayerService>(),
            new PhoneDialerSpy(),
            new FakeCallMonitor(),
            new NoOpAudioInjectionServiceStub(),
            new FakeCallPermissionService(),
            new AppSession { CallAudioDelaySeconds = 0 });

        var act = () => service.MakeCallAsync("123", "missing.mp3", _ => Task.CompletedTask);

        await act.Should().ThrowAsync<FileNotFoundException>();
    }

    private sealed class NoOpAudioInjectionServiceStub : IAudioInjectionService
    {
        public Task PrepareForCallAudioAsync(CancellationToken ct = default) => Task.CompletedTask;

        public Task RestoreAfterCallAsync(CancellationToken ct = default) => Task.CompletedTask;
    }

    private sealed class NeverConnectingCallMonitor : ICallMonitor
    {
        public void Reset()
        {
        }

        public Task WaitForConnectedAsync(CancellationToken ct = default)
        {
            return Task.Delay(Timeout.InfiniteTimeSpan, ct);
        }

        public Task WaitForEndedAsync(CancellationToken ct = default)
        {
            return Task.Delay(Timeout.InfiniteTimeSpan, ct);
        }
    }
}
