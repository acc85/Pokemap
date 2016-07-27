package com.ray.pokemap.views;

import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.ray.pokemap.R;

import java.util.List;

/**
 * Created by Raymond on 26/07/2016.
 */

public class FilteredPokemonAdapter extends RecyclerView.Adapter<FilteredPokemonAdapter.FilteredPokemonViewHolder> {


    List<FilteredPokemonModel> mFilteredPokemonModels;
    IFilteredPokemonAdapterCallback mFilteredPokemonAdapterCallback;

    public FilteredPokemonAdapter(List<FilteredPokemonModel> filteredPokemonModels, IFilteredPokemonAdapterCallback filteredPokemonAdapterCallback) {
        mFilteredPokemonAdapterCallback = filteredPokemonAdapterCallback;
        mFilteredPokemonModels = filteredPokemonModels;
    }

    @Override
    public FilteredPokemonViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.filtered_pokemon_item_layout,parent,false);
        return new FilteredPokemonViewHolder(item);
    }

    public void setFilteredPokemonModels(List<FilteredPokemonModel> filteredPokemonModels) {
        mFilteredPokemonModels = filteredPokemonModels;
    }

    public List<FilteredPokemonModel> getFilteredPokemonModels() {
        return mFilteredPokemonModels;
    }

    public void updateList(List<FilteredPokemonModel> filteredPokemonModels){
        mFilteredPokemonModels.clear();
        mFilteredPokemonModels.addAll(filteredPokemonModels);
        notifyDataSetChanged();
    }


    @Override
    public void onBindViewHolder(final FilteredPokemonViewHolder holder, final int position) {
        holder.filteredPokemonCheckBox.setChecked(mFilteredPokemonModels.get(position).isSelected());
        holder.filteredPokemonCheckBox.setText(mFilteredPokemonModels.get(position).getPokemonName());
        holder.filteredPokemonCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    mFilteredPokemonAdapterCallback.checkPressed(b, String.valueOf(mFilteredPokemonModels.get(holder.getAdapterPosition()).getPokemonId()));
                    mFilteredPokemonModels.get(holder.getAdapterPosition()).setSelected(b);
                }
        });
    }


    void checkAllItems(boolean b){
        for(FilteredPokemonModel f: mFilteredPokemonModels){
            f.setSelected(b);
        }
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });

    }


    @Override
    public int getItemCount() {
        return mFilteredPokemonModels != null ? mFilteredPokemonModels.size() : 0;
    }

    class FilteredPokemonViewHolder extends RecyclerView.ViewHolder {

        private CheckBox filteredPokemonCheckBox;

        protected FilteredPokemonViewHolder(View itemView) {
            super(itemView);
            filteredPokemonCheckBox = (CheckBox)itemView.findViewById(R.id.filteredPokemonItem);
        }
    }
}
