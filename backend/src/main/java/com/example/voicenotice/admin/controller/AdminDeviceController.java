package com.example.voicenotice.admin.controller;

import com.example.voicenotice.admin.dto.AdminDeviceResponse;
import com.example.voicenotice.admin.service.AdminDeviceService;
import com.example.voicenotice.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/devices")
public class AdminDeviceController {

    private final AdminDeviceService adminDeviceService;

    @GetMapping
    public ApiResponse<List<AdminDeviceResponse>> getAllDevices() {
        return ApiResponse.ok(adminDeviceService.getAllDevices());
    }

    @GetMapping("/{deviceId}")
    public ApiResponse<AdminDeviceResponse> getDevice(
            @PathVariable Long deviceId
    ) {
        return ApiResponse.ok(adminDeviceService.getDevice(deviceId));
    }

    @GetMapping("/search")
    public ApiResponse<List<AdminDeviceResponse>> searchDevices(
            @RequestParam(required = false) String deviceUid,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(
                adminDeviceService.searchDevices(deviceUid, status)
        );
    }

    @PatchMapping("/{deviceId}/disable")
    public ResponseEntity<ApiResponse<Void>> disableDevice(
            @PathVariable Long deviceId
    ) {
        adminDeviceService.disableDevice(deviceId);

        return ResponseEntity.ok(
                ApiResponse.ok(null, "디바이스 비활성화 완료")
        );
    }

    @PatchMapping("/{deviceId}/enable")
    public ResponseEntity<ApiResponse<Void>> enableDevice(
            @PathVariable Long deviceId
    ) {
        adminDeviceService.enableDevice(deviceId);

        return ResponseEntity.ok(
                ApiResponse.ok(null, "디바이스 활성화 완료")
        );
    }

    @PatchMapping("/{deviceId}/delete")
    public ResponseEntity<ApiResponse<Void>> deleteDevice(
            @PathVariable Long deviceId
    ) {
        adminDeviceService.deleteDevice(deviceId);

        return ResponseEntity.ok(
                ApiResponse.ok(null, "디바이스 삭제 완료")
        );
    }
}