package com.ray.pokemap.trackingService;

/**
 * Created by Raymond on 16/07/2016.
 */

public interface LocationServiceCallback {

    void updateLocation(String time);

    void updateDistance(String distance);

    void shutdown();
}
