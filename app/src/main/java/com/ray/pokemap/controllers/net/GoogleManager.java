package com.ray.pokemap.controllers.net;

import android.util.Log;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by chris on 7/21/2016.
 */
public class GoogleManager {
    private static final String TAG = "GoogleManager";

    private static GoogleManager ourInstance = new GoogleManager();

    private static final String BASE_URL = "https://www.google.com";
    private static final String SECRET = "NCjF1TLi2CcY6t5mt0ZveuL7";
    private static final String CLIENT_ID = "848232511240-73ri3t7plvk96pj4f85uj8otdat2alem.apps.googleusercontent.com";
    private static final String OAUTH_TOKEN_ENDPOINT = "https://www.googleapis.com/oauth2/v4/token";
    private static final String OAUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/device/code";
    private static final String AUTH_URL = "https://android.clients.google.com/auth";
    private static final String APP = "com.nianticlabs.pokemongo";


    private final OkHttpClient mClient;
    private final GoogleService mGoogleService;

    private final ResponseService mResponseService;

    public static GoogleManager getInstance() {
        return ourInstance;
    }

    private GoogleManager() {
        mClient = new OkHttpClient.Builder()
                .addInterceptor(new NetworkRequestLoggingInterceptor())
                .followRedirects(false)
                .followSslRedirects(false)
                .build();

        mGoogleService = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(mClient)
                .build()
                .create(GoogleService.class);

        mResponseService = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(CustomFactory.create())
                .client(mClient)
                .build()
                .create(ResponseService.class);
    }

    public void authUser(final LoginListener listener, String token) {
        HttpUrl url = HttpUrl.parse(OAUTH_ENDPOINT).newBuilder()
                .addQueryParameter("client_id", CLIENT_ID)
                .addQueryParameter("scope", "openid email https://www.googleapis.com/auth/userinfo.email")
                .addQueryParameter("Authorization", "OAuth " + token)
                .build();

        Callback<GoogleService.AuthRequest> googleCallback = new Callback<GoogleService.AuthRequest>() {
            @Override
            public void onResponse(Call<GoogleService.AuthRequest> call, Response<GoogleService.AuthRequest> response) {
                GoogleService.AuthRequest body = response.body();

                if (body != null) {
                    listener.authRequested(body);
                } else {
                    Log.e(TAG, "Google login failed while authenticating. response.body() is null.");
                    listener.authFailed("Google login failed while authenticating");
                }
            }

            @Override
            public void onFailure(Call<GoogleService.AuthRequest> call, Throwable t) {
                t.printStackTrace();
                Log.e(TAG, "Google authentication failed when calling  authUser(). googleCallback's onFailure() threw: " + t.getMessage());
                listener.authFailed("Failed on getting the information for the user auth");
            }
        };

        if (mGoogleService != null) {
            Call<GoogleService.AuthRequest> call = mGoogleService.requestAuth(url.toString());
            call.enqueue(googleCallback);
        }
    }


    public String getToken(String email, String password) {
        String service = "ac2dm";
        String device_country = "us";
        String operatorCountry = "us";
        String lang = "en";
        String sdk_version = "17";
        String android_id = "9774d56d682e549c";
//
        String token = "";
        URL url = null;
        try {
            url = new URL("https://android.clients.google.com/auth?"
                    + "accountType=HOSTED_OR_GOOGLE&"
                    + "Email="+email+"&"
                    + "has_permission=1&"
                    + "Passwd="+password+"&"
                    + "service=" + service + "&"
                    + "source=android&"
                    + "androidId=" + android_id + "&"
                    + "device_country=" + device_country + "&"
                    + "operatorCountry=" + device_country + "&"
                    + "lang=" + lang + "&"
                    + "sdk_version=" + sdk_version);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains("Token")) {
                    String[] split = inputLine.split("Token");
                    if (split.length > 0) {
                        token = split[1].replace("=", "");
                    }
                } else if (inputLine.contains("Auth")) {
                    String[] split = inputLine.split("Auth");
                    if (split.length > 0) {
                        String auth = split[1].replace("=", "");
                    }
                }
            }
            in.close();
            urlConnection.disconnect();
            return token;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


