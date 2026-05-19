package com.example.voicenotice.device.controller;


import com.example.voicenotice.common.response.ApiResponse;
import com.example.voicenotice.device.dto.DeviceCommandResponse;
import com.example.voicenotice.device.service.DeviceCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceCommandController {

    private final DeviceCommandService deviceCommandService;

    @GetMapping("/{deviceUid}/commands/latest")
    public ApiResponse<DeviceCommandResponse> getLatestCommand(
            @PathVariable String deviceUid
    ) {

        return ApiResponse.ok(
                deviceCommandService.getLatestCommand(deviceUid)
        );
    }
}