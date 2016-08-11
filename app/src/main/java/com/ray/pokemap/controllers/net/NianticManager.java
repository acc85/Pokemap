package com.ray.pokemap.controllers.net;

import android.os.HandlerThread;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pokegoapi.auth.CredentialProvider;
import com.ray.pokemap.models.events.CatchablePokemonEvent;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.ray.pokemap.models.events.LoginEventResult;
import com.ray.pokemap.models.events.LoginFailedEvent;
import com.ray.pokemap.models.events.PokeStopsEvent;
import com.ray.pokemap.models.events.RetryEvent;
import com.ray.pokemap.models.events.ServerUnreachableEvent;
import com.ray.pokemap.models.events.TokenExpiredEvent;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.auth.GoogleAutoCredentialProvider;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.ray.pokemap.models.login.GoogleLoginInfo;
import com.ray.pokemap.models.login.LoginInfo;
import com.ray.pokemap.views.GoogleLoginEvent;
import com.ray.pokemap.views.settings.AutoLoggingFailedEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.pokegoapi.auth.PtcCredentialProvider.CLIENT_ID;
import static com.pokegoapi.auth.PtcCredentialProvider.CLIENT_SECRET;
import static com.pokegoapi.auth.PtcCredentialProvider.LOGIN_OAUTH;
import static com.pokegoapi.auth.PtcCredentialProvider.LOGIN_URL;
import static com.pokegoapi.auth.PtcCredentialProvider.REDIRECT_URI;

/**
 * Created by vanshilshah on 20/07/16.
 */
public class NianticManager {
    private static final String TAG = "NianticManager";

    private static final String BASE_URL = "https://sso.pokemon.com/sso/";

    private static final NianticManager instance = new NianticManager();
    public boolean pokemonCatcherStarted;

    private Handler mHandler;
    private RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo mAuthInfo;
    private NianticService mNianticService;
    private final OkHttpClient mClient;
    private final OkHttpClient mPoGoClient;
    private PokemonGo mPokemonGo;
    private String AuthURL = "https://android.clients.google.com/auth";
    private String SERVICE = "audience:server:client_id:848232511240-7so421jotr2609rmqakceuu1luuq0ptb.apps.googleusercontent.com";
    private String CLIENT_SIG = "321187995bc7cdc2b5fc91b11a96e2baa8602c62";



    public static NianticManager getInstance(){
        return instance;
    }

    private NianticManager(){
        mPoGoClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        HandlerThread thread = new HandlerThread("Niantic Manager Thread");
        thread.start();
        mHandler = new Handler(thread.getLooper());

                  /*
		This is a temporary, in-memory cookie jar.
		We don't require any persistence outside of the scope of the login,
		so it being discarded is completely fine
		*/
        CookieJar tempJar = new CookieJar() {
            private final HashMap<String, List<Cookie>> cookieStore = new HashMap<String, List<Cookie>>();

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                cookieStore.put(url.host(), cookies);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> cookies = cookieStore.get(url.host());
                return cookies != null ? cookies : new ArrayList<Cookie>();
            }
        };

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        mClient = new OkHttpClient.Builder()
                .cookieJar(tempJar)
                .addInterceptor(new NetworkRequestLoggingInterceptor())
                .build();

        mNianticService = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(mClient)
                .build()
                .create(NianticService.class);
    }


//    public void login(final String username, final String password, final LoginListener loginListener){
//        Callback<NianticService.LoginValues> valuesCallback = new Callback<NianticService.LoginValues>() {
//            @Override
//            public void onResponse(Call<NianticService.LoginValues> call, Response<NianticService.LoginValues> response) {
//                if(response.body() != null) {
//                    loginPTC(username, password, response.body(), loginListener);
//                }else{
//                    Log.d(TAG, "onFailure: "+response);
//                    loginListener.authFailed("Fetching Pokemon Trainer Club's Login Url Values Failed");
//                }
//
//            }
//
//            @Override
//            public void onFailure(Call<NianticService.LoginValues> call, Throwable t) {
//                Log.d(TAG, "onFailure: "+call);
//                loginListener.authFailed("Fetching Pokemon Trainer Club's Login Url Values Failed");
//            }
//        };
//        Call<NianticService.LoginValues> call = mNianticService.getLoginValues();
//        call.enqueue(valuesCallback);
//    }

