package com.ray.pokemap.controllers.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.ray.pokemap.controllers.net.NianticManager;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * Created by Raymond on 02/08/2016.
 */

public class PokemonBackgroundScanningService extends Service {

    public static final String STEPS_KEY = "STEPS";
    public static final String X_KEY = "X";
    public static final String Y_KEY = "Y";
    public static final String DX_KEY = "DX";
    public static final String DY_KEY = "DY";
    public static final String STEPS_2_KEY = "STEPS2";
    public static final String CURRENT_STEP_KEY = "CURRENT_STEP";
    private int STEPS = 100;
    private List<CatchablePokemon> catchablePokemons;

    private int INITIAL_STEPS;
    private int x = 0;

    private int y = 0;

    private int dx;
    private int dy;

    private int currentStep;

    private int STEPS2;
    private boolean reset;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        EventBus.getDefault().register(this);
        INITIAL_STEPS = intent.getIntExtra(STEPS_KEY,100);
        x = intent.getIntExtra(X_KEY, 0);
        y = intent.getIntExtra(Y_KEY, 0);
        dx = intent.getIntExtra(DX_KEY, 0);
        dy = intent.getIntExtra(DY_KEY, -1);
        STEPS2 = intent.getIntExtra(STEPS_2_KEY, -1);;
        currentStep = intent.getIntExtra(CURRENT_STEP_KEY,0);
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    public void resetStepsPosition() {
        x = 0;
        y = 0;
        dx = 0;
        dy = -1;
        STEPS =
        STEPS2 = STEPS * STEPS;
        currentStep = 0;
        reset = true;
    }
}
