namespace PhoneNotificator.Core.Abstractions;

public interface IPreferencesService
{
    int? GetInt(string key);

    string? GetString(string key);

    void SetInt(string key, int value);

    void SetString(string key, string value);

    void Remove(string key);
}
