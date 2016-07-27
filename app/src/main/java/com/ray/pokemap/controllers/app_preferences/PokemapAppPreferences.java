package com.ray.pokemap.controllers.app_preferences;

import android.support.annotation.NonNull;

import java.util.Set;

/**
 * A contract which defines a user's app preferences
 */
public interface PokemapAppPreferences {
    /**
     * @return true if the username has been set
     */
    boolean isUsernameSet();

    /**
     * @return true if password has been set
     */
    boolean isPasswordSet();

    /**
     * @return the username stored or an empty @see java.lang.String
     */
    String getUsername();

    /**
     * @param username that should be set
     */
    void setUsername(@NonNull String username);

    /**
     * @param password that should be set
     */
    void setPassword(@NonNull String password);

    /**
     * @return the password stored or an empty @see java.lang.String
     */
    String getPassword();

    /**
     * @param rememberme that should be set
     */
    void setRememberMe(boolean rememberme);

    /**
     * @return the password stored or an empty @see java.lang.String
     */
    boolean rememberMe();

    int getRememberMeLoginType();

    void setRememberMeLoginType(int rememberMeLoginType);

    String getGoogleAuthToken();

    void setGoogleAuthToken(String authToken);

    Set<String> getFilteredPokemon();

    void setFilteredPokemon(Set<String> filteredPokemon);

    int getSteps();

    void setSteps(int steps);

    int getMapSelectionType();

    void setMapSelectionType(int type);

    /**
     *
     * @param isEnabled Sets if the background service is enabled.
     */
    void setServiceState(@NonNull boolean isEnabled);

    /**
     *
     * @return Returns service state as set in preffs
     */
    boolean isServiceEnabled();

    int getServiceRefreshRate();

    boolean getShowPokestops();
    boolean getShowGyms();

    int getTrackingType();

    void setTrackingType(int trackingType);

}
