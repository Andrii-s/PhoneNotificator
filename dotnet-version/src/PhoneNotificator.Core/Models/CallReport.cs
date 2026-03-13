namespace PhoneNotificator.Core.Models;

public sealed class CallReport
{
    public string PhoneNumber { get; set; } = string.Empty;

    public DateTime StartTime { get; set; }

    public DateTime EndTime { get; set; }

    public TimeSpan Duration => EndTime - StartTime;

    public string DurationFormatted => $"{(int)Duration.TotalMinutes}хв {Duration.Seconds}сек";
}
