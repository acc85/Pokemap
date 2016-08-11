package com.ray.pokemap.views;

import com.ray.pokemap.models.events.IEvent;

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass;

/**
 * Created by Raymond on 29/07/2016.
 */

public class GoogleLoginEvent implements IEvent {

    private boolean mIsAutoLogin;
    private RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo mAuthInfo;

    public GoogleLoginEvent(RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo authInfo, boolean isAutoLogin){
        mAuthInfo = authInfo;
        mIsAutoLogin = isAutoLogin;
    }

    public boolean isIsAutoLogin() {
        return mIsAutoLogin;
    }
}
