namespace PhoneNotificator.Core.Abstractions;

public interface IPreferencesService
{
    string? GetString(string key);

    void SetString(string key, string value);

    void Remove(string key);
}
