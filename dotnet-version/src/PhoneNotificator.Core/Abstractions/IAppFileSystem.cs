namespace PhoneNotificator.Core.Abstractions;

public interface IAppFileSystem
{
    string AppDataDirectory { get; }

    void EnsureDirectory(string path);

    Task WriteFileAsync(string destinationPath, Stream source, CancellationToken ct = default);

    bool FileExists(string path);

    void DeleteFile(string path);
}
