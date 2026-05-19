package com.example.voicenotice.device.dto;

import com.example.voicenotice.device.entity.DeviceCommand;

public record DeviceCommandResponse(
        String commandType,
        Integer replyCode
) {
    public static DeviceCommandResponse from(DeviceCommand command) {
        return new DeviceCommandResponse(
                command.getCommandType(),
                command.getReplyCode()
        );
    }
}