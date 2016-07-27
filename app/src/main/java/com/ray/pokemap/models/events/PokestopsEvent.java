package com.ray.pokemap.models.events;

import com.pokegoapi.api.map.fort.Pokestop;

import java.util.Map;

/**
 * Created by socrates on 7/23/2016.
 */
public class PokestopsEvent implements IEvent {

    private Map<String,Pokestop> pokestops;

    public PokestopsEvent(Map<String, Pokestop> catchablePokemon) {
        this.pokestops = catchablePokemon;
    }

    public Map<String, Pokestop> getPokestops() {
        return pokestops;
    }

    public void setPokestops(Map<String, Pokestop> pokestops) {
        this.pokestops = pokestops;
    }
}
