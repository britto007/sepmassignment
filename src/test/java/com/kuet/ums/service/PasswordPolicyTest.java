package com.kuet.ums.service;

import com.kuet.ums.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordPolicy passwordPolicy;

    @Test
    void encodeRequiredRejectsABlankPassword() {
        BadRequestException blank = assertThrows(BadRequestException.class, () -> passwordPolicy.encodeRequired("   "));
        BadRequestException missing = assertThrows(BadRequestException.class, () -> passwordPolicy.encodeRequired(null));

        assertEquals("Password is required.", blank.getMessage());
        assertEquals("Password is required.", missing.getMessage());
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void encodeRequiredRejectsAPasswordShorterThanSixCharacters() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> passwordPolicy.encodeRequired("abc"));

        assertEquals("Password must be between 6 and 72 characters.", exception.getMessage());
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void encodeRequiredRejectsAPasswordLongerThanSeventyTwoCharacters() {
        String tooLong = "a".repeat(73);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> passwordPolicy.encodeRequired(tooLong));

        assertEquals("Password must be between 6 and 72 characters.", exception.getMessage());
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void encodeRequiredEncodesAValidPassword() {
        when(passwordEncoder.encode("secret1")).thenReturn("$2a$hashed");

        assertEquals("$2a$hashed", passwordPolicy.encodeRequired("secret1"));
        verify(passwordEncoder).encode("secret1");
    }

    @Test
    void encodeOptionalKeepsTheCurrentHashWhenTheNewPasswordIsBlank() {
        assertEquals("current-hash", passwordPolicy.encodeOptional("  ", "current-hash"));
        assertEquals("current-hash", passwordPolicy.encodeOptional(null, "current-hash"));
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void encodeOptionalEncodesWhenANewPasswordIsSupplied() {
        when(passwordEncoder.encode("newpass")).thenReturn("$2a$new");

        assertEquals("$2a$new", passwordPolicy.encodeOptional("newpass", "current-hash"));
        verify(passwordEncoder).encode("newpass");
    }
}
