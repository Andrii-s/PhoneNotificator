using System.Net.Http.Json;
using System.Text;
using System.Text.Json;
using PhoneNotificator.Core.Models;
using PhoneNotificator.Core.Services.Interfaces;

namespace PhoneNotificator.Core.Services;

public sealed class ApiService : IApiService
{
    private static readonly JsonSerializerOptions JsonSerializerOptions = new(JsonSerializerDefaults.Web)
    {
        PropertyNameCaseInsensitive = true,
    };

    private readonly HttpClient _httpClient;

    public ApiService(HttpClient httpClient)
    {
        _httpClient = httpClient;
    }

    public async Task<IReadOnlyList<Debtor>> GetDebtorsAsync(CancellationToken ct = default)
    {
        using var response = await _httpClient.PostAsync(
            "/api/debetors",
            new StringContent("{}", Encoding.UTF8, "application/json"),
            ct);

        response.EnsureSuccessStatusCode();

        await using var contentStream = await response.Content.ReadAsStreamAsync(ct);
        var debtors = await JsonSerializer.DeserializeAsync<List<Debtor>>(contentStream, JsonSerializerOptions, ct);
        return debtors ?? [];
    }

    public async Task SendCallReportAsync(CallReport report, CancellationToken ct = default)
    {
        using var response = await _httpClient.PostAsJsonAsync(
            "/api/debetor_report",
            new
            {
                phoneNumber = report.PhoneNumber,
                startTime = report.StartTime,
                endTime = report.EndTime,
                durationFormatted = report.DurationFormatted,
            },
            JsonSerializerOptions,
            ct);

        response.EnsureSuccessStatusCode();
    }
}
