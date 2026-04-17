package com.yourcompany.credittracker.service;

import com.yourcompany.credittracker.model.AppSetting;
import com.yourcompany.credittracker.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppSettingService {
    public static final String BUSINESS_NAME_KEY = "business_name";
    private static final String DEFAULT_BUSINESS_NAME = "Prepaid Credit Tracker";

    private final AppSettingRepository appSettingRepository;

    @Transactional(readOnly = true)
    public String businessName() {
        return appSettingRepository.findById(BUSINESS_NAME_KEY)
                .map(AppSetting::getValue)
                .filter(value -> !value.isBlank())
                .orElse(DEFAULT_BUSINESS_NAME);
    }

    @Transactional
    public void updateBusinessName(String businessName) {
        String normalized = businessName == null ? "" : businessName.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Business name is required");
        }
        AppSetting setting = appSettingRepository.findById(BUSINESS_NAME_KEY).orElseGet(() -> {
            AppSetting created = new AppSetting();
            created.setKey(BUSINESS_NAME_KEY);
            return created;
        });
        setting.setValue(normalized);
        appSettingRepository.save(setting);
    }
}
