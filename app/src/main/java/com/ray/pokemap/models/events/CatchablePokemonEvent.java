package com.ray.pokemap.models.events;

import com.pokegoapi.api.map.pokemon.CatchablePokemon;

import java.util.Map;

/**
 * Created by Jon on 7/23/2016.
 */
public class CatchablePokemonEvent implements IEvent {

    private Map<String,CatchablePokemon> catchablePokemon;

    public CatchablePokemonEvent(Map<String, CatchablePokemon> catchablePokemon) {
        this.catchablePokemon = catchablePokemon;
    }

    public Map<String, CatchablePokemon> getCatchablePokemon() {
        return catchablePokemon;
    }

    public void setCatchablePokemon(Map<String, CatchablePokemon> catchablePokemon) {
        this.catchablePokemon = catchablePokemon;
    }
}
