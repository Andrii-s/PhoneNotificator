using System.Net;
using FluentAssertions;
using PhoneNotificator.Core.Models;
using PhoneNotificator.Core.Services;
using PhoneNotificator.Tests.TestDoubles;

namespace PhoneNotificator.Tests;

public sealed class ApiServiceTests
{
    [Fact]
    public async Task GetDebtorsAsync_ValidJson_ReturnsDeserializedDebtors()
    {
        var handler = new HttpMessageHandlerStub(HttpStatusCode.OK, "[{\"phoneNumber\":\"+380931231212\",\"name\":\"Іваненко\"},{\"phoneNumber\":\"+380671234567\"}]");
        var service = new ApiService(new HttpClient(handler) { BaseAddress = new Uri("https://itproject.com") });

        var result = await service.GetDebtorsAsync();

        result.Should().HaveCount(2);
        result[0].PhoneNumber.Should().Be("+380931231212");
        result[0].Name.Should().Be("Іваненко");
        result[1].Name.Should().BeNull();
    }

    [Fact]
    public async Task GetDebtorsAsync_ServerError_ThrowsHttpRequestException()
    {
        var handler = new HttpMessageHandlerStub(HttpStatusCode.InternalServerError, "{}");
        var service = new ApiService(new HttpClient(handler) { BaseAddress = new Uri("https://itproject.com") });

        var act = () => service.GetDebtorsAsync();

        await act.Should().ThrowAsync<HttpRequestException>();
    }

    [Fact]
    public async Task SendCallReportAsync_ValidReport_PostsExpectedJson()
    {
        string? requestBody = null;
        var handler = new HttpMessageHandlerStub(
            HttpStatusCode.OK,
            "{}",
            async request => requestBody = await request.Content!.ReadAsStringAsync());
        var service = new ApiService(new HttpClient(handler) { BaseAddress = new Uri("https://itproject.com") });
        var report = new CallReport
        {
            PhoneNumber = "+380931231212",
            StartTime = new DateTime(2026, 3, 13, 10, 0, 0, DateTimeKind.Utc),
            EndTime = new DateTime(2026, 3, 13, 10, 2, 30, DateTimeKind.Utc),
        };

        await service.SendCallReportAsync(report);

        requestBody.Should().Contain("380931231212");
        requestBody.Should().Contain("durationFormatted");
        requestBody.Should().Contain("2\\u0445\\u0432");
    }

    [Fact]
    public async Task SendCallReportAsync_ServerError_ThrowsHttpRequestException()
    {
        var handler = new HttpMessageHandlerStub(HttpStatusCode.BadRequest, "{}");
        var service = new ApiService(new HttpClient(handler) { BaseAddress = new Uri("https://itproject.com") });

        var act = () => service.SendCallReportAsync(new CallReport
        {
            PhoneNumber = "+380931231212",
            StartTime = DateTime.UtcNow,
            EndTime = DateTime.UtcNow.AddSeconds(1),
        });

        await act.Should().ThrowAsync<HttpRequestException>();
    }
}
