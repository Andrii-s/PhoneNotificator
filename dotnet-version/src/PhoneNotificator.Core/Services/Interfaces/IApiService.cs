using PhoneNotificator.Core.Models;

namespace PhoneNotificator.Core.Services.Interfaces;

public interface IApiService
{
    Task<IReadOnlyList<Debtor>> GetDebtorsAsync(CancellationToken ct = default);

    Task SendCallReportAsync(CallReport report, CancellationToken ct = default);
}
