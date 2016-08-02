package com.ray.pokemap.util;

import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.ray.pokemap.controllers.app_preferences.PokemapSharedPreferences;
import com.ray.pokemap.controllers.net.NianticManager;
import com.ray.pokemap.models.events.CatchablePokemonEvent;
import com.ray.pokemap.models.events.ServerUnreachableEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Raymond on 02/08/2016.
 */

public class BackgroundServiceUtil {


    public static final int TIMER_HANDLER_ID = 9010;
    public static final String EXPIRED = "expired";
    public static final int CHECK_TIMER_HANDLER_ID = 9011;
    public static final int TYPE_CHECK_HANDLER_ID = 9012;
    private Map<String, CatchablePokemon> persistentPokemonMarkerMap;
    private static BackgroundServiceUtil ourInstance = new BackgroundServiceUtil();
    private Handler timerHandler;
    private long timer;

    private int STEPS = 100;
    private int x = 0;
    private int y = 0;
    private int dx;
    private int dy;
    private int currentStep;
    private int STEPS2 = STEPS * STEPS;

    private int INITIAL_STEPS;
    private int SCAN_INTERVAL;
    private boolean reset;
    private Handler checkTimerHandler;

    private int TRACKING_TYPE;
    private Map<String, Marker> customTrackingLocationPoints;
    private Location mLocationToCheck;
    private Location mStaticLocation;
    private Location mLocation;
    private int currentCustomLocationArrayPos;
    private Handler typeChecker;
    private boolean RUNNING;

    public static BackgroundServiceUtil getInstance() {

        return ourInstance;
    }

    private BackgroundServiceUtil() {

    }

    public void stopService(){
        RUNNING = false;
        EventBus.getDefault().unregister(this);
    }

    public void startBackgroundPokemonService(Location staticLocation,
                                              Location locationToCheck,
                                              Location location,
                                              int initialSteps,
                                              int scan_interval,
                                              int tracking_type,
                                              int x,
                                              int y,
                                              int dx,
                                              int dy,
                                              int currentStep,
                                              Map<String, Marker> customTrackingLocationPoints,
                                              int currentCustomLocationArrayPos) {
        persistentPokemonMarkerMap = new HashMap<>();
        mStaticLocation = staticLocation;
        mLocationToCheck = locationToCheck;
        mLocation = location;
        INITIAL_STEPS = initialSteps;
        STEPS = INITIAL_STEPS;
        SCAN_INTERVAL = scan_interval;
        TRACKING_TYPE = tracking_type;
        this.currentCustomLocationArrayPos = currentCustomLocationArrayPos;
        this.x = x;
        this.y = y;
        this.dy = dy;
        this.dx = dx;
        this.currentStep = currentStep;
        this.customTrackingLocationPoints = customTrackingLocationPoints;
        RUNNING = true;
        EventBus.getDefault().register(this);
        NianticManager.getInstance().getMapInformation(mLocationToCheck.getLatitude(), mLocationToCheck.getLongitude(), 0D);
    }


    public int getCurrentCustomLocationArrayPos() {
        return currentCustomLocationArrayPos;
    }

    public long getTimer() {
        return timer;
    }

