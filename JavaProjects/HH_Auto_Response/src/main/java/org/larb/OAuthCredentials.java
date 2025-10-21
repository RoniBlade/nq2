package org.larb;

public class OAuthCredentials {
    String clientId;
    String clientSecret;
    String redirectUri;
    String authCode;
    String accessToken;
    String refreshToken;
    Long expiresIn;

    public OAuthCredentials(String clientId, String clientSecret, String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }
}
