using System.Net;

namespace PhoneNotificator.Tests.TestDoubles;

public sealed class HttpMessageHandlerStub : HttpMessageHandler
{
    private readonly Func<HttpRequestMessage, CancellationToken, Task<HttpResponseMessage>> _handler;

    public HttpMessageHandlerStub(HttpStatusCode statusCode, string content, Func<HttpRequestMessage, Task>? onRequest = null)
        : this(
            async (request, cancellationToken) =>
            {
                if (onRequest is not null)
                {
                    await onRequest(request);
                }

                return new HttpResponseMessage(statusCode)
                {
                    Content = new StringContent(content),
                    RequestMessage = request,
                };
            })
    {
    }

    public HttpMessageHandlerStub(Func<HttpRequestMessage, CancellationToken, Task<HttpResponseMessage>> handler)
    {
        _handler = handler;
    }

    protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        return _handler(request, cancellationToken);
    }
}
