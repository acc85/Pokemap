package com.ray.pokemap.views;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.GoogleMap;
import com.ray.pokemap.R;
import com.ray.pokemap.models.events.LoginEventResult;
import com.ray.pokemap.models.events.SearchInPosition;
import com.ray.pokemap.models.events.ServerUnreachableEvent;
import com.ray.pokemap.models.events.TokenExpiredEvent;
import com.ray.pokemap.util.PokemonIdUtils;
import com.ray.pokemap.views.login.RequestCredentialsDialogFragment;
import com.ray.pokemap.views.map.MapWrapperFragment;
import com.ray.pokemap.views.settings.SettingsActivity;
import com.ray.pokemap.controllers.app_preferences.PokemapAppPreferences;
import com.ray.pokemap.controllers.app_preferences.PokemapSharedPreferences;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import POGOProtos.Enums.PokemonIdOuterClass;

import static POGOProtos.Enums.PokemonIdOuterClass.PokemonId.MISSINGNO;

public class MainActivity extends BaseActivity implements PlaceSelectionListener {
    private static final String TAG = "Pokemap";

    private PokemapAppPreferences pref;
    private PlaceAutocompleteFragment autocompleteFragment;

    //region Lifecycle Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        pref = new PokemapSharedPreferences(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_fragment);
        autocompleteFragment.setOnPlaceSelectedListener(this);
        autocompleteFragment.setHint("Search a Location");

        if(pref.getTrackingType() == PokemapSharedPreferences.FOLLOW_TRACKING){
            hideAutoCompleteView();
        }
        if(getSupportFragmentManager().findFragmentByTag(MapWrapperFragment.class.getName()) == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.main_container, MapWrapperFragment.newInstance(), MapWrapperFragment.class.getName())
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    //region Menu Methods
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_rescan_from_current_position:
                rescanCurrentPosition();
                break;
            case R.id.action_rescan_from_marker_position:
                rescanMarkerPosition();
                break;
            case R.id.action_filter:
                showFilterDialog();
                break;
            case R.id.action_clear_markers:
                clearAllMarkers();
                break;
            case R.id.action_remove_search_point:
                removeMarkerPoint();
                break;
            case R.id.action_map_type:
                showMapTypeDialog();
                break;
            case R.id.action_set_tracker_type:
                setTrackerType();
                break;

