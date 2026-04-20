package com.yourcompany.credittracker.dto;

public record LoginResponse(boolean requiresMfa, String tempToken, String accessToken) {
    public static LoginResponse mfaRequired(String tempToken) {
        return new LoginResponse(true, tempToken, null);
    }

    public static LoginResponse authenticated(String accessToken) {
        return new LoginResponse(false, null, accessToken);
    }
}
