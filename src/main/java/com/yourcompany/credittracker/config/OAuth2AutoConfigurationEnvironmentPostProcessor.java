package com.yourcompany.credittracker.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class OAuth2AutoConfigurationEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final String PROPERTY_SOURCE_NAME = "creditTrackerOAuth2AutoConfiguration";
    private static final String OAUTH2_CLIENT_AUTO_CONFIGURATION =
            "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean googleEnabled = environment.getProperty("app.auth.google-enabled", Boolean.class, false);
        if (googleEnabled) {
            return;
        }

        String existingExcludes = environment.getProperty("spring.autoconfigure.exclude", "");
        String excludes = appendExclude(existingExcludes);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.autoconfigure.exclude", excludes);
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    private String appendExclude(String existingExcludes) {
        if (!StringUtils.hasText(existingExcludes)) {
            return OAUTH2_CLIENT_AUTO_CONFIGURATION;
        }
        if (existingExcludes.contains(OAUTH2_CLIENT_AUTO_CONFIGURATION)) {
            return existingExcludes;
        }
        return existingExcludes + "," + OAUTH2_CLIENT_AUTO_CONFIGURATION;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
