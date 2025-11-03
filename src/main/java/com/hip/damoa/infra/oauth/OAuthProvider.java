package com.hip.damoa.infra.oauth;

public interface OAuthProvider {
    String getAuthorizationUrl(String state, String redirectUri);
    OAuthTokenResponse getAccessToken(String code, String redirectUri);
    OAuthUserInfo getUserInfo(String accessToken);
    String getProviderName();
}
