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
public class KakaoOAuthProvider implements OAuthProvider {

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.client-secret}")
    private String clientSecret;

    private static final String AUTHORIZATION_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getAuthorizationUrl(String state, String redirectUri) {
        return AUTHORIZATION_URL + "?" +
                "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
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
                    .build();
        } catch (Exception e) {
            log.error("Failed to get Kakao access token", e);
            throw new RuntimeException("Failed to get Kakao access token", e);
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

            JsonNode kakaoAccount = jsonNode.get("kakao_account");
            String email = kakaoAccount.has("email") ? kakaoAccount.get("email").asText() : null;

            // profile이 null일 수 있으므로 null 체크
            JsonNode profile = kakaoAccount.has("profile") ? kakaoAccount.get("profile") : null;
            String name = (profile != null && profile.has("nickname")) ? profile.get("nickname").asText() : null;
            String profileImageUrl = (profile != null && profile.has("profile_image_url")) ?
                    profile.get("profile_image_url").asText() : null;

            return OAuthUserInfo.builder()
                    .providerId(providerId)
                    .email(email)
                    .name(name)
                    .profileImageUrl(profileImageUrl)
                    .provider("KAKAO")
                    .build();
        } catch (Exception e) {
            log.error("Failed to get Kakao user info", e);
            throw new RuntimeException("Failed to get Kakao user info", e);
        }
    }

    @Override
    public String getProviderName() {
        return "KAKAO";
    }
}
