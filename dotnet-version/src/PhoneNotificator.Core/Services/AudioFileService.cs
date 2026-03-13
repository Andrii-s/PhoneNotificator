using System.Text.Json;
using PhoneNotificator.Core.Abstractions;
using PhoneNotificator.Core.Models;
using PhoneNotificator.Core.Services.Interfaces;

namespace PhoneNotificator.Core.Services;

public sealed class AudioFileService : IAudioFileService
{
    private const string PreferenceKey = "audio_files";
    private static readonly string[] AllowedExtensions = [".mp3", ".wav", ".aac"];

    private readonly IAppFileSystem _fileSystem;
    private readonly IPreferencesService _preferencesService;

    public AudioFileService(IAppFileSystem fileSystem, IPreferencesService preferencesService)
    {
        _fileSystem = fileSystem;
        _preferencesService = preferencesService;
    }

    public async Task<AudioFile> ImportFileAsync(PickedFile file, CancellationToken ct = default)
    {
        var extension = Path.GetExtension(file.FileName);
        if (string.IsNullOrWhiteSpace(extension) || !AllowedExtensions.Contains(extension, StringComparer.OrdinalIgnoreCase))
        {
            throw new InvalidOperationException("Unsupported audio file type.");
        }

        var destinationDirectory = Path.Combine(_fileSystem.AppDataDirectory, "audio");
        _fileSystem.EnsureDirectory(destinationDirectory);

        var destinationPath = BuildUniquePath(destinationDirectory, file.FileName);
        await using var source = await file.OpenReadAsync(ct);
        await _fileSystem.WriteFileAsync(destinationPath, source, ct);

        var audioFiles = LoadFiles().ToList();
        var importedFile = new AudioFile
        {
            FileName = Path.GetFileName(destinationPath),
            FilePath = destinationPath,
        };

        audioFiles.Add(importedFile);
        SaveFiles(audioFiles);

        return importedFile;
    }

    public IReadOnlyList<AudioFile> GetAllFiles()
    {
        var files = LoadFiles()
            .Where(file => _fileSystem.FileExists(file.FilePath))
            .ToList();

        SaveFiles(files);
        return files;
    }

    public void DeleteFile(AudioFile file)
    {
        var files = LoadFiles()
            .Where(existing => existing.Id != file.Id)
            .ToList();

        if (_fileSystem.FileExists(file.FilePath))
        {
            _fileSystem.DeleteFile(file.FilePath);
        }

        SaveFiles(files);
    }

    private IReadOnlyList<AudioFile> LoadFiles()
    {
        var payload = _preferencesService.GetString(PreferenceKey);
        if (string.IsNullOrWhiteSpace(payload))
        {
            return [];
        }

        return JsonSerializer.Deserialize<List<AudioFile>>(payload) ?? [];
    }

    private void SaveFiles(IReadOnlyList<AudioFile> files)
    {
        if (files.Count == 0)
        {
            _preferencesService.Remove(PreferenceKey);
            return;
        }

        _preferencesService.SetString(PreferenceKey, JsonSerializer.Serialize(files));
    }

    private string BuildUniquePath(string directoryPath, string originalFileName)
    {
        var extension = Path.GetExtension(originalFileName);
        var fileNameWithoutExtension = SanitizeFileName(Path.GetFileNameWithoutExtension(originalFileName));
        var suffix = 0;

        while (true)
        {
            var candidateName = suffix == 0
                ? $"{fileNameWithoutExtension}{extension}"
                : $"{fileNameWithoutExtension}_{suffix}{extension}";
            var candidatePath = Path.Combine(directoryPath, candidateName);

            if (!_fileSystem.FileExists(candidatePath))
            {
                return candidatePath;
            }

            suffix++;
        }
    }

    private static string SanitizeFileName(string fileName)
    {
        var invalidChars = Path.GetInvalidFileNameChars();
        var sanitized = new string(fileName.Select(ch => invalidChars.Contains(ch) ? '_' : ch).ToArray());
        return string.IsNullOrWhiteSpace(sanitized) ? "audio_file" : sanitized;
    }
}