    public String getSecondToken(String token) {
        String service = "audience:server:client_id:848232511240-7so421jotr2609rmqakceuu1luuq0ptb.apps.googleusercontent.com";
        String device_country = "us";
        String operatorCountry = "us";
        String lang = "en";
        String sdk_version = "17";
        String android_id = "9774d56d682e549c";
        String client_sig = "321187995bc7cdc2b5fc91b11a96e2baa8602c62";


        String auth = "";
        URL url = null;
        try {
            url = new URL("https://android.clients.google.com/auth?"
                    + "accountType=HOSTED_OR_GOOGLE&"
                    + "Email=randomaccesss2015@gmail.com&"
                    + "has_permission=1&"
                    + "EncryptedPasswd=" + token + "&"
                    + "service=" + service + "&"
                    + "source=android&"
                    + "androidId=" + android_id + "&"
                    + "app=" + APP + "&"
                    + "client_sig=" + client_sig + "&"
                    + "device_country=" + device_country + "&"
                    + "operatorCountry=" + device_country + "&"
                    + "lang=" + lang + "&"
                    + "sdk_version=" + sdk_version);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if(inputLine.contains("Auth")) {
                    String[] split = inputLine.split("Auth");
                    if (split.length > 0) {
                        auth = split[1].replace("=", "");
                    }
                }
            }
            in.close();
            urlConnection.disconnect();
            return auth;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void requestToken(String deviceCode, final LoginListener listener) {
        HttpUrl url = HttpUrl.parse(OAUTH_TOKEN_ENDPOINT).newBuilder()
                .addQueryParameter("client_id", CLIENT_ID)
                .addQueryParameter("client_secret", SECRET)
                .addQueryParameter("code", deviceCode)
                .addQueryParameter("grant_type", "http://oauth.net/grant_type/device/1.0")
                .addQueryParameter("scope", "openid email https://www.googleapis.com/auth/userinfo.email")
                .build();

        Callback<GoogleService.TokenResponse> googleCallback = new Callback<GoogleService.TokenResponse>() {
            @Override
            public void onResponse(Call<GoogleService.TokenResponse> call, Response<GoogleService.TokenResponse> response) {

                if (response.body() != null) {
                    listener.authSuccessful(response.body().getIdToken(), response.body().getRefreshToken());
                } else {
                    Log.e(TAG, "Google login failed while fetching token. response.body() is null.");
                    listener.authFailed("Google login failed while authenticating. Token missing.");
                }
            }

            @Override
            public void onFailure(Call<GoogleService.TokenResponse> call, Throwable t) {
                t.printStackTrace();
                Log.e(TAG, "Google authentication failed while fetching request token using requestToken(). googleCallback's onFailure() threw: " + t.getMessage());
                listener.authFailed("Failed on requesting the id token");
            }
        };

        if (mGoogleService != null) {
            Call<GoogleService.TokenResponse> call = mGoogleService.requestToken(url.toString());
            call.enqueue(googleCallback);
        }
    }

    public void refreshToken(String refreshToken, final RefreshListener listener) {
        HttpUrl url = HttpUrl.parse(OAUTH_TOKEN_ENDPOINT).newBuilder()
                .addQueryParameter("client_id", CLIENT_ID)
                .addQueryParameter("client_secret", SECRET)
                .addQueryParameter("refresh_token", refreshToken)
                .addQueryParameter("grant_type", "refresh_token")
                .build();


        Callback<GoogleService.TokenResponse> googleCallback = new Callback<GoogleService.TokenResponse>() {
            @Override
            public void onResponse(Call<GoogleService.TokenResponse> call, Response<GoogleService.TokenResponse> response) {
                if (response != null && response.body() != null) {
                    listener.refreshSuccessful(response.body().getIdToken(), response.body().getRefreshToken());
                } else {
                    listener.refreshFailed("Failed on requesting the id token");
                }
            }

            @Override
            public void onFailure(Call<GoogleService.TokenResponse> call, Throwable t) {
                t.printStackTrace();
                listener.refreshFailed("Failed on requesting the id token");
            }
        };

        if (mGoogleService != null) {
            Call<GoogleService.TokenResponse> call = mGoogleService.requestToken(url.toString());
            call.enqueue(googleCallback);
        }
    }

    public interface LoginListener {
        void authSuccessful(String authToken, String refreshToken);

        void authFailed(String message);

        void authRequested(GoogleService.AuthRequest body);
    }

    public interface RefreshListener {
        void refreshSuccessful(String authToken, String refreshToken);

        void refreshFailed(String message);
    }
}