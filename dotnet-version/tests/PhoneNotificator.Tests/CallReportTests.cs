using FluentAssertions;
using PhoneNotificator.Core.Models;

namespace PhoneNotificator.Tests;

public sealed class CallReportTests
{
    [Theory]
    [InlineData(0, 0, 30, "0хв 30сек")]
    [InlineData(0, 1, 0, "1хв 0сек")]
    [InlineData(0, 2, 30, "2хв 30сек")]
    public void DurationFormatted_ReturnsExpectedValue(int hours, int minutes, int seconds, string expected)
    {
        var report = new CallReport
        {
            StartTime = new DateTime(2026, 3, 13, 10, 0, 0, DateTimeKind.Utc),
            EndTime = new DateTime(2026, 3, 13, 10, 0, 0, DateTimeKind.Utc)
                .AddHours(hours)
                .AddMinutes(minutes)
                .AddSeconds(seconds),
        };

        report.DurationFormatted.Should().Be(expected);
    }

    [Fact]
    public void Duration_ReturnsTimeSpanDifference()
    {
        var report = new CallReport
        {
            StartTime = new DateTime(2026, 3, 13, 10, 0, 0, DateTimeKind.Utc),
            EndTime = new DateTime(2026, 3, 13, 10, 2, 30, DateTimeKind.Utc),
        };

        report.Duration.Should().Be(TimeSpan.FromMinutes(2.5));
    }
}
