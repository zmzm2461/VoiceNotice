package com.example.voicenotice.api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test/me")
    public String me(Authentication authentication) {
        if (authentication == null) {
            return "authentication is null";
        }

        return "current userId = " + authentication.getPrincipal()
                + ", authorities = " + authentication.getAuthorities();
    }
}