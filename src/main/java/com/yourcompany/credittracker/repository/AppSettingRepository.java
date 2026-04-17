package com.yourcompany.credittracker.repository;

import com.yourcompany.credittracker.model.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
