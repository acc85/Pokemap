package com.ray.pokemap.models.events;

/**
 * Created by Raymond on 29/07/2016.
 */

public class LoginFailedEvent implements IEvent {


    private String mErrorMessage;

    public LoginFailedEvent (String message) {
        mErrorMessage = message;
    }

    public String getmErrorMessage() {
        return mErrorMessage;
    }

    public void setmErrorMessage(String mErrorMessage) {
        this.mErrorMessage = mErrorMessage;
    }
}
