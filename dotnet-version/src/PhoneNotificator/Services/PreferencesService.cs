using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Services;

public sealed class PreferencesService : IPreferencesService
{
    public int? GetInt(string key)
    {
        return Preferences.Default.ContainsKey(key) ? Preferences.Default.Get(key, 0) : null;
    }

    public string? GetString(string key)
    {
        return Preferences.Default.ContainsKey(key) ? Preferences.Default.Get(key, string.Empty) : null;
    }

    public void SetInt(string key, int value)
    {
        Preferences.Default.Set(key, value);
    }

    public void SetString(string key, string value)
    {
        Preferences.Default.Set(key, value);
    }

    public void Remove(string key)
    {
        Preferences.Default.Remove(key);
    }
}