    public int getSTEPS() {
        return STEPS;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public boolean isReset() {
        return reset;
    }


    public Location getStaticLocation() {
        return mStaticLocation;
    }

    public Location getLocation() {
        return mLocation;
    }

    public Map<String,CatchablePokemon> getPokemonList(){
        return persistentPokemonMarkerMap;
    };

    public Location getLocationToCheck() {
        return mLocationToCheck;
    }

    /**
     * Called whenever a CatchablePokemonEvent is posted to the bus. Posted when new catchable pokemon are found.
     *
     * @param event The event information
     */
    @Subscribe
    public void onEvent(CatchablePokemonEvent event) {
        if(RUNNING) {
            final HandlerThread timerThread = new HandlerThread("TIMER_THREAD");
            timerThread.start();
            final int interval = SCAN_INTERVAL;
            timerHandler = new Handler(timerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    timer++;
                    if (timer < interval) {
                        timerHandler.sendEmptyMessageDelayed(TIMER_HANDLER_ID, 1000);
                    } else {
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
                if (currentStep != STEPS2 && STEPS2 / INITIAL_STEPS == INITIAL_STEPS) {
                    currentStep += 1;
                } else {
                    resetStepsPosition();
                }
                final HandlerThread handlerThread = new HandlerThread("TIMER_CHECK_THREAD");
                handlerThread.start();
                checkTimerHandler = new Handler(handlerThread.getLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        if (timer < interval) {
                            checkTimerHandler.sendEmptyMessage(CHECK_TIMER_HANDLER_ID);
                        } else {
                            timer = 0;
                            if (TRACKING_TYPE == PokemapSharedPreferences.CUSOTM_LOCATION_POINTS_TRACKING) {
                                if (getCustomTrackingLocationPoints().size() > 1) {
                                    currentCustomLocationArrayPos++;
                                }
                            }
                            handlerThread.quit();
                            System.out.println("size:" + persistentPokemonMarkerMap.size());
                            getNextLocation();
                        }
                        super.handleMessage(msg);
                    }
                };
                checkTimerHandler.sendEmptyMessage(CHECK_TIMER_HANDLER_ID);
            } else {
                mLocation = null;
            }
        }
    }

    public Map<String, Marker> getCustomTrackingLocationPoints() {
        if (customTrackingLocationPoints == null) {
            customTrackingLocationPoints = new LinkedHashMap<>();
        }
        return customTrackingLocationPoints;
    }


    public boolean isRunning(){
        return RUNNING;
    }

    private void getNextLocation() {
        if (TRACKING_TYPE == PokemapSharedPreferences.LOCATION_TRACKING) {
            getCustomTrackingLocationPoints().clear();
            customTrackingLocationPoints.clear();
            performLocationTracking();
        } else if (TRACKING_TYPE == PokemapSharedPreferences.FOLLOW_TRACKING) {
            customTrackingLocationPoints.clear();
            performFollowTracking();
        } else if (TRACKING_TYPE == PokemapSharedPreferences.CUSOTM_LOCATION_POINTS_TRACKING) {
            performCustomPointTracking();
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ServerUnreachableEvent event) {
        continueMapInfoGatherer();
    }

    public void continueMapInfoGatherer() {
        NianticManager.getInstance().getMapInformation(mLocationToCheck.getLatitude(), mLocationToCheck.getLongitude(), 0.0);
    }


    private void performCustomPointTracking() {
        resetStepsPosition();
        if (!getCustomTrackingLocationPoints().isEmpty()) {
            if (currentCustomLocationArrayPos >= getCustomTrackingLocationPoints().size()) {
                currentCustomLocationArrayPos = 0;
            }
            Marker markerPoint = (Marker) getCustomTrackingLocationPoints().values().toArray()[currentCustomLocationArrayPos];
            LatLng location = markerPoint.getPosition();
            mLocationToCheck.setLatitude(location.latitude);
            mLocationToCheck.setLongitude(location.longitude);
            if (currentCustomLocationArrayPos >= getCustomTrackingLocationPoints().size()) {
                currentCustomLocationArrayPos = 0;
            }
            NianticManager.getInstance().getMapInformation(mLocationToCheck.getLatitude(), mLocationToCheck.getLongitude(), 0D);
        } else {
            getTrackerTypeHandler().sendEmptyMessage(TYPE_CHECK_HANDLER_ID);
        }
    }

    public Handler getTrackerTypeHandler() {
        if (typeChecker == null) {
            HandlerThread thread = new HandlerThread("TYPE CHECKER THREAD");
            thread.start();
            typeChecker = new Handler(thread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (TRACKING_TYPE == PokemapSharedPreferences.LOCATION_TRACKING
                            || TRACKING_TYPE == PokemapSharedPreferences.FOLLOW_TRACKING
                            || (TRACKING_TYPE == PokemapSharedPreferences.CUSOTM_LOCATION_POINTS_TRACKING && !getCustomTrackingLocationPoints().isEmpty())) {

                        getNextLocation();
                    } else {
                        typeChecker.sendEmptyMessage(TYPE_CHECK_HANDLER_ID);
                    }
                }
            };

        }
        return typeChecker;
    }

    private void performFollowTracking() {
        NianticManager.getInstance().getMapInformation(mLocation.getLatitude(), mLocation.getLongitude(), 0D);
    }


    private void performLocationTracking() {
        if (mStaticLocation != null) {
            if (!reset) {
                if (x > (-STEPS2 / 2) && x <= STEPS2 / 2 && y > (-STEPS2 / 2) && y <= (STEPS2 / 2)) {
                    getOrInitialiseLocationToCheck().setLatitude(x * 0.0025 + mStaticLocation.getLatitude());
                    getOrInitialiseLocationToCheck().setLongitude(y * 0.0025 + mStaticLocation.getLongitude());
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
                NianticManager.getInstance().getMapInformation(mStaticLocation.getLatitude(), mStaticLocation.getLongitude(), 0D);
            }
        }
    }

    private Location getOrInitialiseLocationToCheck() {
        if (mLocationToCheck == null) {
            mLocationToCheck = new Location("dummyprovider");
        }
        return mLocationToCheck;
    }


    public void resetStepsPosition() {
        x = 0;
        y = 0;
        dx = 0;
        dy = -1;
        STEPS = INITIAL_STEPS;
        STEPS2 = STEPS * STEPS;
        currentStep = 0;
        reset = true;
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


}
