package com.omkarmoghe.pokemap.views.map;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.omkarmoghe.pokemap.R;
import com.omkarmoghe.pokemap.controllers.app_preferences.PokemapSharedPreferences;
import com.omkarmoghe.pokemap.controllers.map.LocationManager;
import com.omkarmoghe.pokemap.controllers.net.NianticManager;
import com.omkarmoghe.pokemap.models.events.CatchablePokemonEvent;
import com.omkarmoghe.pokemap.models.events.PokemonMarker;
import com.omkarmoghe.pokemap.models.events.PokestopMarker;
import com.omkarmoghe.pokemap.models.events.PokestopsEvent;
import com.omkarmoghe.pokemap.models.events.SearchInPosition;
import com.omkarmoghe.pokemap.util.PokemonIdUtils;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A simple {@link Fragment} subclass.
 * <p>
 * Use the {@link MapWrapperFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapWrapperFragment extends Fragment implements OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMarkerClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 19;
    public static final String EXPIRED = "Expired";
    public static final int COUNTDOWN_HANDLER_ID = 1001;

    private Location mStaticLocation;

    private Location mLocationToCheck;

    private Location mMarkerPosition;

    private boolean mClearingExpiredPokemon;

    private Map<String,CatchablePokemon> persistentPokemonMarkerMap;

    private int STEPS = 100;

    private int x = 0;

    private int y = 0;

    private int dx;
    private int dy;

    private int currentStep;
    private int STEPS2 = STEPS * STEPS;

    private View mView;
    private SupportMapFragment mSupportMapFragment;
    private GoogleMap mGoogleMap;
    private Location mLocation = null;
    private Marker userSelectedPositionMarker = null;
    private Circle userSelectedPositionCircle = null;
    //    private List<Marker> markerList = new ArrayList<>();
    private Map<String, PokemonMarker> markerList = new HashMap<>();
    private HashMap<String, PokestopMarker> pokestopsList = new HashMap<>();
    private PokemapSharedPreferences mPref;
    private Handler handler;
    private Handler countDownHandler;

    public MapWrapperFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MapWrapperFragment.
     */
    public static MapWrapperFragment newInstance() {
        MapWrapperFragment fragment = new MapWrapperFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        persistentPokemonMarkerMap = new LinkedHashMap<>();
        super.onCreate(savedInstanceState);
    }

    public void registerEventBus() {
        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
        if(mLocationToCheck != null){
            NianticManager.getInstance().getMapInformation(mLocationToCheck.getLatitude(), mLocationToCheck.getLongitude(), 0.0);
        }else if(mStaticLocation != null){
            NianticManager.getInstance().getMapInformation(mStaticLocation.getLatitude(), mStaticLocation.getLongitude(), 0.0);
        }
    }

    public Location getMarkerPosition() {
        return mMarkerPosition;
    }

    public void setMarkerPosition(Location markerPosition) {
        mMarkerPosition = mMarkerPosition;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LocationManager locationManager = LocationManager.getInstance(getContext());
        locationManager.register(new LocationManager.Listener() {
            @Override
            public void onLocationChanged(Location location) {
                if (mLocation == null) {
                    mLocation = location;
                    mStaticLocation = location;
                    initMap();
                    NianticManager.getInstance().getMapInformation(mStaticLocation.getLatitude(), mStaticLocation.getLongitude(), 0.0);
                } else {
                    mLocation = location;
                }

            }
        });
        mPref = new PokemapSharedPreferences(getActivity());
        resetStepsPosition();
        // Inflate the layout for this fragment if the view is not null
        if (mView == null)
            mView = inflater.inflate(R.layout.fragment_map_wrapper, container, false);
        else {

        }

        // build the map
        if (mSupportMapFragment == null) {
            mSupportMapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace(R.id.map, mSupportMapFragment).commit();
        }

        if (mGoogleMap == null) {
            mSupportMapFragment.getMapAsync(this);
        }

        FloatingActionButton locationFab = (FloatingActionButton) mView.findViewById(R.id.location_fab);
        locationFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mLocation != null && mGoogleMap != null) {
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(mLocation.getLatitude(), mLocation.getLongitude()), 15));
                } else {
                    Snackbar.make(getView().findViewById(R.id.root), "Waiting on location...", Snackbar.LENGTH_LONG).show();
                }
            }
        });

        return mView;
    }

    public void resetStepsPosition() {
        x = 0;
        y = 0;
        dx = 0;
        dy = -1;
        STEPS = mPref.getSteps();
        STEPS2 = STEPS * STEPS;
        currentStep = 0;
    }


    public Location getStaticLocation() {
        return mStaticLocation;
    }

    public void setStaticLocation(Location mStaticLocation) {
        this.mStaticLocation = mStaticLocation;
    }

    public Location getLocation() {
        return mLocation;
    }

    public void setLocation(Location mLocation) {
        this.mLocation = mLocation;
    }

    public void removeSearchMarker() {
        if(userSelectedPositionMarker != null) {
            userSelectedPositionMarker.remove();
            userSelectedPositionMarker = null;
        }
    }

    public int getMapType() {
        return mGoogleMap.getMapType();
    }

    public void setMapType(int type) {
        mGoogleMap.setMapType(type);
    }

    public void setToMarkerPosition() {
        mLocation = mMarkerPosition;
        mStaticLocation = mMarkerPosition;
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(mMarkerPosition.getLatitude(), mMarkerPosition.getLongitude()), 15));
    }

    public void getNextLocation() {
        if(mStaticLocation != null) {
            if (x > (-STEPS2 / 2) && x <= STEPS2 / 2 && y > (-STEPS2 / 2) && y <= (STEPS2 / 2)) {
                if (mLocationToCheck == null) {
                    mLocationToCheck = new Location("dummyprovider");
                }
                mLocationToCheck.setLatitude(x * 0.0025 + mStaticLocation.getLatitude());
                mLocationToCheck.setLongitude(y * 0.0025 + mStaticLocation.getLongitude());
                NianticManager.getInstance().getMapInformation(mLocationToCheck.getLatitude(), mLocationToCheck.getLongitude(), 0D);
            }
            if (x == y || x < 0 && x == -y || x > 0 && x == 1 - y) {
                int currentdx = dx;
                int currentdy = dy;
                dx = -currentdy;
                dy = currentdx;
            }
            x += dx;
            y += dy;
        }

    }

    private void initMap() {
        if (mLocation != null && mGoogleMap != null && getActivity() != null) {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mGoogleMap.setMyLocationEnabled(true);
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLocation.getLatitude(), mLocation.getLongitude()), 15));
        }
    }

    public void clearAllPokemon() {
        mGoogleMap.clear();
        persistentPokemonMarkerMap.clear();
        if(userSelectedPositionMarker != null) {
            LatLng location = userSelectedPositionMarker.getPosition();
            userSelectedPositionMarker =  mGoogleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Position Picked")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        }
        markerList.clear();
    }


    public void clearingExpiredAndFilteredPokemonFromMarketList() {
        final Set<String> filteredPokemon = mPref.getFilteredPokemon();
        mClearingExpiredPokemon = true;
        for(Iterator<String> iter = markerList.keySet().iterator(); iter.hasNext();){
            String id = iter.next();
            CatchablePokemon pokemon = markerList.get(id).getPokemon();
            long time = pokemon.getExpirationTimestampMs();
            long timeLeft = time - System.currentTimeMillis();
            if (getDurationBreakdown(timeLeft).equalsIgnoreCase(EXPIRED) || filteredPokemon.contains(String.valueOf(pokemon.getPokemonId().getNumber()))) {
                PokemonMarker pokemonMarker = markerList.get(id);
                Marker marker = pokemonMarker.getMarker();
                marker.setVisible(false);
                marker.remove();
                iter.remove();
            }
        }
    }

    private void pokemonMarkersToAdd(){
        final Set<String> filteredPokemon = mPref.getFilteredPokemon();
        int markerSize = getResources().getDimensionPixelSize(R.dimen.pokemon_marker);
        for(final String id: persistentPokemonMarkerMap.keySet()){
            final CatchablePokemon poke = persistentPokemonMarkerMap.get(id);
            final long millisLeft = poke.getExpirationTimestampMs() - System.currentTimeMillis();
//            if(!markerList.containsKey(id) &&  !filteredPokemon.contains(String.valueOf(poke.getPokemonId().getNumber())) && !getDurationBreakdown(millisLeft).equalsIgnoreCase(EXPIRED)){
                Glide.with(getActivity())
                        .load("http://serebii.net/pokemongo/pokemon/" + PokemonIdUtils.getCorrectPokemonImageId(poke.getPokemonId().getNumber()) + ".png")
                        .asBitmap()
                        .skipMemoryCache(false)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(new SimpleTarget<Bitmap>(markerSize, markerSize) { // Width and height FIXME: Maybe get different sizes based on devices DPI? this need tests
                            @Override
                            public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                                if(!markerList.containsKey(id) &&  !filteredPokemon.contains(String.valueOf(poke.getPokemonId().getNumber())) && !getDurationBreakdown(millisLeft).equalsIgnoreCase(EXPIRED)) {
                                    //Setting marker since we got image
                                    //int resourceID = getResources().getIdentifier("p" + poke.getPokemonId().getNumber(), "drawable", getActivity().getPackageName());
                                    Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                            .position(new LatLng(poke.getLatitude(), poke.getLongitude()))
                                            .title(PokemonIdUtils.getLocalePokemonName(getResources(), poke.getPokemonId()))
                                            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                                            .snippet("Dissapears in: " + getDurationBreakdown(millisLeft))
                                            .anchor(0.5f, 0.5f));
                                    //adding pokemons to list to be removed on next search
                                    markerList.put(poke.getSpawnPointId(), new PokemonMarker(marker, poke));
                                }
//                                markerList.put(poke.getSpawnPointId(), new PokemonMarkerExtended(poke, marker));
                            }
                        });
//                int resourceID = getResources().getIdentifier("p" + poke.getPokemonId().getNumber(), "drawable", getActivity().getPackageName());
//                Marker marker = mGoogleMap.addMarker(new MarkerOptions()
//                        .position(new LatLng(poke.getLatitude(), poke.getLongitude()))
//                        .title(poke.getPokemonId().name())
//                        .snippet("Dissapears in: " + getDurationBreakdown(millisLeft))
//                        .icon(BitmapDescriptorFactory.fromResource(resourceID)));
//                PokemonMarker pokemonMarker = new PokemonMarker(marker,poke);
//                markerList.put(poke.getSpawnPointId(), pokemonMarker);
//            }
        }
    }

    private void pokemonMarkersToRemove(){
    }

    private void setPokemonMarkers(final Map<String, CatchablePokemon> pokeList) {
        if (mGoogleMap != null) {
            clearingExpiredAndFilteredPokemonFromMarketList();
            pokemonMarkersToAdd();

            //Removing all pokemons from map
//            if (markerList != null && !markerList.isEmpty()) {
//                for (Marker marker : markerList) {
//                    marker.remove();
//                }
//                markerList = new ArrayList<Marker>(); //cleaning the array
//            }

//            Map<Long, Marker> tempMarkers = new HashMap<>();
//            tempMarkers.putAll(markerList);
//            for (long o : markerList.keySet()) {
//                long time = o;
//                long timeLeft = time - System.currentTimeMillis();
//                if(getDurationBreakdown(timeLeft).equalsIgnoreCase(EXPIRED)){
//                    Marker marker = markerList.get(o);
//                    marker.setVisible(false);
//                    marker.remove();
//                    tempMarkers.remove(o);
//                }
//            }
//            markerList.clear();
//            markerList.putAll(tempMarkers);
//            for(Marker m: markerList.values()){

//            for (CatchablePokemon poke : pokeList.values()) {
//                int resourceID = getResources().getIdentifier("p" + poke.getPokemonId().getNumber(), "drawable", getActivity().getPackageName());
//                long millisLeft = poke.getExpirationTimestampMs() - System.currentTimeMillis();
//                if (!getDurationBreakdown(millisLeft).equals(EXPIRED) && !markerList.containsKey(poke.getSpawnPointId())) {
//                    Marker marker = mGoogleMap.addMarker(new MarkerOptions()
//                            .position(new LatLng(poke.getLatitude(), poke.getLongitude()))
//                            .title(poke.getPokemonId().name())
//                            .snippet("Dissapears in: " + getDurationBreakdown(millisLeft))
//                            .icon(BitmapDescriptorFactory.fromResource(resourceID)));
//                    PokemonMarker pokemonMarker = new PokemonMarker(marker,poke);
//                    markerList.put(poke.getSpawnPointId(), pokemonMarker);
//                }
////                marker.showInfoWindow();
//                //adding pokemons to list to be removed on next search
////                markerList.add(marker);
//            }
        } else {
            Snackbar.make(getView().findViewById(R.id.root), "The map is not initialized.", Snackbar.LENGTH_LONG).show();
        }
    }

    public static String getDurationBreakdown(long millis) {
        if (millis < 0) {
            return EXPIRED;
        }

        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        sb.append(minutes);
        sb.append(" Minutes ");
        sb.append(seconds);
        sb.append(" Seconds");

        return (sb.toString());
    }

    /**
     * Called whenever a CatchablePokemonEvent is posted to the bus. Posted when new catchable pokemon are found.
     *
     * @param event The event information
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CatchablePokemonEvent event) {
        if(event.getCatchablePokemon() != null) {
            persistentPokemonMarkerMap.putAll(event.getCatchablePokemon());
            for (Iterator<CatchablePokemon> iterator =  persistentPokemonMarkerMap.values().iterator();iterator.hasNext();) {
                CatchablePokemon poke = iterator.next();
                long millisLeft = poke.getExpirationTimestampMs() - System.currentTimeMillis();
                if(getDurationBreakdown(millisLeft).equals(EXPIRED)){
                    iterator.remove();
                }
            }

//            for (Map.Entry<String, CatchablePokemon> pokemonEntry : event.getMapInformation().entrySet()) {
//                String pokemonId = String.valueOf(pokemonEntry.getValue().getPokemonId().getNumber());
//                if (filteredPokemon != null) {
//                    if (filteredPokemon.contains(pokemonId)) {
//                        break;
//                    } else {
//                        tempPokemonMap.put(pokemonEntry.getKey(), pokemonEntry.getValue());
//                    }
//                } else {
//                    tempPokemonMap.put(pokemonEntry.getKey(), pokemonEntry.getValue());
//                }
//            }

//            for (Map.Entry<String, CatchablePokemon> pokemonEntry : event.getMapInformation().entrySet()) {
//                String pokemonId = String.valueOf(pokemonEntry.getValue().getPokemonId().getNumber());
//                if (filteredPokemon != null) {
//                    if (filteredPokemon.contains(pokemonId)) {
//                        break;
//                    } else {
//                        tempPokemonMap.put(pokemonEntry.getKey(), pokemonEntry.getValue());
//                    }
//                } else {
//                    tempPokemonMap.put(pokemonEntry.getKey(), pokemonEntry.getValue());
//                }
//            }
            setPokemonMarkers(persistentPokemonMarkerMap);
            if (currentStep != STEPS2) {
                currentStep += 1;
            } else {
                resetStepsPosition();
            }
            getNextLocation();
        }else{
            mLocation = null;
        }

    }

    /**
     * Called whenever a PokestopsEvent is posted to the bus. Posted when new pokestops are found.
     *
     * @param event The event information
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(PokestopsEvent event) {
//        setPokestopsMarkers(event.getPokestops());
    }


    private void setPokestopsMarkers(final Map<String,Pokestop> pokestops) {
        if (mGoogleMap != null) {
            if (pokestops != null && mPref.getShowPokestops()) {
                int pstopID = getResources().getIdentifier("pstop", "drawable", getActivity().getPackageName());
                int pstopLuredID = getResources().getIdentifier("pstop_lured", "drawable", getActivity().getPackageName());
                for (String id : pokestops.keySet()) {
                    if (!pokestopsList.containsKey(id)) {
                        Pokestop pokestop = pokestops.get(id);
                        Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                .position(new LatLng(pokestop.getLatitude(), pokestop.getLongitude()))
                                .title("PokeStop")
                                .icon(BitmapDescriptorFactory.fromResource(pokestop.hasLurePokemon() ? pstopLuredID : pstopID))
                                .anchor(0.5f, 0.5f));

                        //adding pokemons to list to be removed on next search
                        pokestopsList.put(pokestop.getId(), new PokestopMarker(pokestop, marker));
                    }
                }
            }
//            updateMarkers();
//
//        } else {
//            showMapNotInitializedError();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        UiSettings settings = mGoogleMap.getUiSettings();
        settings.setCompassEnabled(true);
        settings.setTiltGesturesEnabled(true);
        settings.setMyLocationButtonEnabled(false);
        //Handle long click
        mGoogleMap.setOnMapLongClickListener(this);
        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.setMapType(mPref.getMapSelectionType() == 0 ? GoogleMap.MAP_TYPE_HYBRID:GoogleMap.MAP_TYPE_NORMAL);
        //Disable for now coz is under FAB
        settings.setMapToolbarEnabled(false);
        mGoogleMap.setOnInfoWindowClickListener(this);
        initMap();
    }

    @Override
    public void onMapLongClick(LatLng position) {
        //Draw user position marker with circle
        drawMarkerWithCircle(position);

        //Sending event to MainActivity
        SearchInPosition sip = new SearchInPosition();
        if(mMarkerPosition == null){
            mMarkerPosition = new Location("DummyProvider");
        }
        mMarkerPosition.setLongitude(position.longitude);
        mMarkerPosition.setLatitude(position.latitude);
        mStaticLocation.setLatitude(position.latitude);
        mStaticLocation.setLongitude(position.longitude);
        mLocation.setLatitude(position.latitude);
        mLocation.setLongitude(position.longitude);
        if(mGoogleMap!=null) {
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mStaticLocation.getLatitude(), mStaticLocation.getLongitude()), 15));
        }
        resetStepsPosition();
        sip.setPosition(position);
        EventBus.getDefault().post(sip);
    }

    private void drawMarkerWithCircle(LatLng position) {
        if(userSelectedPositionMarker != null) {
            userSelectedPositionMarker.remove();
        }
        userSelectedPositionMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(position)
                .title("Position Picked")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

    }

    @Override
    public void onInfoWindowClick(final Marker marker) {
        long time = 0L;
        View view  = getActivity().getLayoutInflater().inflate(R.layout.marker_pokemon_info_layout,null);
        TextView pokemonName = (TextView)view.findViewById(R.id.pokemonName);
        TextView pokemonId = (TextView)view.findViewById(R.id.pokemonId);
        final TextView pokemonDescription = (TextView)view.findViewById(R.id.pokemonDisappearText);
        Button filterButton = (Button)view.findViewById(R.id.add_to_filter_button);
        Button shareLocationButton = (Button)view.findViewById(R.id.share_location_button);
        String pokemonIdString = "";
        PokemonMarker pokemonMarker = null;
        for (String o : markerList.keySet()) {
            if (markerList.get(o).getMarker().getPosition().equals(marker.getPosition())) {
                time = markerList.get(o).getPokemon().getExpirationTimestampMs();
                pokemonMarker = markerList.get(o);
                pokemonName.setText(PokemonIdUtils.getLocalePokemonName(getResources(), pokemonMarker.getPokemon().getPokemonId()));
                pokemonIdString = String.valueOf(pokemonMarker.getPokemon().getPokemonId().getNumber());
                pokemonId.setText(Html.fromHtml("<a href=http://www.pokemon.com/uk/pokedex/"+pokemonIdString+">#"+pokemonIdString+"</a>"));
                pokemonId.setAutoLinkMask(Linkify.ALL);
                final String finalPokemonIdString1 = pokemonIdString;
                pokemonId.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String url = "http://www.pokemon.com/uk/pokedex/"+ finalPokemonIdString1;
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                    }
                });
                pokemonDescription.setText(marker.getSnippet());
            }
        }

        final String finalPokemonIdString = pokemonIdString;
        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPref.addPokemonToFilteredList(finalPokemonIdString);
//                filteredPokemon.add(String.valueOf(finalPokemonIdString));
            }
        });

        final PokemonMarker finalPokemonMarker = pokemonMarker;
        shareLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(finalPokemonMarker!= null) {
                    LatLng location = new LatLng(finalPokemonMarker.getPokemon().getLatitude(),finalPokemonMarker.getPokemon().getLongitude());
                    try {
                        String address = getAddress(location).replace(" ","+");
                        String ShareSub = PokemonIdUtils.getLocalePokemonName(getResources(), finalPokemonMarker.getPokemon().getPokemonId())+" found here";
                        String uri = ShareSub+"\n\nhttps://www.google.com/maps/place/"+address+"/@"+finalPokemonMarker.getPokemon().getLatitude()+","+finalPokemonMarker.getPokemon().getLongitude();
                        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                        sharingIntent.setType("text/plain");

                        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, ShareSub);
                        sharingIntent.putExtra(Intent.EXTRA_TEXT, uri);
                        startActivity(Intent.createChooser(sharingIntent, "Share via"));
                        System.out.println("test:"+address);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            private String getAddress(LatLng location) throws IOException {
                Geocoder geocoder;
                List<Address> addresses;
                geocoder = new Geocoder(getActivity(), Locale.getDefault());

                addresses = geocoder.getFromLocation(location.latitude, location.longitude,1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                String city = addresses.get(0).getLocality();
                String state = addresses.get(0).getAdminArea();
                String countryCode = addresses.get(0).getLocale().getLanguage();
                String country = addresses.get(0).getCountryName();
                String postalCode = addresses.get(0).getPostalCode();
                String knownName = addresses.get(0).getFeatureName();
                StringBuilder stringBuilder = new StringBuilder(address);
                if(city != null){
                    stringBuilder.append(","+city);
                }
                if(postalCode != null){
                    stringBuilder.append(","+postalCode);
                }
                return stringBuilder.toString();
            }
        });
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle("Pokemon Info")
                .setView(view)
                .setPositiveButton("Get Directions", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://maps.google.com/maps?saddr=" + LocationManager.getInstance(getActivity()).getLocation().latitude + "," + LocationManager.getInstance(getActivity()).getLocation().longitude + "&daddr=" + marker.getPosition().latitude + "," + marker.getPosition().longitude));
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                countDownHandler.removeMessages(COUNTDOWN_HANDLER_ID);
            }
        });

        final Dialog dialog = alertDialog.create();
        dialog.show();

        final long finalTime = time;
        countDownHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                final long millisLeft = finalTime - System.currentTimeMillis();
                if(millisLeft < 0){
                    dialog.dismiss();
                }else {
                    if (pokemonDescription != null) {
                        pokemonDescription.setText("Disappears in: " + getDurationBreakdown(millisLeft));
                    }
                    countDownHandler.sendEmptyMessageDelayed(COUNTDOWN_HANDLER_ID,1000);
                }
                super.handleMessage(msg);
            }
        };
        countDownHandler.sendEmptyMessage(COUNTDOWN_HANDLER_ID);

    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        long time = 0L;
        if(userSelectedPositionMarker!= null && marker.getId().equals(userSelectedPositionMarker.getId())){
            return true;
        }
        for (String o : markerList.keySet()) {
            if (markerList.get(o).getMarker().getPosition().equals(marker.getPosition())) {
                time = markerList.get(o).getPokemon().getExpirationTimestampMs();
            }
        }
        final long millisLeft = time - System.currentTimeMillis();
        if (getDurationBreakdown(millisLeft).equals(EXPIRED)) {
            marker.setVisible(false);
            marker.remove();
        }
        marker.setSnippet("Disappears in: " + getDurationBreakdown(millisLeft));


        return false;
    }
}

