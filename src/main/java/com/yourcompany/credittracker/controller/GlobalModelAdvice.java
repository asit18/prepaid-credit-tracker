package com.yourcompany.credittracker.controller;

import com.yourcompany.credittracker.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {
    private final AppSettingService appSettingService;

    @ModelAttribute("businessName")
    String businessName() {
        return appSettingService.businessName();
    }
}
