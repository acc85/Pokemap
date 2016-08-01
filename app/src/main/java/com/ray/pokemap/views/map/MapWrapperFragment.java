package com.ray.pokemap.views.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
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
import android.widget.ImageButton;
import android.widget.TextView;

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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.ray.pokemap.R;
import com.ray.pokemap.controllers.app_preferences.PokemapAppPreferences;
import com.ray.pokemap.controllers.app_preferences.PokemapSharedPreferences;
import com.ray.pokemap.controllers.map.LocationManager;
import com.ray.pokemap.controllers.net.NianticManager;
import com.ray.pokemap.models.events.CatchablePokemonEvent;
import com.ray.pokemap.models.events.PokeStopsEvent;
import com.ray.pokemap.models.events.PokemonMarker;
import com.ray.pokemap.models.events.PokestopMarker;
import com.google.maps.android.SphericalUtil;
import com.ray.pokemap.models.events.ServerUnreachableEvent;
import com.ray.pokemap.trackingService.LocationTrackerService;
import com.ray.pokemap.util.PokemonIdUtils;
import com.ray.pokemap.views.GoogleLoginEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
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

    //    private static final int LOCATION_PERMISSION_REQUEST = 19;
    private static final String EXPIRED = "Expired";
    private static final int COUNTDOWN_HANDLER_ID = 1001;
    public static final int TYPE_CHECK_HANDLER_ID = 1021;
    public static final int TIMER_HANDLER_ID = 1001;
    public static final int CHECK_TIMER_HANDLER_ID = 1005;

    private Location mStaticLocation;

    private Location mLocationToCheck;

    private Location mMarkerPosition;

    private Map<String, CatchablePokemon> persistentPokemonMarkerMap;

//    private boolean processingPokeStops;

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
    private Marker searchLocationMarker = null;
    //    private Circle userSelectedPositionCircle = null;
    private boolean reset;
    private Map<String, PokemonMarker> markerList = new HashMap<>();
    private HashMap<String, PokestopMarker> pokestopsList = new HashMap<>();
    private Map<String,Marker> customTrackingLocationPoints;
    private PokemapAppPreferences mPref;
    private Handler countDownHandler;
    private int currentCustomLocationArrayPos;
    private boolean isAddingToCustomPoints;
    private Set<LatLng> positionsToAdd;
    private Handler typeChecker;
    private int timer;
    private Handler timerHandler;
    private Handler checkTimerHandler;

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
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        if (mLocationToCheck != null) {
            getSearchLocationMarker(new LatLng(mLocationToCheck.getLatitude(), mLocationToCheck.getLongitude()));
            NianticManager.getInstance().getMapInformation(mLocationToCheck.getLatitude(), mLocationToCheck.getLongitude(), 0.0);
        } else if (mStaticLocation != null) {
            getSearchLocationMarker(new LatLng(mStaticLocation.getLatitude(), mStaticLocation.getLongitude()));
            NianticManager.getInstance().getMapInformation(mStaticLocation.getLatitude(), mStaticLocation.getLongitude(), 0.0);
        }
    }

    public void continueMapInfoGatherer() {
        getSearchLocationMarker(new LatLng(mLocationToCheck.getLatitude(), mLocationToCheck.getLongitude()));
        NianticManager.getInstance().getMapInformation(mLocationToCheck.getLatitude(), mLocationToCheck.getLongitude(), 0.0);
    }

    public void animateCameraToLocaton(LatLng location) {
        searchAtMarkedLocation(location);
    }

    public Location getMarkerPosition() {
        return mMarkerPosition;
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
//                    getSearchLocationMarker(new LatLng(mStaticLocation.getLatitude(), mStaticLocation.getLongitude()));
                    NianticManager.getInstance().getMapInformation(mStaticLocation.getLatitude(), mStaticLocation.getLongitude(), 0.0);
                } else {
                    mLocation = location;
                }
                if (mPref.getTrackingType() == PokemapSharedPreferences.FOLLOW_TRACKING) {
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(
                            new LatLng(mLocation.getLatitude(), mLocation.getLongitude())));
                }
            }
        });
        mPref = new PokemapSharedPreferences(getActivity().getApplicationContext());
        resetStepsPosition();
        // Inflate the layout for this fragment if the view is not null
        if (mView == null) {
            mView = inflater.inflate(R.layout.fragment_map_wrapper, container, false);
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
                    showSnackBar(getString(R.string.waiting_on_location_text));
                }
            }
        });

        return mView;
    }

    public Set<LatLng> getPositionsToAdd() {
        if (positionsToAdd == null) {
            positionsToAdd = new HashSet<>();
        }
        return positionsToAdd;
    }

    public Map<String,Marker> getCustomTrackingLocationPoints() {
        if (customTrackingLocationPoints == null) {
            customTrackingLocationPoints = new LinkedHashMap<>();
        }
        return customTrackingLocationPoints;
    }


    public void resetStepsPosition() {
        x = 0;
        y = 0;
        dx = 0;
        dy = -1;
        STEPS = mPref.getSteps();
        STEPS2 = STEPS * STEPS;
        currentStep = 0;
        reset = true;
    }

    public void setStaticLocation(Location mStaticLocation) {
        this.mStaticLocation = mStaticLocation;
    }

    public void setLocation(Location mLocation) {
        this.mLocation = mLocation;
    }

    public void removeSearchMarker() {
        if (userSelectedPositionMarker != null) {
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
        resetStepsPosition();
    }


    private void searchAtMarkedLocation(LatLng position) {
        if(mPref.getTrackingType() != PokemapSharedPreferences.CUSOTM_LOCATION_POINTS_TRACKING) {
            drawMarker(position);
        }
        //Sending event to MainActivity
//        SearchInPosition sip = new SearchInPosition();
        if (mMarkerPosition == null) {
            mMarkerPosition = new Location("DummyProvider");
        }
        mMarkerPosition.setLongitude(position.longitude);
        mMarkerPosition.setLatitude(position.latitude);
        mStaticLocation.setLatitude(position.latitude);
        mStaticLocation.setLongitude(position.longitude);
        if(mLocation != null) {
            mLocation.setLatitude(position.latitude);
            mLocation.setLongitude(position.longitude);
        }
        if (mGoogleMap != null) {
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mStaticLocation.getLatitude(), mStaticLocation.getLongitude()), 15));
        }
        resetStepsPosition();
