package com.example.voicenotice.device.controller;

import com.example.voicenotice.common.response.ApiResponse;
import com.example.voicenotice.device.entity.DevicePairing;
import com.example.voicenotice.device.service.DevicePairingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/device-pairings")
public class DevicePairingController {

    private final DevicePairingService devicePairingService;

    @PostMapping
    public ApiResponse<DevicePairingResponse> pair(
            @RequestBody PairRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");

        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        DevicePairing pairing = devicePairingService.pair(request.deviceUid(), userId);

        return ApiResponse.ok(new DevicePairingResponse(
                pairing.getId(),
                pairing.getDevice().getId(),
                pairing.getDevice().getDeviceUid(),
                pairing.getDevice().getLocation(),
                pairing.getUser().getId(),
                pairing.getPairedAt()
        ));
    }

    public record PairRequest(
            String deviceUid
    ) {}

    public record DevicePairingResponse(
            Long pairingId,
            Long deviceId,
            String deviceUid,
            String location,
            Long userId,
            java.time.LocalDateTime pairedAt
    ) {}
}