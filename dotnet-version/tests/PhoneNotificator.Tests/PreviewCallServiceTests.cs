using FluentAssertions;
using PhoneNotificator.Core.Models;
using PhoneNotificator.Core.Services;

namespace PhoneNotificator.Tests;

public sealed class PreviewCallServiceTests
{
    [Fact]
    public async Task MakeCallAsync_ReturnsCallReport()
    {
        var service = new PreviewCallService();
        var audioFilePath = Path.GetTempFileName();
        await File.WriteAllBytesAsync(audioFilePath, [1, 2, 3]);

        try
        {
            CallReport? report = null;
            await service.MakeCallAsync("+380001112233", audioFilePath, currentReport =>
            {
                report = currentReport;
                return Task.CompletedTask;
            });

            report.Should().NotBeNull();
            report!.PhoneNumber.Should().Be("+380001112233");
        }
        finally
        {
            File.Delete(audioFilePath);
        }
    }

    [Fact]
    public async Task MakeCallsSequentialAsync_CompletesAllNumbers()
    {
        var service = new PreviewCallService();
        var audioFilePath = Path.GetTempFileName();
        await File.WriteAllBytesAsync(audioFilePath, [1, 2, 3]);

        try
        {
            var count = 0;
            await service.MakeCallsSequentialAsync(
                ["+380001112233", "+380002223344"],
                audioFilePath,
                _ =>
                {
                    count++;
                    return Task.CompletedTask;
                });

            count.Should().Be(2);
        }
        finally
        {
            File.Delete(audioFilePath);
        }
    }
}
