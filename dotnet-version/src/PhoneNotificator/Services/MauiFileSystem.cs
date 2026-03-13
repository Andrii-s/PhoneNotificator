using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Services;

public sealed class MauiFileSystem : IAppFileSystem
{
    public string AppDataDirectory => FileSystem.Current.AppDataDirectory;

    public void EnsureDirectory(string path)
    {
        Directory.CreateDirectory(path);
    }

    public async Task WriteFileAsync(string destinationPath, Stream source, CancellationToken ct = default)
    {
        await using var destination = File.Create(destinationPath);
        await source.CopyToAsync(destination, ct);
    }

    public bool FileExists(string path)
    {
        return File.Exists(path);
    }

    public void DeleteFile(string path)
    {
        File.Delete(path);
    }
}
