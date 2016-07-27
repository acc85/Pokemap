package com.omkarmoghe.pokemap.models.events;

import com.google.android.gms.maps.model.Marker;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.pokemon.Pokemon;

/**
 * Created by Raymond on 25/07/2016.
 */

public class PokemonMarker {

    private Marker mMarker;
    private CatchablePokemon mPokemon;

    public PokemonMarker(Marker marker, CatchablePokemon pokemon) {
        mMarker = marker;
        mPokemon = pokemon;
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
