package com.ray.pokemap.models.events;

public class PokeObjectsEvent implements IEvent {

    private CatchablePokemonEvent mCatchablePokemonEvent;
    private PokeStopsEvent mPokeStopsEvent;

    public PokeObjectsEvent(CatchablePokemonEvent catchablePokemonEvent, PokeStopsEvent pokeStopsEvent) {
        mCatchablePokemonEvent = catchablePokemonEvent;
        mPokeStopsEvent = pokeStopsEvent;
    }

    public CatchablePokemonEvent getCatchablePokemonEvent() {
        return mCatchablePokemonEvent;
    }

    public void setCatchablePokemonEvent(CatchablePokemonEvent catchablePokemonEvent) {
        mCatchablePokemonEvent = catchablePokemonEvent;
    }

    public PokeStopsEvent getPokeStopEvent() {
        return mPokeStopsEvent;
    }

    public void setPokeStopsEvent(PokeStopsEvent pokeStopsEvent) {
        mPokeStopsEvent = pokeStopsEvent;

    }
}