package com.ray.pokemap.models.events;

import com.google.android.gms.maps.model.Marker;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;

/**
 * Created by Raymond on 25/07/2016.
 */

public class PokemonMarker {

    private Marker mMarker;
    private CatchablePokemon mPokemon;
    private boolean mIsMarked;

    public PokemonMarker(Marker marker, CatchablePokemon pokemon) {
        mMarker = marker;
        mPokemon = pokemon;
        mIsMarked = false;
    }

    public boolean isIsMarked() {
        return mIsMarked;
    }

    public void setIsMarked(boolean mIsMarked) {
        this.mIsMarked = mIsMarked;
    }

    public Marker getMarker() {
        return mMarker;
    }

    public void setMarker(Marker marker) {
        mMarker = marker;
    }

    public CatchablePokemon getPokemon() {
        return mPokemon;
    }

    public void setPokemon(CatchablePokemon pokemon) {
        mPokemon = pokemon;
    }
}
