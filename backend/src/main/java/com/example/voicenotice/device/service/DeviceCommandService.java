package com.example.voicenotice.device.service;


import com.example.voicenotice.device.entity.Device;
import com.example.voicenotice.device.dto.DeviceCommandResponse;
import com.example.voicenotice.device.entity.DeviceCommand;
import com.example.voicenotice.device.repository.DeviceCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceCommandService {

    private final DeviceCommandRepository deviceCommandRepository;

    @Transactional
    public void createPlayReplyCommand(
            Device device,
            Integer replyCode
    ) {

        DeviceCommand command = new DeviceCommand(
                device,
                "PLAY_REPLY",
                replyCode
        );

        deviceCommandRepository.save(command);
    }

    @Transactional
    public DeviceCommandResponse getLatestCommand(String deviceUid) {

        DeviceCommand command =
                deviceCommandRepository
                        .findTopByDevice_DeviceUidAndProcessedFalseOrderByIdAsc(deviceUid)
                        .orElse(null);

        if (command == null) {
            return null;
        }

        command.processed();

        return DeviceCommandResponse.from(command);
    }
}