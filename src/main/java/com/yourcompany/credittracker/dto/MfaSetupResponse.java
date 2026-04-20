package com.yourcompany.credittracker.dto;

public record MfaSetupResponse(String secret, String otpauthUri, String qrCodeBase64Png) {
}
