package com.yourcompany.credittracker.dto;

import java.util.List;

public record MfaConfirmResponse(boolean success, List<String> backupCodes) {
}