            default:
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void setTrackerType(){
        int selection = pref.getTrackingType();
        CharSequence[] trackingTypes = new CharSequence[]{getString(R.string.location_tracker_text),getString(R.string.follow_tracker_text)};
        new AlertDialog.Builder(this)
                .setSingleChoiceItems(trackingTypes, selection , new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        pref.setTrackingType(i);
                        removeMarkerPoint();
                        dialogInterface.dismiss();
                        if(i == PokemapSharedPreferences.FOLLOW_TRACKING) {
                            hideAutoCompleteView();
                        }else{
                            showAutoCompleteView();
                        }
                    }
                }).create()
                .show();
    }

    private void hideAutoCompleteView() {
        View autoCompleteView = autocompleteFragment.getView();
        if(autoCompleteView != null){
            autoCompleteView.setVisibility(View.GONE);
        }
    }


    private void showAutoCompleteView() {
        View autoCompleteView = autocompleteFragment.getView();
        if(autoCompleteView != null){
            autoCompleteView.setVisibility(View.VISIBLE);
        }
    }

    private void removeMarkerPoint() {
        MapWrapperFragment mwp = (MapWrapperFragment)getSupportFragmentManager().findFragmentByTag(MapWrapperFragment.class.getName());
        if(mwp!=null && mwp.getMarkerPosition() != null){
            mwp.removeSearchMarker();
            mwp.setLocation(null);
            mwp.setStaticLocation(null);
            mwp.resetStepsPosition();
            Snackbar.make(findViewById(R.id.root), R.string.search_marker_removed_text, Snackbar.LENGTH_LONG).show();
        }
    }

    private void clearAllMarkers() {
        MapWrapperFragment mwf = (MapWrapperFragment)getSupportFragmentManager().findFragmentByTag(MapWrapperFragment.class.getName());
        if(mwf!=null){
            mwf.clearAllPokemon();
        }
    }

    private void rescanMarkerPosition() {
        MapWrapperFragment mwp = (MapWrapperFragment)getSupportFragmentManager().findFragmentByTag(MapWrapperFragment.class.getName());
        if(mwp!=null && mwp.getMarkerPosition() != null){
            mwp.setToMarkerPosition();
            mwp.resetStepsPosition();
            login();
        }else{
            Snackbar.make(findViewById(R.id.root), R.string.no_marker_placed_error_text, Snackbar.LENGTH_LONG).show();
        }
    }

    private void rescanCurrentPosition() {
        MapWrapperFragment mwp = (MapWrapperFragment)getSupportFragmentManager().findFragmentByTag(MapWrapperFragment.class.getName());
        if(mwp!=null){
            mwp.setLocation(null);
            mwp.setStaticLocation(null);
            mwp.resetStepsPosition();
        }
        login();
    }

    private void showMapTypeDialog(){
        final MapWrapperFragment mwp = (MapWrapperFragment)getSupportFragmentManager().findFragmentByTag(MapWrapperFragment.class.getName());
        int mapType = 0;
        if(mwp!=null){
            mapType = mwp.getMapType();
        }
        CharSequence[] mapChoices = new CharSequence[]{"Satellite","Terrain"};
        new AlertDialog.Builder(this)
                .setSingleChoiceItems(mapChoices, mapType == GoogleMap.MAP_TYPE_HYBRID ? 0 : 1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(mwp != null) {
                            mwp.setMapType(i == 0 ? GoogleMap.MAP_TYPE_HYBRID : GoogleMap.MAP_TYPE_NORMAL);
                        }
                        pref.setMapSelectionType(i);
                        dialogInterface.dismiss();
                    }
                }).create()
                .show();
    }

    private void showFilterDialog(){
        LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.filtered_pokemon_layout,null);
        final CheckBox filterAll = (CheckBox)view.findViewById(R.id.filter_select_all);
        RecyclerView filteredPokemonList = (RecyclerView)view.findViewById(R.id.filteredPokemonList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        filteredPokemonList.setLayoutManager(linearLayoutManager);
        final Set<String> pokemonList = pref.getFilteredPokemon();
        final List<FilteredPokemonModel> completeListOfPokemon = new LinkedList<>();
        final List<FilteredPokemonModel> filteredPokemonModels = new LinkedList<>();
        final IFilteredPokemonAdapterCallback filteredPokemonAdapterCallback = new IFilteredPokemonAdapterCallback() {
            @Override
            public void checkPressed(boolean isChecked, String pokemonId) {
                if (isChecked) {
                    pokemonList.add(pokemonId);
                } else {
                    pokemonList.remove(pokemonId);
                }
            }

            @Override
            public void checkAll() {
                for(FilteredPokemonModel f: completeListOfPokemon){
                    pokemonList.add("-1");
                    pokemonList.add(String.valueOf(f.getPokemonId()));
                    f.setSelected(true);
                }
            }

            @Override
            public void unCheckAll() {
                for(FilteredPokemonModel f: completeListOfPokemon){
                    pokemonList.remove("-1");
                    pokemonList.remove(String.valueOf(f.getPokemonId()));
                    f.setSelected(false);
                }
            }
        };

        for(PokemonIdOuterClass.PokemonId id: PokemonIdOuterClass.PokemonId.values()){
            if(id != MISSINGNO) {
                try {
                    FilteredPokemonModel filteredPokemonModel = new FilteredPokemonModel( PokemonIdUtils.getLocalePokemonName(getResources(), id),id.getNumber(),false);
                    if (pokemonList!=null && pokemonList.contains(String.valueOf(id.getNumber()))) {
                        filteredPokemonModel.setSelected(true);
                    }
                    filteredPokemonModels.add(filteredPokemonModel);
                    completeListOfPokemon.add(filteredPokemonModel);
                }catch(IllegalArgumentException iae){
                    //empty
                }
            }
        }
        final FilteredPokemonAdapter filteredPokemonAdapter = new FilteredPokemonAdapter(filteredPokemonModels,filteredPokemonAdapterCallback);
        filteredPokemonList.setAdapter(filteredPokemonAdapter);
        filterAll.setChecked(pokemonList.contains("-1"));
        filterAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                filteredPokemonAdapter.checkAllItems(b);
                if(b) {
                    filteredPokemonAdapterCallback.checkAll();
                }else{
                    filteredPokemonAdapterCallback.unCheckAll();
                }
            }
        });

        SearchView pokemonSearch = (SearchView)view.findViewById(R.id.filteredPokemon);
        pokemonSearch.setIconifiedByDefault(false);
        SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                System.out.println("Filteredlist size:"+completeListOfPokemon.size());
                filteredPokemonAdapter.updateList(filter(completeListOfPokemon, newText));
                return true;
            }
        };
        pokemonSearch.setOnQueryTextListener(queryTextListener);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filtered Pokemon")
                .setView(view)
                .setPositiveButton("DONE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        pref.setFilteredPokemon(pokemonList);
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .create()
                .show();
    }

    private List<FilteredPokemonModel> filter(List<FilteredPokemonModel> models, String query) {
        query = query.toLowerCase();

        final List<FilteredPokemonModel> filteredModelList = new LinkedList<>();
        for (FilteredPokemonModel model : models) {
            final String pokemonName = model.getPokemonName().toLowerCase();
            if (pokemonName.contains(query)) {
                filteredModelList.add(model);
            }
        }
        return filteredModelList;
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // TODO: test all this shit on a 6.0+ phone lmfao
        switch (requestCode) {
            case 703:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "permission granted");
                }
                break;
        }
    }

    private void login() {
        if(!pref.getGoogleAuthToken().isEmpty()) {
            nianticManager.setGoogleAuthToken(pref.getGoogleAuthToken());
        }else {
            if (!pref.isUsernameSet() || !pref.isPasswordSet()) {
                requestLoginCredentials();
            } else {
                nianticManager.login(pref.getUsername(), pref.getPassword());
            }
        }
    }

    private void requestLoginCredentials() {
        getSupportFragmentManager().beginTransaction().add(RequestCredentialsDialogFragment.newInstance(
                new RequestCredentialsDialogFragment.Listener() {
                    @Override
                    public void credentialsIntroduced(String username, String password) {
                        pref.setUsername(username);
                        pref.setPassword(password);
                        login();
                    }
                }), "request_credentials").commit();
    }

    /**
     * Called whenever a LoginEventResult is posted to the bus. Originates from LoginTask.java
     *
     * @param result Results of a log in attempt
     */
    @Subscribe
    public void onEvent(LoginEventResult result) {
        if (result.isLoggedIn()) {
            Snackbar.make(findViewById(R.id.root), "You have logged in successfully.", Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(findViewById(R.id.root), "Could not log in. Make sure your credentials are correct.", Snackbar.LENGTH_LONG).show();
            pref.setGoogleAuthToken("");
            pref.setRememberMe(false);
            pref.setRememberMeLoginType(PokemapSharedPreferences.PTC);
            startLoginActivity();
        }
    }


    private void startLoginActivity(){
        Intent intent = new Intent(this,LoginActivity.class);
        pref.setPassword("");
        startActivity(intent);
    };
    /**
     * Called whenever a use whats to search pokemons on a different position
     *
     * @param event PoJo with LatLng obj
     */
    @Subscribe
    public void onEvent(SearchInPosition event) {
        Snackbar.make(findViewById(R.id.root), R.string.searching_text, Snackbar.LENGTH_SHORT).show();
        nianticManager.getMapInformation(event.getPosition().latitude, event.getPosition().longitude, 0D);
    }

    /**
     * Called whenever a ServerUnreachableEvent is posted to the bus. Posted when the server cannot be reached
     *
     * @param event The event information
     */
    @Subscribe
    public void onEvent(ServerUnreachableEvent event) {
        Toast.makeText(this, "Unable to contact the Pokemon GO servers. The servers may be down.", Toast.LENGTH_LONG).show();
    }

    /**
     * Called whenever a TokenExpiredEvent is posted to the bus. Posted when the token from the login expired.
     *
     * @param event The event information
     */
    @Subscribe
    public void onEvent(TokenExpiredEvent event) {
        Toast.makeText(this, "The login token has expired. Getting a new one.", Toast.LENGTH_LONG).show();
        login();
    }

    @Override
    public void onResume() {
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void onPlaceSelected(Place place) {
        MapWrapperFragment mwp = (MapWrapperFragment)getSupportFragmentManager().findFragmentByTag(MapWrapperFragment.class.getName());
        if(mwp!=null){
            mwp.animateCameraToLocaton(place.getLatLng());
        }

    }

    @Override
    public void onError(Status status) {
        Snackbar.make(findViewById(R.id.root), R.string.google_places_search_error, Snackbar.LENGTH_LONG).show();
    }
}