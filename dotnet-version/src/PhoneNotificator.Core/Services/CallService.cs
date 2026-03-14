using PhoneNotificator.Core.Abstractions;
using PhoneNotificator.Core.Models;
using PhoneNotificator.Core.Services.Interfaces;

namespace PhoneNotificator.Core.Services;

public sealed class CallService : ICallService
{
    private static readonly TimeSpan DefaultCallConnectionTimeout = TimeSpan.FromSeconds(30);

    private readonly IAudioPlayerService _audioPlayerService;
    private readonly IPhoneDialerService _phoneDialerService;
    private readonly ICallMonitor _callMonitor;
    private readonly IAudioInjectionService _audioInjectionService;
    private readonly ICallPermissionService _callPermissionService;
    private readonly IAppSession _appSession;
    private readonly TimeSpan _callConnectionTimeout;

    public CallService(
        IAudioPlayerService audioPlayerService,
        IPhoneDialerService phoneDialerService,
        ICallMonitor callMonitor,
        IAudioInjectionService audioInjectionService,
        ICallPermissionService callPermissionService,
        IAppSession appSession,
        TimeSpan? callConnectionTimeout = null)
    {
        _audioPlayerService = audioPlayerService;
        _phoneDialerService = phoneDialerService;
        _callMonitor = callMonitor;
        _audioInjectionService = audioInjectionService;
        _callPermissionService = callPermissionService;
        _appSession = appSession;
        _callConnectionTimeout = callConnectionTimeout ?? DefaultCallConnectionTimeout;
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
        try
        {
            await _callMonitor.WaitForConnectedAsync(ct).WaitAsync(_callConnectionTimeout, ct);
        }
        catch (TimeoutException ex)
        {
            throw new TimeoutException("The call was not connected within the expected time.", ex);
        }

        var callEndedTask = _callMonitor.WaitForEndedAsync(ct);
        await _audioInjectionService.PrepareForCallAudioAsync(ct);
        try
        {
            var audioStartDelay = TimeSpan.FromSeconds(Math.Max(0, _appSession.CallAudioDelaySeconds));
            if (audioStartDelay > TimeSpan.Zero)
            {
                var completedTask = await Task.WhenAny(Task.Delay(audioStartDelay, ct), callEndedTask);
                if (completedTask == callEndedTask)
                {
                    await callEndedTask;
                }
            }

            if (!callEndedTask.IsCompleted)
            {
                await _audioPlayerService.PlayAsync(audioFilePath, ct);
                await callEndedTask;
            }
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
