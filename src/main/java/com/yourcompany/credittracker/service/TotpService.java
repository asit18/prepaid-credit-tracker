package com.yourcompany.credittracker.service;

import com.yourcompany.credittracker.security.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TotpService {
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    private final SecurityProperties securityProperties;

    public TotpVerificationResult verify(String base32Secret, String code) {
        if (code == null || !code.matches("^\\d{6}$")) {
            return TotpVerificationResult.invalid();
        }
        long currentStep = currentTimeStep();
        int window = securityProperties.mfa().allowedWindow();
        for (int offset = -window; offset <= window; offset++) {
            long step = currentStep + offset;
            if (constantTimeEquals(generateCode(base32Secret, step), code)) {
                return TotpVerificationResult.valid(step);
            }
        }
        return TotpVerificationResult.invalid();
    }

    public long currentTimeStep() {
        return Instant.now().getEpochSecond() / securityProperties.mfa().timeStepSeconds();
    }

    private String generateCode(String base32Secret, long timeStep) {
        try {
            byte[] key = new Base32().decode(base32Secret);
            byte[] counter = ByteBuffer.allocate(8).putLong(timeStep).array();
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(counter);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int modulus = (int) Math.pow(10, securityProperties.mfa().digits());
            return String.format("%0" + securityProperties.mfa().digits() + "d", binary % modulus);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to generate TOTP code", ex);
        }
    }

    private boolean constantTimeEquals(String expected, String provided) {
        if (expected == null || provided == null || expected.length() != provided.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expected.length(); i++) {
            result |= expected.charAt(i) ^ provided.charAt(i);
        }
        return result == 0;
    }

    public record TotpVerificationResult(boolean valid, Long timeStep) {
        static TotpVerificationResult valid(long timeStep) {
            return new TotpVerificationResult(true, timeStep);
        }

        static TotpVerificationResult invalid() {
            return new TotpVerificationResult(false, null);
        }
    }
}
