using PhoneNotificator.Core.Abstractions;
using PhoneNotificator.Core.Models;
using PhoneNotificator.Core.Services.Interfaces;

namespace PhoneNotificator.Core.Services;

public sealed class CallService : ICallService
{
    private readonly IAudioPlayerService _audioPlayerService;
    private readonly IPhoneDialerService _phoneDialerService;
    private readonly ICallMonitor _callMonitor;
    private readonly IAudioInjectionService _audioInjectionService;
    private readonly ICallPermissionService _callPermissionService;

    public CallService(
        IAudioPlayerService audioPlayerService,
        IPhoneDialerService phoneDialerService,
        ICallMonitor callMonitor,
        IAudioInjectionService audioInjectionService,
        ICallPermissionService callPermissionService)
    {
        _audioPlayerService = audioPlayerService;
        _phoneDialerService = phoneDialerService;
        _callMonitor = callMonitor;
        _audioInjectionService = audioInjectionService;
        _callPermissionService = callPermissionService;
    }

    public async Task MakeCallAsync(
        string phoneNumber,
        string audioFilePath,
        Func<CallReport, Task> onCallCompleted,
        CancellationToken ct = default)
    {
        if (!File.Exists(audioFilePath))
        {
            throw new FileNotFoundException("Audio file was not found.", audioFilePath);
        }

        var permissionGranted = await _callPermissionService.EnsureGrantedAsync(ct);
        if (!permissionGranted)
        {
            throw new InvalidOperationException("Call permissions were not granted.");
        }

        _callMonitor.Reset();
        var startTime = DateTime.UtcNow;

        await _phoneDialerService.DialAsync(phoneNumber, ct);
        await _callMonitor.WaitForConnectedAsync(ct);
        await Task.Delay(TimeSpan.FromSeconds(1), ct);

        await _audioInjectionService.PrepareForCallAudioAsync(ct);
        try
        {
            await _audioPlayerService.PlayAsync(audioFilePath, ct);
            await _callMonitor.WaitForEndedAsync(ct);
        }
        finally
        {
            await _audioPlayerService.StopAsync(ct);
            await _audioInjectionService.RestoreAfterCallAsync(ct);
        }

        var endTime = DateTime.UtcNow;
        await onCallCompleted(new CallReport
        {
            PhoneNumber = phoneNumber,
            StartTime = startTime,
            EndTime = endTime,
        });
    }

    public async Task MakeCallsSequentialAsync(
        IEnumerable<string> phoneNumbers,
        string audioFilePath,
        Func<CallReport, Task> onEachCallCompleted,
        CancellationToken ct = default)
    {
        foreach (var phoneNumber in phoneNumbers)
        {
            ct.ThrowIfCancellationRequested();
            await MakeCallAsync(phoneNumber, audioFilePath, onEachCallCompleted, ct);
        }
    }
}
