package com.example.voicenotice.device.controller;

import com.example.voicenotice.common.response.ApiResponse;
import com.example.voicenotice.device.entity.Device;
import com.example.voicenotice.device.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    public record RegisterDeviceRequest(String deviceUid, String location) {}
    public record RegisterDeviceResponse(Long deviceId, String deviceUid, String status) {}

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterDeviceResponse>> register(@RequestBody RegisterDeviceRequest request) {
        Device device = deviceService.register(request.deviceUid(), request.location());
        return ResponseEntity.ok(ApiResponse.ok(
                new RegisterDeviceResponse(device.getId(), device.getDeviceUid(), device.getStatus())
        ));
    }
}
