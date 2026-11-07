package com.kuet.ums.service;

import com.kuet.ums.exception.BadRequestException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    private final PasswordEncoder passwordEncoder;

    public PasswordPolicy(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String encodeRequired(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BadRequestException("Password is required.");
        }
        return encode(rawPassword);
    }

    public String encodeOptional(String rawPassword, String currentHash) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return currentHash;
        }
        return encode(rawPassword);
    }

    private String encode(String rawPassword) {
        if (rawPassword.length() < 6 || rawPassword.length() > 72) {
            throw new BadRequestException("Password must be between 6 and 72 characters.");
        }
        return passwordEncoder.encode(rawPassword);
    }
}