//    private void loginPTC(final String username, final String password, NianticService.LoginValues values, final LoginListener loginListener){
//        HttpUrl url = HttpUrl.parse(LOGIN_URL).newBuilder()
//                .addQueryParameter("lt", values.getLt())
//                .addQueryParameter("execution", values.getExecution())
//                .addQueryParameter("_eventId", "submit")
//                .addQueryParameter("username", username)
//                .addQueryParameter("password", password)
//                .build();
//
//        OkHttpClient client = mClient.newBuilder()
//                .followRedirects(false)
//                .followSslRedirects(false)
//                .build();
//
//        NianticService service = new Retrofit.Builder()
//                .baseUrl(BASE_URL)
//                .addConverterFactory(GsonConverterFactory.create())
//                .client(client)
//                .build()
//                .create(NianticService.class);
//
//        Callback<NianticService.LoginResponse> loginCallback = new Callback<NianticService.LoginResponse>() {
//            @Override
//            public void onResponse(Call<NianticService.LoginResponse> call, Response<NianticService.LoginResponse> response) {
//                Log.d(TAG,"body:"+call.request());
//                if(response.headers().get("location") == null){
//                    loginListener.authFailed("No location with response, try again");
//                }else {
//                    String location = response.headers().get("location");
//                    String ticket = location.split("ticket=")[1];
//                    requestToken(ticket, loginListener);
//                }
//            }
//
//            @Override
//            public void onFailure(Call<NianticService.LoginResponse> call, Throwable t) {
//                loginListener.authFailed("Pokemon Trainer Club Login Failed");
//            }
//        };
//        Call<NianticService.LoginResponse> call = service.login(url.toString());
//        call.enqueue(loginCallback);
//    }

//    private void requestToken(String code, final LoginListener loginListener){
//        Log.d(TAG, "requestToken() called with: code = [" + code + "]");
//        HttpUrl url = HttpUrl.parse(LOGIN_OAUTH).newBuilder()
//                .addQueryParameter("client_id", CLIENT_ID)
//                .addQueryParameter("redirect_uri", REDIRECT_URI)
//                .addQueryParameter("client_secret", CLIENT_SECRET)
//                .addQueryParameter("grant_type", "refresh_token")
//                .addQueryParameter("code", code)
//                .build();
//
//        Callback<ResponseBody> authCallback = new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                try {
//                    String token = response.body().string().split("token=")[1];
//                    token = token.split("&")[0];
//                    loginListener.authSuccessful(token);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    loginListener.authFailed("Pokemon Trainer Club Authentication Failed");
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                t.printStackTrace();
//                loginListener.authFailed("Pokemon Trainer Club Authentication Failed");
//            }
//        };
//        Call<ResponseBody> call = mNianticService.requestToken(url.toString());
//        call.enqueue(authCallback);
//    }

    public interface LoginListener {
        void authSuccessful(PtcCredentialProvider ptcCredentialProvider);
        void authFailed(String message);
    }

//    /**
//     * Sets the google auth token for the auth info also invokes the onLogin callback.
//     * @param token - a valid google auth token.
//     */


    /**
     * Sets the google auth token for the auth info also invokes the onLogin callback.
     * @param username -  username of the google account
     * @param password - password of the google account
     * @param autoLogging -  boolean to dertmine is auto logging or not
     */

    public void setGoogleAuthToken(@NonNull final String username, final String password, final boolean autoLogging) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    GoogleAutoCredentialProvider googleAutoCredentialProvider = new GoogleAutoCredentialProvider(mPoGoClient,username,password);
                    mPokemonGo = new PokemonGo(googleAutoCredentialProvider, mPoGoClient);
                    mAuthInfo = mPokemonGo.getAuthInfo();
//                    EventBus.getDefault().post(new LoginEventResult(true, mAuthInfo, mPokemonGo));
                    EventBus.getDefault().post(new GoogleLoginEvent(mAuthInfo,autoLogging));
                } catch (LoginFailedException e) {
                    e.printStackTrace();
                    if(autoLogging){
                        EventBus.getDefault().post(new AutoLoggingFailedEvent());
                    }else {
                        EventBus.getDefault().post(new LoginFailedEvent("Failed to log in"));
                    }
                } catch (RemoteServerException | NullPointerException e) {
                    EventBus.getDefault().post(new RetryEvent());
                }
            }
        });
    }



//    /**
//     * Sets the pokemon trainer club auth token for the auth info also invokes the onLogin callback.
//     * @param token - a valid pokemon trainer club auth token.
//     */
    public void setPTCAuthToken(final String username, final String password) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    PtcCredentialProvider ptcCredentialProvider = new PtcCredentialProvider(mPoGoClient,username, password);
