package com.ray.pokemap.trackingService;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.ray.pokemap.R;
import com.ray.pokemap.controllers.app_preferences.PokemapAppPreferences;
import com.ray.pokemap.controllers.app_preferences.PokemapSharedPreferences;

class LocationHeadView extends View {

    private Context mContext;
    private FrameLayout mTrackingView;
    private WindowManager mWindowManager;
    private TextView mDistanceView;
    private TextView mTimeView;
    private WindowManager.LayoutParams mDetailsLayoutParams;
    private PokemapAppPreferences mPref;

    LocationHeadView(Context context) {
        super(context);
        mContext = context;
        mTrackingView = new FrameLayout(mContext);
        mPref =  new PokemapSharedPreferences(context.getApplicationContext());
        addToWindowManager();

    }

    void updateDistance(String distance) {
        mDistanceView.setText(distance);
    }


    private void addTrackingViewTouchListeer() {
        // Support dragging the image view
        mTrackingView.setOnTouchListener(new OnTouchListener() {
            private int initY;
            private int initTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int y = (int) event.getRawY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initY = mDetailsLayoutParams.y;
                        initTouchY = y;
                        return true;

                    case MotionEvent.ACTION_UP:
                        mPref.setLastKnownWidgetYPosition(mDetailsLayoutParams.y);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        mDetailsLayoutParams.y = initY + (y - initTouchY);
                        mWindowManager.updateViewLayout(mTrackingView, mDetailsLayoutParams);
                        return true;
                }
                return false;
            }
        });
    }


    void updateTime(String time) {
        mTimeView.setText(time);
    }

    private void addToWindowManager() {
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        addLayoutsToFrames();
        createTopNavLayout();
        addTrackingViewTouchListeer();
    }

    private void addLayoutsToFrames() {
        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // Here is the place where you can inject whatever layout you want.
        layoutInflater.inflate(R.layout.location_tracking_layout, mTrackingView);
    }

    /**
     * Removes the view from window manager.
     */
    void destroy() {
        mWindowManager.removeView(mTrackingView);
    }


    private void createTopNavLayout() {
        mDetailsLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_TOAST,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        mDetailsLayoutParams.gravity = Gravity.START | Gravity.TOP;
        mDetailsLayoutParams.y = mPref.getLastKnownWidgetYPosition();
        mDistanceView = (TextView) mTrackingView.findViewById(R.id.distanceFromTarget);
        mTimeView = (TextView) mTrackingView.findViewById(R.id.timeToExpiration);
        mWindowManager.addView(mTrackingView, mDetailsLayoutParams);
    }
}
