package com.yourcompany.credittracker.util;

import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class Base32SecretGenerator {
    private static final int SECRET_BYTES = 20;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base32 base32 = new Base32();

    public String generate() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return base32.encodeAsString(bytes).replace("=", "");
    }
}