//        sip.setPosition(position);
//        EventBus.getDefault().post(sip);
    }

    private void getNextLocation() {
        if (mPref.getTrackingType() == PokemapSharedPreferences.LOCATION_TRACKING) {
            for(Marker m: getCustomTrackingLocationPoints().values()){
                m.remove();
            }
            getCustomTrackingLocationPoints().clear();
            customTrackingLocationPoints.clear();
            performLocationTracking();
        } else if (mPref.getTrackingType() == PokemapSharedPreferences.FOLLOW_TRACKING) {
            customTrackingLocationPoints.clear();
            performFollowTracking();
        } else if (mPref.getTrackingType() == PokemapSharedPreferences.CUSOTM_LOCATION_POINTS_TRACKING) {
            performCustomPointTracking();
        }

    }

    private void performCustomPointTracking() {
        resetStepsPosition();
        if (!getCustomTrackingLocationPoints().isEmpty()) {
            if(currentCustomLocationArrayPos >= getCustomTrackingLocationPoints().size()){
                currentCustomLocationArrayPos = 0;
            }
            Marker markerPoint = (Marker) getCustomTrackingLocationPoints().values().toArray()[currentCustomLocationArrayPos];
            LatLng location = markerPoint.getPosition();
            mLocationToCheck.setLatitude(location.latitude);
            mLocationToCheck.setLongitude(location.longitude);
            markerPoint.setIcon(BitmapDescriptorFactory.fromBitmap(getBlueSearchBitmap()));
            if (currentCustomLocationArrayPos >= getCustomTrackingLocationPoints().size()) {
                currentCustomLocationArrayPos = 0;
            }
            getSearchLocationMarker(new LatLng(mLocationToCheck.getLatitude(), mLocationToCheck.getLongitude()));
            NianticManager.getInstance().getMapInformation(mLocationToCheck.getLatitude(), mLocationToCheck.getLongitude(), 0D);
        } else {
            removeSearchLocationMarker();
            getTrackerTypeHandler().sendEmptyMessage(TYPE_CHECK_HANDLER_ID);
        }
    }

    public Handler getTrackerTypeHandler(){
        if(typeChecker == null) {
            HandlerThread thread = new HandlerThread("TYPE CHECKER THREAD");
            thread.start();
            typeChecker = new Handler(thread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (mPref.getTrackingType() == PokemapSharedPreferences.LOCATION_TRACKING
                            || mPref.getTrackingType() == PokemapSharedPreferences.FOLLOW_TRACKING
                            || (mPref.getTrackingType() == PokemapSharedPreferences.CUSOTM_LOCATION_POINTS_TRACKING && !getCustomTrackingLocationPoints().isEmpty())) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getNextLocation();
                            }
                        });
                    }
                    else {
                        typeChecker.sendEmptyMessage(TYPE_CHECK_HANDLER_ID);
                    }
                }
            };

        }
        return typeChecker;
    }

    public void checkForTrackerChange(){
        if (mPref.getTrackingType() == PokemapSharedPreferences.LOCATION_TRACKING || mPref.getTrackingType() == PokemapSharedPreferences.FOLLOW_TRACKING) {
            getNextLocation();
        } else if (mPref.getTrackingType() == PokemapSharedPreferences.CUSOTM_LOCATION_POINTS_TRACKING) {

        }
    }

    public void removeSearchLocationMarker() {
        if (searchLocationMarker != null) {
            searchLocationMarker.remove();
            searchLocationMarker = null;
        }
    }

    private void performFollowTracking() {
        getSearchLocationMarker(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
        NianticManager.getInstance().getMapInformation(mLocation.getLatitude(), mLocation.getLongitude(), 0D);
    }

    private void performLocationTracking() {
        if (mStaticLocation != null) {
            if (!reset) {
                if (x > (-STEPS2 / 2) && x <= STEPS2 / 2 && y > (-STEPS2 / 2) && y <= (STEPS2 / 2)) {
                    getLocationToCheck().setLatitude(x * 0.0025 + mStaticLocation.getLatitude());
                    getLocationToCheck().setLongitude(y * 0.0025 + mStaticLocation.getLongitude());
                    getSearchLocationMarker(new LatLng(mLocationToCheck.getLatitude(), mLocationToCheck.getLongitude()));
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
            } else {
                reset = false;
                getSearchLocationMarker(new LatLng(mStaticLocation.getLatitude(), mStaticLocation.getLongitude()));
                NianticManager.getInstance().getMapInformation(mStaticLocation.getLatitude(), mStaticLocation.getLongitude(), 0D);
            }
        }
    }

    private Location getLocationToCheck() {
        if (mLocationToCheck == null) {
            mLocationToCheck = new Location("dummyprovider");
        }
        return mLocationToCheck;
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

    private void getSearchLocationMarker(LatLng location) {
        if (searchLocationMarker == null && mGoogleMap != null) {
            try {
                Bitmap drawableBitmap = getBlueSearchBitmap();
                MarkerOptions markerOptions = new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(drawableBitmap))
                        .position(location);
                searchLocationMarker = mGoogleMap.addMarker(markerOptions);
            } catch (NullPointerException npe) {
                //empty
            }
        } else {
            searchLocationMarker.setPosition(location);
        }
    }

    @NonNull
    private Bitmap getBlueSearchBitmap() {
        Drawable secondLayer = getResources().getDrawable(R.drawable.ic_location_searching_blue_24dp);
        Bitmap drawableBitmap = Bitmap.createBitmap(secondLayer.getIntrinsicWidth(), secondLayer.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(drawableBitmap);
        secondLayer.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        secondLayer.draw(canvas);
        return drawableBitmap;
    }

    public void clearAllPokemon() {
        mGoogleMap.clear();
        persistentPokemonMarkerMap.clear();
        if (userSelectedPositionMarker != null) {
            LatLng location = userSelectedPositionMarker.getPosition();
            userSelectedPositionMarker = mGoogleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Position Picked")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        }
        markerList.clear();
    }


    private void clearingExpiredAndFilteredPokemonFromMarketList() {
        final Set<String> filteredPokemon = mPref.getFilteredPokemon();
        for (Iterator<String> iter = markerList.keySet().iterator(); iter.hasNext(); ) {
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

    private void pokemonMarkersToAdd() {
        final Set<String> filteredPokemon = mPref.getFilteredPokemon();
        int markerSize = getResources().getDimensionPixelSize(R.dimen.pokemon_marker);
        for (final String id : persistentPokemonMarkerMap.keySet()) {
            final CatchablePokemon poke = persistentPokemonMarkerMap.get(id);
            final long millisLeft = poke.getExpirationTimestampMs() - System.currentTimeMillis();
            Glide.with(getActivity())
                    .load(String.format(getString(R.string.serbii_link), PokemonIdUtils.getCorrectPokemonImageId(poke.getPokemonId().getNumber())))
                    .asBitmap()
                    .skipMemoryCache(false)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(new SimpleTarget<Bitmap>(markerSize, markerSize) { // Width and height FIXME: Maybe get different sizes based on devices DPI? this need tests
                        @Override
                        public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                            if (!markerList.containsKey(id) && !filteredPokemon.contains(String.valueOf(poke.getPokemonId().getNumber())) && !getDurationBreakdown(millisLeft).equalsIgnoreCase(EXPIRED)) {
                                //Setting marker since we got image
                                //int resourceID = getResources().getIdentifier("p" + poke.getPokemonId().getNumber(), "drawable", getActivity().getPackageName());
                                Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(poke.getLatitude(), poke.getLongitude()))
                                        .title(PokemonIdUtils.getLocalePokemonName(getResources(), poke.getPokemonId()))
                                        .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                                        .snippet("Disappears in: " + getDurationBreakdown(millisLeft))
                                        .anchor(0.5f, 0.5f));
                                //adding pokemons to list to be removed on next search
                                markerList.put(poke.getSpawnPointId(), new PokemonMarker(marker, poke));
                            } else if (markerList.containsKey(id)) {
                                PokemonMarker pokemonMarker = markerList.get(id);
                                if (pokemonMarker.isIsMarked()) {
                                    BitmapDrawable firstLayer = new BitmapDrawable(bitmap);
                                    Drawable secondLayer = getResources().getDrawable(R.drawable.ic_block_black_24dp);
                                    LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{firstLayer, secondLayer});
                                    Bitmap drawableBitmap = Bitmap.createBitmap(layerDrawable.getIntrinsicWidth(), layerDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                                    Canvas canvas = new Canvas(drawableBitmap);
                                    layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                                    layerDrawable.draw(canvas);
                                    pokemonMarker.getMarker().setIcon(BitmapDescriptorFactory.fromBitmap(drawableBitmap));
                                } else {
                                    pokemonMarker.getMarker().setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
                                }
//                                if(pokemonMarker.isIsMarked()){
//                                    pokemonMarker.getMarker().setTitle(PokemonIdUtils.getLocalePokemonName(getResources(), poke.getPokemonId())+"(Encountered)");
//                                }else{
//                                    pokemonMarker.getMarker().setTitle(PokemonIdUtils.getLocalePokemonName(getResources(), poke.getPokemonId()));
//                                }
                            }
                        }
                    });
//            }
        }
    }

    private void setPokemonMarkers() {
        if (mGoogleMap != null) {
            clearingExpiredAndFilteredPokemonFromMarketList();
            pokemonMarkersToAdd();
        } else {
            showSnackBar(getString(R.string.map_not_initialized_error));

        }
    }

    private void showSnackBar(String message) {
        if (getView() != null) {
            Snackbar.make(getView().findViewById(R.id.root), message, Snackbar.LENGTH_LONG).show();
        }
    }

    private static String getDurationBreakdown(long millis) {
        if (millis < 0) {
            return EXPIRED;
        }

        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        return (String.valueOf(minutes) +
                " Minutes " +
                seconds +
                " Seconds");
    }

    /**
     * Called whenever a CatchablePokemonEvent is posted to the bus. Posted when new catchable pokemon are found.
     *
     * @param event The event information
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CatchablePokemonEvent event) {
        final HandlerThread timerThread = new HandlerThread("TIMER_THREAD");
        timerThread.start();
        final int interval = mPref.getScanInterval();
        timerHandler = new Handler(timerThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                timer++;
                if(timer < interval) {
                    timerHandler.sendEmptyMessageDelayed(TIMER_HANDLER_ID, 1000);
                }else {
                    timerThread.quit();
                }
                super.handleMessage(msg);
            }
        };
        timerHandler.sendEmptyMessage(TIMER_HANDLER_ID);

        if (event.getCatchablePokemon() != null) {
            persistentPokemonMarkerMap.putAll(event.getCatchablePokemon());
            for (Iterator<CatchablePokemon> iterator = persistentPokemonMarkerMap.values().iterator(); iterator.hasNext(); ) {
                CatchablePokemon poke = iterator.next();
                long millisLeft = poke.getExpirationTimestampMs() - System.currentTimeMillis();
                if (getDurationBreakdown(millisLeft).equals(EXPIRED)) {
                    iterator.remove();
                }
            }
            setPokemonMarkers();
            if (currentStep != STEPS2 && STEPS2 / mPref.getSteps() == mPref.getSteps()) {
                currentStep += 1;
            } else {
                resetStepsPosition();
            }
            final HandlerThread handlerThread = new HandlerThread("TIMER_CHECK_THREAD");
            handlerThread.start();
            checkTimerHandler = new Handler(handlerThread.getLooper()){
                @Override
                public void handleMessage(Message msg) {
                    if(timer < interval){
                        checkTimerHandler.sendEmptyMessage(CHECK_TIMER_HANDLER_ID);
                    }else {
                        timer = 0;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(mPref.getTrackingType() == PokemapSharedPreferences.CUSOTM_LOCATION_POINTS_TRACKING){
                                    if(getCustomTrackingLocationPoints().size() > 1) {
                                        if (currentCustomLocationArrayPos >= getCustomTrackingLocationPoints().size()) {
                                            Marker previousPoint = (Marker) getCustomTrackingLocationPoints().values().toArray()[0];
                                            previousPoint.setIcon(BitmapDescriptorFactory.fromBitmap(getRedSearchBitmap()));
                                        } else {
                                            Marker previousPoint = (Marker) getCustomTrackingLocationPoints().values().toArray()[currentCustomLocationArrayPos];
                                            previousPoint.setIcon(BitmapDescriptorFactory.fromBitmap(getRedSearchBitmap()));
                                        }
                                        currentCustomLocationArrayPos++;
                                    }

                                }
                                getNextLocation();
                            }
                        });
                        handlerThread.quit();
                    }
                    super.handleMessage(msg);
                }
            };
            checkTimerHandler.sendEmptyMessage(CHECK_TIMER_HANDLER_ID);
        } else {
            mLocation = null;
        }

    }

    public void hideSearchMarkers(){
        if(mPref.getIsHidingSearchMarkers()) {
            for (Marker m : getCustomTrackingLocationPoints().values()) {
                m.setVisible(false);
            }
        }else{
            for (Marker m : getCustomTrackingLocationPoints().values()) {
                m.setVisible(true);
            }
        }
    }


    /**
     * Called whenever a CatchablePokemonEvent is posted to the bus. Posted when new catchable pokemon are found.
     *
     * @param event The event information
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ServerUnreachableEvent event) {
        continueMapInfoGatherer();
    }

    /**
     * Called whenever a TokenExpiredEvent is posted to the bus. Posted when the token from the login expired.
     *
     * @param event The event information
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(GoogleLoginEvent event) {
        Snackbar.make(getActivity().findViewById(R.id.root), "Gathering Map Data.", Snackbar.LENGTH_SHORT).show();
        continueMapInfoGatherer();
    }

    /**
     * Called whenever a PokestopsEvent is posted to the bus. Posted when new pokestops are found.
     *
     * @param event The event information
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(PokeStopsEvent event) {
//        if(!processingPokeStops) {
//            processingPokeStops = true;
//            setPokestopsMarkers(event.getPokestops());
//            processingPokeStops = false;
//        }

    }


//    private void setPokestopsMarkers(final Map<String, Pokestop> pokestops) {
//        if (mGoogleMap != null) {
//            if (pokestops != null && mPref.getShowPokeStops()) {
//                int pstopID = getResources().getIdentifier("pstop", "drawable", getActivity().getPackageName());
//                int pstopLuredID = getResources().getIdentifier("pstop_lured", "drawable", getActivity().getPackageName());
//                for (String id : pokestops.keySet()) {
//                    if (pokestopsList.containsKey(id)) {
//                        PokestopMarker pokestopMarker = pokestopsList.get(id);
//                        Marker pokeStopMarker = pokestopMarker.getMarker();
//                        pokeStopMarker.setIcon(BitmapDescriptorFactory.fromResource(pokestops.get(id).hasLurePokemon() ? pstopLuredID : pstopID));
//                    }else {
//                        Pokestop pokestop = pokestops.get(id);
//                        Marker marker = mGoogleMap.addMarker(new MarkerOptions()
//                                .position(new LatLng(pokestop.getLatitude(), pokestop.getLongitude()))
//                                .title("PokeStop")
//                                .icon(BitmapDescriptorFactory.fromResource(pokestop.hasLurePokemon() ? pstopLuredID : pstopID))
//                                .anchor(0.5f, 0.5f));
//
//                        //adding pokemons to list to be removed on next search
//                        pokestopsList.put(pokestop.getId(), new PokestopMarker(pokestop, marker));
//                    }
//                }
//            }
////            updateMarkers();
////
////        } else {
////            showMapNotInitializedError();
//        }
//    }

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
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
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
        mGoogleMap.setMapType(mPref.getMapSelectionType() == 0 ? GoogleMap.MAP_TYPE_HYBRID : GoogleMap.MAP_TYPE_NORMAL);
        //Disable for now coz is under FAB
        settings.setMapToolbarEnabled(false);
        mGoogleMap.setOnInfoWindowClickListener(this);
        initMap();
    }

    @Override
    public void onMapLongClick(LatLng position) {
        if (mPref.getTrackingType() == PokemapSharedPreferences.LOCATION_TRACKING) {
            //Draw user position marker with circle
            searchAtMarkedLocation(position);
        } else if (mPref.getTrackingType() == PokemapSharedPreferences.CUSOTM_LOCATION_POINTS_TRACKING) {
            longClickForCustomPointTracking(position);
        }
    }

    private void longClickForCustomPointTracking(LatLng position) {
        Marker customPointMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(new LatLng(position.latitude, position.longitude))
                .icon(BitmapDescriptorFactory.fromBitmap(getRedSearchBitmap()))
                .title("Search point "+(currentCustomLocationArrayPos+1))
                .snippet("Tap here to remove point")
                .visible(!mPref.getIsHidingSearchMarkers()));
        getCustomTrackingLocationPoints().put(customPointMarker.getId(),customPointMarker);
        if (getCustomTrackingLocationPoints().size() == 1) {
            getLocationToCheck().setLatitude(position.latitude);
            getLocationToCheck().setLongitude(position.longitude);
            NianticManager.getInstance().getMapInformation(mLocationToCheck.getLatitude(), mLocationToCheck.getLongitude(), 0D);
            getSearchLocationMarker(new LatLng(position.latitude, position.longitude));
            currentCustomLocationArrayPos++;
            if (currentCustomLocationArrayPos >= getCustomTrackingLocationPoints().size()) {
                currentCustomLocationArrayPos = 0;
            }
        }
        Vibrator vibrator = (Vibrator)getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(100);
        Snackbar.make(getActivity().findViewById(R.id.root),"Point added", Snackbar.LENGTH_SHORT).show();
    }


    public void clearAllSearchMarkers(){
        for(Marker m: getCustomTrackingLocationPoints().values()){
            m.remove();
        }
        getCustomTrackingLocationPoints().clear();
    }

    @NonNull
    private Bitmap getRedSearchBitmap() {
        Drawable locationPointMarker = getResources().getDrawable(R.drawable.ic_location_searching_point_24dp);
        Bitmap drawableBitmap = Bitmap.createBitmap(locationPointMarker.getIntrinsicWidth(), locationPointMarker.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(drawableBitmap);
        locationPointMarker.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        locationPointMarker.draw(canvas);
        return drawableBitmap;
    }


    private void drawMarker(LatLng position) {
        if (userSelectedPositionMarker != null) {
            userSelectedPositionMarker.remove();
        }
        userSelectedPositionMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(position)
                .title("Position Picked")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

    }

    @Override
    @SuppressLint("InflateParams")
    public void onInfoWindowClick(final Marker marker) {
        if(getCustomTrackingLocationPoints().containsKey(marker.getId())){
            marker.remove();
            getCustomTrackingLocationPoints().remove(marker.getId());;
        }else {
            long time = 0L;
            View view = getActivity().getLayoutInflater().inflate(R.layout.marker_pokemon_info_layout, null);
            ImageButton markButton = (ImageButton) view.findViewById(R.id.mark_button);
            TextView pokemonName = (TextView) view.findViewById(R.id.pokemonName);
            TextView pokemonId = (TextView) view.findViewById(R.id.pokemonId);
            final TextView pokemonDescription = (TextView) view.findViewById(R.id.pokemonDisappearText);
            final TextView pokemonDistance = (TextView) view.findViewById(R.id.pokemonDistanceText);
            Button filterButton = (Button) view.findViewById(R.id.add_to_filter_button);
            Button shareLocationButton = (Button) view.findViewById(R.id.share_location_button);
            String pokemonIdString = "";

            PokemonMarker pokemonMarker = null;
            for (final String o : markerList.keySet()) {
                if (markerList.get(o).getMarker().getPosition().equals(marker.getPosition())) {
                    time = markerList.get(o).getPokemon().getExpirationTimestampMs();
                    pokemonMarker = markerList.get(o);
                    pokemonName.setText(PokemonIdUtils.getLocalePokemonName(getResources(), pokemonMarker.getPokemon().getPokemonId()));
                    pokemonIdString = String.valueOf(pokemonMarker.getPokemon().getPokemonId().getNumber());
                    pokemonId.setText(Html.fromHtml("<a href=http://www.pokemon.com/uk/pokedex/" + pokemonIdString + ">#" + pokemonIdString + "</a>"));
                    pokemonId.setAutoLinkMask(Linkify.ALL);
                    if (mLocation != null && markerList.get(o).getPokemon() != null) {
                        LatLng[] locations = new LatLng[]{
                                new LatLng(mLocation.getLatitude(), mLocation.getLongitude()),
                                new LatLng(markerList.get(o).getPokemon().getLatitude(), markerList.get(o).getPokemon().getLongitude())};
                        String distanceToTravelText = "Distance From current Location : \n" + getMiles(SphericalUtil.computeDistanceBetween(locations[0], locations[1]));
                        pokemonDistance.setText(distanceToTravelText + " miles");
                    }
                    final String finalPokemonIdString1 = pokemonIdString;
                    pokemonId.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String url = String.format(getString(R.string.parameterised_pokedex_url), finalPokemonIdString1);
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            startActivity(i);
                        }
                    });
                    pokemonDescription.setText(marker.getSnippet());
                }
            }

            final String finalPokemonIdString = pokemonIdString;


            if (pokemonMarker.isIsMarked()) {
                markButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_block_red_24dp));
            } else {
                markButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_block_black_24dp));
            }


            final PokemonMarker finalPokemonMarker = pokemonMarker;
            shareLocationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (finalPokemonMarker != null) {
                        LatLng location = new LatLng(finalPokemonMarker.getPokemon().getLatitude(), finalPokemonMarker.getPokemon().getLongitude());
                        try {
                            String address = getAddress(location).replace(" ", "+");
                            String ShareSub = PokemonIdUtils.getLocalePokemonName(getResources(), finalPokemonMarker.getPokemon().getPokemonId()) + " found here";
                            String uri = ShareSub + "\n\nhttps://www.google.com/maps/place/" + address + "/@" + finalPokemonMarker.getPokemon().getLatitude() + "," + finalPokemonMarker.getPokemon().getLongitude();
                            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                            sharingIntent.setType("text/plain");
                            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, ShareSub);
                            sharingIntent.putExtra(Intent.EXTRA_TEXT, uri);
                            startActivity(Intent.createChooser(sharingIntent, "Share via"));

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            final long finalTime1 = time;
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity())
                    .setTitle("Pokemon Info")
                    .setView(view)
                    .setPositiveButton("Get Directions", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent locationTrackerIntent = new Intent(getActivity(), LocationTrackerService.class);
                            locationTrackerIntent.putExtra("TIME", finalTime1);
                            locationTrackerIntent.putExtra("LATITUDE", finalPokemonMarker.getPokemon().getLatitude());
                            locationTrackerIntent.putExtra("LONGITUDE", finalPokemonMarker.getPokemon().getLongitude());
                            getActivity().startService(locationTrackerIntent);
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

            filterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mPref.addPokemonToFilteredList(finalPokemonIdString);
                    dialog.dismiss();
//                filteredPokemon.add(String.valueOf(finalPokemonIdString));
                }
            });

            final PokemonMarker finalPokemonMarker1 = pokemonMarker;
            markButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (finalPokemonMarker1.isIsMarked()) {
                        finalPokemonMarker1.setIsMarked(false);
                        ((ImageButton) view).setImageDrawable(getResources().getDrawable(R.drawable.ic_block_black_24dp));
                    } else {
                        ((ImageButton) view).setImageDrawable(getResources().getDrawable(R.drawable.ic_block_red_24dp));
                        finalPokemonMarker1.setIsMarked(true);
                    }
                }
            });

            dialog.show();

            final long finalTime = time;
            countDownHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    final long millisLeft = finalTime - System.currentTimeMillis();
                    if (millisLeft < 0) {
                        dialog.dismiss();
                    } else {
                        if (pokemonDescription != null) {
                            LatLng[] locations = new LatLng[]{
                                    new LatLng(mLocation.getLatitude(), mLocation.getLongitude()),
                                    new LatLng(finalPokemonMarker.getPokemon().getLatitude(), finalPokemonMarker.getPokemon().getLongitude())};
                            String distanceToTravelText = "Distance From current Location : \n" + getMiles(SphericalUtil.computeDistanceBetween(locations[0], locations[1]));
                            pokemonDistance.setText(distanceToTravelText + " miles");
                            pokemonDescription.setText("Disappears in: " + getDurationBreakdown(millisLeft));

                        }
                        countDownHandler.sendEmptyMessageDelayed(COUNTDOWN_HANDLER_ID, 1000);
                    }
                    super.handleMessage(msg);
                }
            };
            countDownHandler.sendEmptyMessage(COUNTDOWN_HANDLER_ID);
        }

    }


    private String getAddress(LatLng location) throws IOException {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(getActivity(), Locale.getDefault());

        addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
        String city = addresses.get(0).getLocality();
        String state = addresses.get(0).getAdminArea();
        String countryCode = addresses.get(0).getLocale().getLanguage();
        String country = addresses.get(0).getCountryName();
        String postalCode = addresses.get(0).getPostalCode();
        String knownName = addresses.get(0).getFeatureName();
        StringBuilder stringBuilder = new StringBuilder(address);
        if (city != null) {
            stringBuilder.append("," + city);
        }
        if (postalCode != null) {
            stringBuilder.append("," + postalCode);
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        long time = 0L;
        boolean isPokemonMarker = false;
        if (userSelectedPositionMarker != null && marker.getId().equals(userSelectedPositionMarker.getId()) ||
                (searchLocationMarker != null) && marker.getId().equals(searchLocationMarker.getId())) {
            return true;
        }else if(getCustomTrackingLocationPoints().containsKey(marker.getId())){
            return false;
        }

        for (String o : markerList.keySet()) {
            if (markerList.get(o).getMarker().getPosition().equals(marker.getPosition())) {
                time = markerList.get(o).getPokemon().getExpirationTimestampMs();
                isPokemonMarker = true;
            }
        }
        final long millisLeft = time - System.currentTimeMillis();
        if (getDurationBreakdown(millisLeft).equals(EXPIRED) && isPokemonMarker) {
            marker.setVisible(false);
            marker.remove();
        }
        marker.setSnippet("Disappears in: " + getDurationBreakdown(millisLeft));
        return false;
    }

    public String getMiles(double meters) {
        DecimalFormat df = new DecimalFormat();
        return df.format(meters * 0.00062137).replace("-1", "");
    }
}

