package com.ray.pokemap.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.SignInButton;
import com.ray.pokemap.R;
import com.ray.pokemap.controllers.app_preferences.PokemapAppPreferences;
import com.ray.pokemap.controllers.app_preferences.PokemapSharedPreferences;
import com.ray.pokemap.controllers.net.GoogleManager;
import com.ray.pokemap.controllers.net.GoogleService;
import com.ray.pokemap.controllers.net.NianticManager;
import com.ray.pokemap.models.events.LoginEventResult;
import com.ray.pokemap.models.events.LoginFailedEvent;
import com.ray.pokemap.models.events.RetryEvent;
import com.ray.pokemap.views.settings.AutoLoggingFailedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * A login screen that offers login via username/password. And a Google Sign in
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private static final int REQUEST_USER_AUTH = 1;

    // UI references.
    private AutoCompleteTextView mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private CheckBox mRememberMe;
    private NianticManager mNianticManager;
    private NianticManager.LoginListener mNianticLoginListener;
    private GoogleManager.LoginListener mGoogleLoginListener;
    private GoogleManager mGoogleManager;
    private PokemapAppPreferences mPref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EventBus.getDefault().register(this);
        mNianticManager = NianticManager.getInstance();
        mGoogleManager = GoogleManager.getInstance();
        mPref = new PokemapSharedPreferences(getApplicationContext());

        setContentView(R.layout.activity_login);

        mNianticLoginListener = new NianticManager.LoginListener() {
            @Override
            public void authSuccessful(String authToken) {
                showProgress(false);
                Log.d(TAG, "authSuccessful() called with: authToken = [" + authToken + "]");
                mNianticManager.setPTCAuthToken(authToken);
                mPref.setLoginType(PokemapSharedPreferences.PTC);
                finishLogin();
            }

            @Override
            public void authFailed(String message) {
                Log.d(TAG, "authFailed() called with: message = [" + message + "]");
                Snackbar.make((View) mLoginFormView.getParent(), "PTC Login Failed", Snackbar.LENGTH_LONG).show();
                mProgressView.setVisibility(View.GONE);
                mLoginFormView.setAlpha(1);
                mLoginFormView.setVisibility(View.VISIBLE);
            }
        };


        mGoogleLoginListener = new GoogleManager.LoginListener() {
            @Override
            public void authSuccessful(String authToken) {
                mPref.setGoogleAuthToken(authToken);
                mPref.setUsername(mUsernameView.getText().toString());
                mPref.setPassword(mPasswordView.getText().toString());
                setGoogleAuthTokenAndFinish(false);
            }

            @Override
            public void authFailed(String message) {
                Snackbar.make(findViewById(R.id.login_root_layout), "Login failed, check credentials", Snackbar.LENGTH_SHORT).show();
                showProgress(false);
//                Snackbar.make(findViewById(R.id.login_root_layout), "Login failed, retrying", Snackbar.LENGTH_SHORT).show();
//                mGoogleManager.googleAuthAttempt(mUsernameView.getText().toString(), mPasswordView.getText().toString(), this);
            }

            @Override
            public void authRequested(GoogleService.AuthRequest body) {

            }
        };
        //Bold words in Warning
        TextView warning = (TextView) findViewById(R.id.login_warning);
        String text = getString(R.string.login_warning) + " <b>banned</b>.";
        warning.setText(Html.fromHtml(text));

        // Set up the login form.
        mUsernameView = (AutoCompleteTextView) findViewById(R.id.username);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptPTCLogin();
                    return true;
                }
                return false;
            }
        });

        mRememberMe = (CheckBox) findViewById(R.id.remember_me_check_box);

        Button signInButton = (Button) findViewById(R.id.email_sign_in_button);
        signInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                if (mRememberMe.isChecked()) {
                    mPref.setRememberMe(true);
                    mPref.setRememberMeLoginType(0);
                }
                mPref.setPassword(mPasswordView.getText().toString());
                mPref.setUsername(mUsernameView.getText().toString());
                attemptPTCLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        SignInButton signInButtonGoogle = (SignInButton) findViewById(R.id.sign_in_button);
        signInButtonGoogle.setSize(SignInButton.SIZE_WIDE);
        signInButtonGoogle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                loginWithGoogle();
            }
        });

        checkAndPerformAutoLogin();
    }

    private void checkAndPerformAutoLogin() {
        if (mPref.rememberMe()) {
            showProgress(true);
            if (mPref.getRememberMeLoginType() == PokemapSharedPreferences.GOOGLE) {
                setGoogleAuthTokenAndFinish(true);
            } else {
                attemptPTCLogin();
            }
        }
    }


    /**
     * Called whenever a LoginEventResult is posted to the bus. Originates from LoginTask.java
     *
     * @param result Results of a log in attempt
     */
    @Subscribe
    public void onEvent(LoginEventResult result) {
        if (result.isLoggedIn()) {
            mPref.setPassword(mPasswordView.getText().toString());
            mPref.setUsername(mUsernameView.getText().toString());
            if (mRememberMe.isChecked()) {
                mPref.setRememberMe(true);
                mPref.setRememberMeLoginType(1);
            }
            Snackbar.make(findViewById(R.id.root), "You have logged in successfully.", Snackbar.LENGTH_LONG).show();
            finishLogin();
        }
    }

    /**
     * Called whenever a LoginEventResult is posted to the bus. Originates from LoginTask.java
     *
     * @param result Results of a log in attempt
     */
    @Subscribe
    public void onEvent(GoogleLoginEvent result) {
        if(!result.isIsAutoLogin()) {
            mPref.setPassword(mPasswordView.getText().toString());
            mPref.setUsername(mUsernameView.getText().toString());
        }
        if (mRememberMe.isChecked()) {
            mPref.setRememberMe(true);
            mPref.setRememberMeLoginType(1);
        }
        mPref.setLoginType(PokemapSharedPreferences.GOOGLE);
        try {
            Snackbar.make(findViewById(R.id.root), "You have logged in successfully.", Snackbar.LENGTH_LONG).show();
        }catch (Exception e){
            //empty
        }
        finishLogin();
    }


    /**
     * Called whenever a LoginEventResult is posted to the bus. Originates from LoginTask.java
     *
     * @param result Results of a log in attempt
     */
    @Subscribe
    public void onEvent(RetryEvent result) {
        Snackbar.make(findViewById(R.id.login_root_layout), "Server error. Retrying connection", Snackbar.LENGTH_SHORT).show();
        setGoogleAuthTokenAndFinish(false);
    }


    /**
     * Called whenever a LoginEventResult is posted to the bus. Originates from LoginTask.java
     *
     * @param m Results of a log in attempt
     */
    @Subscribe
    public void onEvent(LoginFailedEvent m) {
        Snackbar.make(findViewById(R.id.root), "You have failed to log in.", Snackbar.LENGTH_LONG).show();
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                showProgress(false);
            }
        });

    }

    /**
     * Called whenever a LoginEventResult is posted to the bus. Originates from LoginTask.java
     *
     * @param a Results of a log in attempt
     */
    @Subscribe
    public void onEvent(AutoLoggingFailedEvent a) {
        if (!mPref.getUsername().isEmpty() && !mPref.getPassword().isEmpty()) {
            mGoogleManager.googleAuthAttempt(mPref.getUsername(), mPref.getPassword(), mGoogleLoginListener);
            Snackbar.make(findViewById(R.id.login_root_layout), "Auto logging failed. Retrying with credentials", Snackbar.LENGTH_SHORT).show();
        } else {
            System.out.println("Failed auto login");
            mRememberMe.setChecked(false);
            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    showProgress(false);
                }
            });

        }
    }

    private void setGoogleAuthTokenAndFinish(boolean autoLogging) {
        mNianticManager.setGoogleAuthToken(mPref.getGoogleAuthToken(), autoLogging);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_USER_AUTH:
                    showProgress(true);
                    break;

            }
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptPTCLogin() {
        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);
        String username;
        String password;
        if (!mPref.getUsername().isEmpty()) {
            username = mPref.getUsername();
            password = mPref.getPassword();
        } else {
            // Store values at the time of the login attempt.
            username = mUsernameView.getText().toString();
            password = mPasswordView.getText().toString();
        }


        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mNianticManager.login(username, password, mNianticLoginListener);

        }
    }

    private void loginWithGoogle() {

        mUsernameView.setError(null);
        mPasswordView.setError(null);
        String username;
        String password;
        // Store values at the time of the login attempt.
        username = mUsernameView.getText().toString();
        password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mGoogleManager.googleAuthAttempt(username, password, mGoogleLoginListener);
        }
    }

    private void finishLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}