//                    mAuthInfo = new PtcLogin(mPoGoClient).login(token);
                    mPokemonGo = new PokemonGo(ptcCredentialProvider,mPoGoClient);
                    mAuthInfo = mPokemonGo.getAuthInfo();
                    EventBus.getDefault().post(new LoginEventResult(true, mAuthInfo, mPokemonGo));
                } catch (LoginFailedException | RemoteServerException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void login(@NonNull final String username, @NonNull final String password) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    PtcCredentialProvider ptcCredentialProvider = new PtcCredentialProvider(mPoGoClient,username, password);
//                    mAuthInfo = new PtcLogin(mPoGoClient).login(username, password);
                    mPokemonGo = new PokemonGo(ptcCredentialProvider, mPoGoClient);
                    EventBus.getDefault().post(new LoginEventResult(true, mAuthInfo, mPokemonGo));
                } catch (LoginFailedException | NullPointerException e) {
                    EventBus.getDefault().post(new LoginEventResult(false, null, null));
                } catch (RemoteServerException e) {
                    EventBus.getDefault().post(new ServerUnreachableEvent(e));
                }
            }
        });
    }

    public void getMapInformation(final double lat, final double longitude, final double alt){
        pokemonCatcherStarted = true;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mPokemonGo.setLocation(lat, longitude, alt);
                    Map<String, CatchablePokemon> pokemonMapTable = new HashMap<String, CatchablePokemon>();
                    for(CatchablePokemon c: mPokemonGo.getMap().getCatchablePokemon()){
                        pokemonMapTable.put(c.getSpawnPointId(),c);
                    }

//                    final List<Pokestop> pokestops = new ArrayList<Pokestop>();
//                    pokestops.addAll(mPokemonGo.getMap().getMapObjects().getPokestops());
//                            HandlerThread thread = new HandlerThread("Niantic Manager Second Thread");
//                    thread.start();
//                    new Handler(thread.getLooper()).post(new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//
//                                Map<String, Pokestop> pokeStopsMapTable = new HashMap<String, Pokestop>();
//                                for(Pokestop p: pokestops){
//                                    pokeStopsMapTable.put(p.getDetails().getId(),p);
//                                }
//                                EventBus.getDefault().post(new PokeStopsEvent(pokeStopsMapTable));
//                                pokestops.clear();
//                            } catch (LoginFailedException e) {
//                                EventBus.getDefault().post(new TokenExpiredEvent()); //Because we aren't coming from a log in event, the token must have expired.
//                            } catch (RemoteServerException e) {
//                                EventBus.getDefault().post(new ServerUnreachableEvent(e));
//                            } catch(NullPointerException npe){
//                                EventBus.getDefault().post(new PokeStopsEvent(null));
//                            }
//                        }
//                    });
//                    for(Pokestop p: mPokemonGo.getMap().getMapObjects().getPokestops()){
//                        pokeStopsMapTable.put(p.getDetails().getId(),p);
//                    }
//                    for(Pokestop p: mPokemonGo.getMap().getMapObjects().getPokestops()){
//                        pokeStopsMapTable.put(p.getDetails().getId(),p);
//                    }

                    EventBus.getDefault().post(new CatchablePokemonEvent(pokemonMapTable));
//                    System.out.println("event");
//                    EventBus.getDefault().post(new PokestopsEvent(pokeStopsMapTable));
                } catch (LoginFailedException e) {
                    EventBus.getDefault().post(new TokenExpiredEvent()); //Because we aren't coming from a log in event, the token must have expired.
                } catch (RemoteServerException e) {
                    EventBus.getDefault().post(new ServerUnreachableEvent(e));
                } catch(NullPointerException npe){
                    EventBus.getDefault().post(new CatchablePokemonEvent(null));
                }
            }
        });


//        new Handler().post(new Thread() {
//            @Override
//            public void run() {
//                try {
//
//                    Map<String, Pokestop> pokeStopsMapTable = new HashMap<String, Pokestop>();
//                    List<Pokestop> pokestops = new ArrayList<Pokestop>();
//                    pokestops.addAll(mPokemonGo.getMap().getMapObjects().getPokestops());
//                    for(Pokestop p: pokestops){
//                        pokeStopsMapTable.put(p.getDetails().getId(),p);
//                    }
//                    EventBus.getDefault().post(new PokeStopsEvent(pokeStopsMapTable));
//                    pokestops.clear();
//                } catch (LoginFailedException e) {
//                    EventBus.getDefault().post(new TokenExpiredEvent()); //Because we aren't coming from a log in event, the token must have expired.
//                } catch (RemoteServerException e) {
//                    EventBus.getDefault().post(new ServerUnreachableEvent(e));
//                } catch(NullPointerException npe){
//                    EventBus.getDefault().post(new PokeStopsEvent(null));
//                }
//            }
//        });
    }

}
