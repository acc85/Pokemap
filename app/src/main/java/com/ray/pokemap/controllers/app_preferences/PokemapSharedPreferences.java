package com.ray.pokemap.controllers.app_preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Provide convenience methods to access shared preferences
 */

public final class PokemapSharedPreferences implements PokemapAppPreferences {
    private static final String USERNAME_KEY = "UsernameKey";
    private static final String PASSWORD_KEY = "PasswordKey";
    private static final String FILTERED_POKEMON_KEY = "FilteredPokemonKey";
    private static final String STEPS_KEY = "StepsKey";
    private static final String MAP_TYPE_KEY = "MapTypeKey";
    private static final String REMEMBER_ME = "RememberMe";
    private static final String REMEMBER_ME_LOGIN_TYPE = "RememberMeLoginType";
    private static final String GOOGLE_AUTH_TOKEN = "GoogleAuthToken";
    private static final String SERVICE_KEY = "background_poke_service";
    private static final String SERVICE_REFRESH_KEY = "service_refresh_rate";
    private static final String SHOW_POKESTOPS = "show_pokestops";
    private static final String SHOW_GYMS = "show_gyms";

    public static final int PTC = 0;
    public static final int GOOGLE = 1;
    public static final int FOLLOW_TRACKING =1;
    public static final int LOCATION_TRACKING = 0;
    private static final String TRACKING_TYPE = "tracking_type";

    private final SharedPreferences sharedPreferences;

    public PokemapSharedPreferences(@NonNull Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public boolean isUsernameSet() {
        return sharedPreferences.contains(USERNAME_KEY);
    }

    @Override
    public boolean isPasswordSet() {
        return sharedPreferences.contains(PASSWORD_KEY);
    }


    @Override
    public String getUsername() {
        return sharedPreferences.getString(USERNAME_KEY, "");
    }

    @Override
    public void setUsername(@NonNull String username) {
        sharedPreferences.edit().putString(USERNAME_KEY, username).apply();
    }

    @Override
    public void setPassword(@NonNull String password) {
        sharedPreferences.edit().putString(PASSWORD_KEY, password).apply();
    }

    @Override
    public String getPassword() {
        return sharedPreferences.getString(PASSWORD_KEY, "");
    }

    @Override
    public void setRememberMe(boolean rememberME) {
        sharedPreferences.edit().putBoolean(REMEMBER_ME, rememberME).apply();
    }

    @Override
    public int getRememberMeLoginType() {
        return sharedPreferences.getInt(REMEMBER_ME_LOGIN_TYPE, 0);
    }

    @Override
    public void setRememberMeLoginType(int rememberMeLoginType) {
        sharedPreferences.edit().putInt(REMEMBER_ME_LOGIN_TYPE, rememberMeLoginType).apply();
    }

    @Override
    public boolean rememberMe() {
        return sharedPreferences.getBoolean(REMEMBER_ME, false);
    }

    public Set<String> getFilteredPokemon(){
        return sharedPreferences.getStringSet(FILTERED_POKEMON_KEY,new HashSet<String>());
    }


    public void addPokemonToFilteredList(String id){
        Set<String> filter = sharedPreferences.getStringSet(FILTERED_POKEMON_KEY,new HashSet<String>());
        filter.add(id);
        setFilteredPokemon(filter);
    }

    public String getGoogleAuthToken(){
        return sharedPreferences.getString(GOOGLE_AUTH_TOKEN,"");
    };

    public void setGoogleAuthToken(String authToken){
        sharedPreferences.edit().putString(GOOGLE_AUTH_TOKEN, authToken).apply();
    };

    @Override
    public void setServiceState(@NonNull boolean isEnabled) {
        sharedPreferences.edit().putBoolean(SERVICE_KEY, isEnabled).apply();
    }


    public void setFilteredPokemon(Set<String> filteredPokemon){
        sharedPreferences.edit().putStringSet(FILTERED_POKEMON_KEY, filteredPokemon).apply();
    }

    public int getSteps(){
        return sharedPreferences.getInt(STEPS_KEY,10);
    }

    public void setSteps(int steps){
        sharedPreferences.edit().putInt(STEPS_KEY, steps).apply();
    }


    public int getMapSelectionType(){
        return sharedPreferences.getInt(MAP_TYPE_KEY,0);

    };

    public void setMapSelectionType(int type){
        sharedPreferences.edit().putInt(MAP_TYPE_KEY, type).apply();
    };

    @Override
    public boolean isServiceEnabled() {
        return sharedPreferences.getBoolean(SERVICE_KEY, false);
    }

    @Override
    public int getServiceRefreshRate() {
        return Integer.valueOf(sharedPreferences.getString(SERVICE_REFRESH_KEY, "60"));
    }

    @Override
    public boolean getShowPokestops() {
        return sharedPreferences.getBoolean(SHOW_POKESTOPS, false);
    }

    @Override
    public boolean getShowGyms() {
        return sharedPreferences.getBoolean(SHOW_GYMS, false);
    }

    @Override
    public int getTrackingType() {
        return sharedPreferences.getInt(TRACKING_TYPE, LOCATION_TRACKING);
    }

    @Override
    public void setTrackingType(int trackingType) {
        sharedPreferences.edit().putInt(TRACKING_TYPE, trackingType).apply();
    }
}
