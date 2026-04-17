package com.yourcompany.credittracker.config;

import com.yourcompany.credittracker.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {
    private final AdminUserService adminUserService;

    @Override
    public void run(ApplicationArguments args) {
        adminUserService.bootstrapAdmins();
    }
}
