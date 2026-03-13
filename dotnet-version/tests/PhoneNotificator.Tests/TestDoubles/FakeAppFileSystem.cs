using PhoneNotificator.Core.Abstractions;

namespace PhoneNotificator.Tests.TestDoubles;

public sealed class FakeAppFileSystem : IAppFileSystem
{
    private readonly Dictionary<string, byte[]> _files = new(StringComparer.OrdinalIgnoreCase);

    public FakeAppFileSystem(string appDataDirectory = "C:\\appdata")
    {
        AppDataDirectory = appDataDirectory;
    }

    public string AppDataDirectory { get; }

    public void EnsureDirectory(string path)
    {
    }

    public async Task WriteFileAsync(string destinationPath, Stream source, CancellationToken ct = default)
    {
        using var memoryStream = new MemoryStream();
        await source.CopyToAsync(memoryStream, ct);
        _files[destinationPath] = memoryStream.ToArray();
    }

    public bool FileExists(string path)
    {
        return _files.ContainsKey(path);
    }

    public void DeleteFile(string path)
    {
        _files.Remove(path);
    }

    public void SeedFile(string path, byte[] content)
    {
        _files[path] = content;
    }
}
