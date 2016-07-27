package com.omkarmoghe.pokemap.views;

/**
 * Created by Raymond on 26/07/2016.
 */

public interface IFilteredPokemonAdapterCallback {

    void checkPressed(boolean isChecked, String pokemonId);

    void checkAll();

    void unCheckAll();
}
