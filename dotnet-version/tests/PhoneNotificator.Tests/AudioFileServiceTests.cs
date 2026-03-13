using System.Text.Json;
using FluentAssertions;
using PhoneNotificator.Core.Abstractions;
using PhoneNotificator.Core.Services;
using PhoneNotificator.Tests.TestDoubles;

namespace PhoneNotificator.Tests;

public sealed class AudioFileServiceTests
{
    [Fact]
    public async Task ImportFileAsync_ValidFile_AddsFileAndPersistsMetadata()
    {
        var fileSystem = new FakeAppFileSystem();
        var preferences = new FakePreferencesService();
        var service = new AudioFileService(fileSystem, preferences);
        var pickedFile = CreatePickedFile("message.mp3", [1, 2, 3]);

        var result = await service.ImportFileAsync(pickedFile);

        result.FileName.Should().Be("message.mp3");
        fileSystem.FileExists(result.FilePath).Should().BeTrue();
        var persistedPayload = preferences.GetString("audio_files");
        persistedPayload.Should().NotBeNullOrWhiteSpace();
        JsonSerializer.Deserialize<List<PhoneNotificator.Core.Models.AudioFile>>(persistedPayload!).Should().ContainSingle();
    }

    [Fact]
    public async Task GetAllFiles_FiltersOutMissingFiles()
    {
        var fileSystem = new FakeAppFileSystem();
        var preferences = new FakePreferencesService();
        var service = new AudioFileService(fileSystem, preferences);
        var imported = await service.ImportFileAsync(CreatePickedFile("message.mp3", [1, 2, 3]));

        fileSystem.DeleteFile(imported.FilePath);

        var files = service.GetAllFiles();

        files.Should().BeEmpty();
        preferences.GetString("audio_files").Should().BeNull();
    }

    [Fact]
    public async Task DeleteFile_RemovesStoredFileAndMetadata()
    {
        var fileSystem = new FakeAppFileSystem();
        var preferences = new FakePreferencesService();
        var service = new AudioFileService(fileSystem, preferences);
        var imported = await service.ImportFileAsync(CreatePickedFile("message.mp3", [1, 2, 3]));

        service.DeleteFile(imported);

        fileSystem.FileExists(imported.FilePath).Should().BeFalse();
        service.GetAllFiles().Should().BeEmpty();
    }

    private static PickedFile CreatePickedFile(string fileName, byte[] content)
    {
        return new PickedFile
        {
            FileName = fileName,
            OpenReadAsync = _ => Task.FromResult<Stream>(new MemoryStream(content)),
        };
    }
}
