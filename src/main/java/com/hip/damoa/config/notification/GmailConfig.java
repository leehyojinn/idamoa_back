package com.hip.damoa.config.notification;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Gmail API 설정
 * Service Account + Domain-wide Delegation 방식
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "notification.email.gmail-api.enabled", havingValue = "true")
public class GmailConfig {

    @Value("${notification.email.gmail-api.service-account-key-path}")
    private String keyPath;

    @Value("${notification.email.gmail-api.delegated-user}")
    private String delegatedUser;

    @Value("${notification.email.gmail-api.application-name}")
    private String applicationName;

    /**
     * Gmail 클라이언트 빈 생성
     * Service Account를 사용하여 Domain-wide Delegation 적용
     */
    @Bean
    public Gmail gmail() throws IOException, GeneralSecurityException {
        log.info("Gmail API 클라이언트 초기화 시작: delegatedUser={}, appName={}",
                delegatedUser, applicationName);

        try {
            // Service Account 키 파일 경로 처리
            String actualPath = keyPath.replace("file:", "");
            log.debug("Service Account 키 파일 경로: {}", actualPath);

            try (InputStream in = new FileInputStream(actualPath)) {
                // Service Account 인증 정보 로드
                GoogleCredentials baseCredentials = GoogleCredentials.fromStream(in)
                        .createScoped(List.of(GmailScopes.GMAIL_SEND));

                // Domain-wide Delegation 적용 (위임할 사용자 지정)
                GoogleCredentials delegatedCredentials = baseCredentials.createDelegated(delegatedUser);

                // Gmail 클라이언트 생성
                Gmail client = new Gmail.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        new HttpCredentialsAdapter(delegatedCredentials)
                ).setApplicationName(applicationName).build();

                log.info("Gmail API 클라이언트 초기화 완료");
                return client;
            }
        } catch (IOException e) {
            log.error("Gmail API 초기화 실패: Service Account 키 파일을 읽을 수 없습니다. path={}", keyPath, e);
            throw new IOException("Service Account 키 파일을 읽을 수 없습니다: " + keyPath, e);
        } catch (GeneralSecurityException e) {
            log.error("Gmail API 초기화 실패: 보안 설정 오류", e);
            throw e;
        }
    }
}
