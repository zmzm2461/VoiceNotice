package com.example.voicenotice.domain.user.exception;

public class InvalidAdminInviteCodeException extends RuntimeException {

    public InvalidAdminInviteCodeException() {
        super("관리자 코드가 올바르지 않습니다.");
    }
}