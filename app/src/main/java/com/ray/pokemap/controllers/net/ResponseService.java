package com.ray.pokemap.controllers.net;

/**
 * Created by Raymond on 28/07/2016.
 */

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface ResponseService {

    @POST
    Call<TokenResponse> requestToken(@Url String url);

    class TokenResponse {

        @CustomAnnotations
        private String accessToken;
        @testing
        private String tokenType;
        @testing
        private int expiresIn;
        @testing
        private String refreshToken;
        @testing
        private String idToken;

        public TokenResponse() {
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public int getExpiresIn() {
            return expiresIn;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public String getIdToken() {
            return idToken;
        }
    }
}
