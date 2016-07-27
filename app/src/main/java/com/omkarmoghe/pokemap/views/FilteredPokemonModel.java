package com.omkarmoghe.pokemap.views;

/**
 * Created by Raymond on 26/07/2016.
 */

public class FilteredPokemonModel {

    private String mPokemonName;
    private int mPokemonId;
    private boolean mIsSelected;


    public FilteredPokemonModel(String pokemonName, int pokemonId, boolean isSelected) {
        mPokemonName = pokemonName;
        mPokemonId = pokemonId;
        mIsSelected = isSelected;
    }

    public String getPokemonName() {
        return mPokemonName;
    }

    public void setPokemonName(String pokemonName) {
        mPokemonName = pokemonName;
    }

    public int getPokemonId() {
        return mPokemonId;
    }

    public void setPokemonId(int pokemonId) {
        mPokemonId = pokemonId;
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean selected) {
        mIsSelected = selected;
    }
}
