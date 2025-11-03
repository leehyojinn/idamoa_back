package com.hip.damoa.infra.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class GoogleOAuthProvider implements OAuthProvider {

    @Value("${oauth.google.client-id}")
    private String clientId;

    @Value("${oauth.google.client-secret}")
    private String clientSecret;

    private static final String AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getAuthorizationUrl(String state, String redirectUri) {
        return AUTHORIZATION_URL + "?" +
                "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=openid email profile" +
                "&state=" + state;
    }

    @Override
    public OAuthTokenResponse getAccessToken(String code, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(TOKEN_URL, request, String.class);
            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            return OAuthTokenResponse.builder()
                    .accessToken(jsonNode.get("access_token").asText())
                    .tokenType(jsonNode.get("token_type").asText())
                    .refreshToken(jsonNode.has("refresh_token") ? jsonNode.get("refresh_token").asText() : null)
                    .expiresIn(jsonNode.get("expires_in").asInt())
                    .scope(jsonNode.has("scope") ? jsonNode.get("scope").asText() : null)
                    .build();
        } catch (Exception e) {
            log.error("Failed to get Google access token", e);
            throw new RuntimeException("Failed to get Google access token", e);
        }
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            String providerId = jsonNode.get("id").asText();
            String email = jsonNode.has("email") ? jsonNode.get("email").asText() : null;
            String name = jsonNode.has("name") ? jsonNode.get("name").asText() : null;
            String profileImageUrl = jsonNode.has("picture") ? jsonNode.get("picture").asText() : null;

            return OAuthUserInfo.builder()
                    .providerId(providerId)
                    .email(email)
                    .name(name)
                    .profileImageUrl(profileImageUrl)
                    .provider("GOOGLE")
                    .build();
        } catch (Exception e) {
            log.error("Failed to get Google user info", e);
            throw new RuntimeException("Failed to get Google user info", e);
        }
    }

    @Override
    public String getProviderName() {
        return "GOOGLE";
    }
}
