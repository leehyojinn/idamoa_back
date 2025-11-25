package com.hip.damoa.config.analytics;

import com.google.analytics.data.v1beta.BetaAnalyticsDataClient;
import com.google.analytics.data.v1beta.BetaAnalyticsDataSettings;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Google Analytics Data API 설정
 * google.analytics.enabled=true로 설정해야 활성화됩니다.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "google.analytics.enabled", havingValue = "true", matchIfMissing = false)
public class GoogleAnalyticsConfig {

    @Value("${google.analytics.credentials-path}")
    private String credentialsPath;

    @Value("${google.analytics.property-id}")
    private String propertyId;

    @Bean
    public BetaAnalyticsDataClient analyticsDataClient() throws IOException {
        log.info("Google Analytics Data API 초기화 시작: credentialsPath={}", credentialsPath);

        // 파일 존재 확인
        File credentialsFile = new File(credentialsPath);
        if (!credentialsFile.exists()) {
            throw new IllegalStateException(
                    "Google Analytics Service Account 파일을 찾을 수 없습니다: " + credentialsPath);
        }

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(credentialsPath))
                .createScoped("https://www.googleapis.com/auth/analytics.readonly");

        BetaAnalyticsDataSettings settings = BetaAnalyticsDataSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();

        BetaAnalyticsDataClient client = BetaAnalyticsDataClient.create(settings);

        log.info("Google Analytics Data API 초기화 완료: propertyId={}", propertyId);

        return client;
    }

    @Bean
    public String analyticsPropertyId() {
        return propertyId;
    }
}
