package com.ray.pokemap.views;

import com.ray.pokemap.models.events.IEvent;

/**
 * Created by Raymond on 29/07/2016.
 */

public class GoogleLoginEvent implements IEvent {

    private boolean mIsAutoLogin;

    public GoogleLoginEvent(boolean isAutoLogin){
        mIsAutoLogin = isAutoLogin;
    }

    public boolean isIsAutoLogin() {
        return mIsAutoLogin;
    }
}
