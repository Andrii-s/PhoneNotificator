using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Tests.TestDoubles;

public sealed class FakePreferencesService : IPreferencesService
{
    private readonly Dictionary<string, string> _storage = new(StringComparer.Ordinal);

    public string? GetString(string key)
    {
        return _storage.TryGetValue(key, out var value) ? value : null;
    }

    public void SetString(string key, string value)
    {
        _storage[key] = value;
    }

    public void Remove(string key)
    {
        _storage.Remove(key);
    }
}
