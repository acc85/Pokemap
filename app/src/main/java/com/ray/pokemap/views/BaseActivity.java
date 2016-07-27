package com.ray.pokemap.views;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ray.pokemap.controllers.map.LocationManager;
import com.ray.pokemap.controllers.net.NianticManager;
import com.ray.pokemap.views.map.MapWrapperFragment;

/**
 * Created by vanshilshah on 19/07/16.
 */
public class BaseActivity extends AppCompatActivity {
    public static final String TAG = "BaseActivity";
    protected LocationManager.Listener locationListener;
    LocationManager locationManager;
    protected NianticManager nianticManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationManager = LocationManager.getInstance(this);
        nianticManager = NianticManager.getInstance();

    }

    @Override
    public void onResume(){
        super.onResume();
        locationManager.onResume();
        MapWrapperFragment mwp = (MapWrapperFragment)getSupportFragmentManager().findFragmentByTag(MapWrapperFragment.class.getName());
        if(mwp!=null){
            mwp.registerEventBus();
        }
        if(locationListener != null){
            locationManager.register(locationListener);
        }
    }

    @Override
    public void onPause(){
        LocationManager.getInstance(this).onPause();
//        MapWrapperFragment mwp = (MapWrapperFragment)getSupportFragmentManager().findFragmentByTag(MapWrapperFragment.class.getName());
//        if(mwp!=null){
//            mwp.setLocation(null);
//            mwp.setStaticLocation(null);
//        }
        if(locationListener != null){
            locationManager.unregister(locationListener);
        }
        super.onPause();
    }
}
