package com.ray.pokemap.trackingService;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.ray.pokemap.views.MainActivity;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class LocationTrackerService extends Service {


    private final static int FOREGROUND_ID = 999;
    private static final String EXPIRED = "EXPIRED";
    private double lat1;
    private double lon1;

    private LocationHeadView mHeadLayer;
    private Location startLocation;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private BroadcastReceiver broadcastReceiver;
    private long expirationTime;
    private Handler handler;
    private double startLatitude;
    private double startLongitude;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logServiceStarted();

        initHeadLayer();
        expirationTime = intent.getLongExtra("TIME",0);
        startLongitude = intent.getDoubleExtra("LONGITUDE",0);
        startLatitude = intent.getDoubleExtra("LATITUDE",0);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equalsIgnoreCase("STOP")){
                    stopSelf();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("STOP");
        registerReceiver(broadcastReceiver,intentFilter);
//        PendingIntent pendingIntent = createPendingIntent();
//        Notification notification = createNotification(pendingIntent);
//        startForeground(FOREGROUND_ID, notification);
        initialiseLocationManager();

        return START_STICKY;
    }

    private void getLocationInMetersAndDisplay(Location location) {
        Location loc2 = new Location("");
        loc2.setLatitude(location.getLatitude());
        loc2.setLongitude(location.getLongitude());
        float distanceInMeters = startLocation.distanceTo(loc2);
        LatLng start = new LatLng(startLocation.getLatitude(),startLocation.getLongitude());
        LatLng end = new LatLng(location.getLatitude(),location.getLongitude());
        if(mHeadLayer != null) {
            mHeadLayer.updateDistance(getMiles(SphericalUtil.computeDistanceBetween(start,end))+" miles");
            System.out.println("long:" + location.getLongitude() + ", lat:" + location.getLatitude());
        }
    }

    private void initialiseLocationManager() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                System.out.println("changed");
                getLocationInMetersAndDisplay(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.\
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1,1,locationListener);
        setNewCurrentLocation();
    }

    public void setNewCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.\
            return;
        }
        Location currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(currentLocation == null){
            currentLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        }
        startLocation = new Location("reverseGeocoded");
        startLocation.setLatitude(startLatitude);
        startLocation.setLongitude(startLongitude);
        getLocationInMetersAndDisplay(currentLocation);
    }

    @Override
    public void onDestroy() {
        destroyHeadLayer();
        stopForeground(true);

        logServiceEnded();
    }

    private void initHeadLayer() {
        mHeadLayer = new LocationHeadView(this);
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                final long millisLeft = expirationTime - System.currentTimeMillis();
                String timeToExpire = getDurationBreakdown(millisLeft);
                if(!getDurationBreakdown(expirationTime).equalsIgnoreCase(EXPIRED)) {
                    if (mHeadLayer != null) {
                        mHeadLayer.updateTime(timeToExpire);
                    }
                    handler.sendEmptyMessageDelayed(2010,1000);
                }

            }
        };
        handler.sendEmptyMessage(2010);

    }

    private static String getDurationBreakdown(long millis) {
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

    private void destroyHeadLayer() {
        mHeadLayer.destroy();
        mHeadLayer = null;
    }

    private PendingIntent createPendingIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        return PendingIntent.getActivity(this, 0, intent, 0);
    }

//    private Notification createNotification(PendingIntent intent) {
//        return new Notification.Builder(this)
//                .setContentTitle(getText(R.string.notificationTitle))
//                .setContentText(getText(R.string.notificationText))
//                .setSmallIcon(R.drawable.ic_launcher)
//                .setContentIntent(intent)
//                .build();
//    }

    private void logServiceStarted() {
        Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();
    }

    private void logServiceEnded() {
        Toast.makeText(this, "Service ended", Toast.LENGTH_SHORT).show();
    }

    private void logBeaconSent(){
        Toast.makeText(this, "Beacon Sent", Toast.LENGTH_SHORT).show();
    }

    private double distance_between(Location l1, Location l2) {
        double lat1=l1.getLatitude();
        double lon1=l1.getLongitude();
        double lat2=l2.getLatitude();
        double lon2=l2.getLongitude();
        double R = 6371; // km
        double dLat = (lat2-lat1)* Math.PI/180;
        double dLon = (lon2-lon1)* Math.PI/180;
        lat1 = lat1* Math.PI/180;
        lat2 = lat2* Math.PI/180;

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c * 1000;

        System.out.println("dist betn "+
                d + " " +
                l1.getLatitude()+ " " +
                l1.getLongitude() + " " +
                l2.getLatitude() + " " +
                l2.getLongitude()
        );

        return d;
    }

    public String getMiles(double meters){
        DecimalFormat df = new DecimalFormat();
        return df.format(meters * 0.00062137).replace("-1","");
    }
}
