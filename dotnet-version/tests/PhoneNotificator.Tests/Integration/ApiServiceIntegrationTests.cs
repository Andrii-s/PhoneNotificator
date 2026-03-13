using System.Net;
using FluentAssertions;
using Microsoft.Extensions.DependencyInjection;
using PhoneNotificator.Core.Services;
using PhoneNotificator.Core.Services.Interfaces;
using Polly;
using WireMock.RequestBuilders;
using WireMock.ResponseBuilders;
using WireMock.Server;
using WireMock.Settings;

namespace PhoneNotificator.Tests.Integration;

public sealed class ApiServiceIntegrationTests : IDisposable
{
    private readonly WireMockServer _server = WireMockServer.Start(new WireMockServerSettings
    {
        StartAdminInterface = true,
        ReadStaticMappings = false,
    });

    [Fact]
    [Trait("Category", "Integration")]
    public async Task GetDebtorsAsync_DeserializesWireMockResponse()
    {
        _server
            .Given(Request.Create().WithPath("/api/debetors").UsingPost())
            .RespondWith(Response.Create().WithStatusCode(HttpStatusCode.OK).WithBody("[{\"phoneNumber\":\"+380931231212\",\"name\":\"Іваненко\"}]"));

        var service = CreateApiService();

        var result = await service.GetDebtorsAsync();

        result.Should().ContainSingle();
        result[0].PhoneNumber.Should().Be("+380931231212");
    }

    [Fact]
    [Trait("Category", "Integration")]
    public async Task GetDebtorsAsync_RetriesUntilSuccess()
    {
        _server
            .Given(Request.Create().WithPath("/api/debetors").UsingPost())
            .InScenario("retry")
            .WillSetStateTo("second")
            .RespondWith(Response.Create().WithStatusCode(HttpStatusCode.ServiceUnavailable));

        _server
            .Given(Request.Create().WithPath("/api/debetors").UsingPost())
            .InScenario("retry")
            .WhenStateIs("second")
            .WillSetStateTo("third")
            .RespondWith(Response.Create().WithStatusCode(HttpStatusCode.ServiceUnavailable));

        _server
            .Given(Request.Create().WithPath("/api/debetors").UsingPost())
            .InScenario("retry")
            .WhenStateIs("third")
            .RespondWith(Response.Create().WithStatusCode(HttpStatusCode.OK).WithBody("[]"));

        var service = CreateApiService();

        var result = await service.GetDebtorsAsync();

        result.Should().BeEmpty();
        _server.LogEntries.Count.Should().Be(3);
    }

    [Fact]
    [Trait("Category", "Integration")]
    public async Task SendCallReportAsync_PostsExpectedBody()
    {
        _server
            .Given(Request.Create().WithPath("/api/debetor_report").UsingPost())
            .RespondWith(Response.Create().WithStatusCode(HttpStatusCode.OK));

        var service = CreateApiService();

        await service.SendCallReportAsync(new PhoneNotificator.Core.Models.CallReport
        {
            PhoneNumber = "+380931231212",
            StartTime = new DateTime(2026, 3, 13, 10, 0, 0, DateTimeKind.Utc),
            EndTime = new DateTime(2026, 3, 13, 10, 2, 30, DateTimeKind.Utc),
        });

        _server.LogEntries.Should().ContainSingle(entry =>
            entry.RequestMessage.AbsolutePath == "/api/debetor_report" &&
            entry.RequestMessage.Body != null &&
            entry.RequestMessage.Body.Contains("380931231212"));
    }

    public void Dispose()
    {
        _server.Dispose();
    }

    private IApiService CreateApiService()
    {
        var services = new ServiceCollection();
        services
            .AddHttpClient<IApiService, ApiService>(client =>
            {
                client.BaseAddress = new Uri(_server.Url!);
                client.Timeout = TimeSpan.FromSeconds(5);
            })
            .AddTransientHttpErrorPolicy(policyBuilder =>
                policyBuilder.WaitAndRetryAsync(2, attempt => TimeSpan.FromMilliseconds(10)));

        return services.BuildServiceProvider().GetRequiredService<IApiService>();
    }
}
