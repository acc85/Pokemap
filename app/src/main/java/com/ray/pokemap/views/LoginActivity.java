package com.ray.pokemap.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
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
import com.ray.pokemap.models.login.GoogleLoginInfo;

/**
 * A login screen that offers login via username/password. And a Google Sign in
 *
 */
public class LoginActivity extends AppCompatActivity{

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
    private GoogleManager mGoogleManager;
    private GoogleManager.LoginListener mGoogleLoginListener;

    private String mDeviceCode;
    private PokemapAppPreferences mPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNianticManager = NianticManager.getInstance();
        mGoogleManager = GoogleManager.getInstance();
        mPref = new PokemapSharedPreferences(this);


        setContentView(R.layout.activity_login);

        mNianticLoginListener = new NianticManager.LoginListener() {
            @Override
            public void authSuccessful(String authToken) {
                showProgress(false);
                Log.d(TAG, "authSuccessful() called with: authToken = [" + authToken + "]");
                mNianticManager.setPTCAuthToken(authToken);
                finishLogin();
            }

            @Override
            public void authFailed(String message) {
                Log.d(TAG, "authFailed() called with: message = [" + message + "]");
                Snackbar.make((View)mLoginFormView.getParent(), "PTC Login Failed", Snackbar.LENGTH_LONG).show();
                mProgressView.setVisibility(View.GONE);
                mLoginFormView.setAlpha(1);
                mLoginFormView.setVisibility(View.VISIBLE);
            }
        };

        mGoogleLoginListener = new GoogleManager.LoginListener() {
            @Override
            public void authSuccessful(final String authToken, String refreshToken) {
                GoogleLoginInfo info = new GoogleLoginInfo(authToken, refreshToken);
                View view = getLayoutInflater().inflate(R.layout.google_auth_remmeber_me_layout,null);
                final CheckBox rememberMeCheckbox = (CheckBox)view.findViewById(R.id.google_remember_me);
                new AlertDialog.Builder(LoginActivity.this)
                        .setView(view)
                        .setTitle("Remember Me")
                        .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mPref.setRememberMe(rememberMeCheckbox.isChecked());
                                dialogInterface.dismiss();
                                showProgress(false);
                                mPref.setRememberMeLoginType(1);
                                Log.d(TAG, "authSuccessful() called with: authToken = [" + authToken + "]");
                                mPref.setGoogleAuthToken(authToken);
                                setGoogleAuthTokenAndFinish();
                            }
                        }).create()
                        .show();
//                Log.d(TAG, "authSuccessful() called with: authToken = [" + authToken + "]");
//                mPref.setLoginInfo(info);
//                mNianticManager.setLoginInfo(LoginActivity.this, info, mNianticAuthListener);
            }

            @Override
            public void authFailed(String message) {
                showProgress(false);
                Log.d(TAG, "authFailed() called with: message = [" + message + "]");
                mPref.setRememberMe(false);
                mPref.setRememberMeLoginType(0);
                mPref.setUsername("");
                mPref.setPassword("");
                Snackbar.make((View)mLoginFormView.getParent(), "Google Login Failed", Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void authRequested(GoogleService.AuthRequest body) {
                GoogleAuthActivity.startForResult(LoginActivity.this, REQUEST_USER_AUTH,
                        body.getVerificationUrl(), body.getUserCode());
                mDeviceCode = body.getDeviceCode();
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

        mRememberMe = (CheckBox)findViewById(R.id.remember_me_check_box);

        Button signInButton = (Button) findViewById(R.id.email_sign_in_button);
        signInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                if(mRememberMe.isChecked()){
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
                mGoogleManager.authUser(mGoogleLoginListener);
            }
        });

        if(mPref.rememberMe()){
            if(mPref.getRememberMeLoginType() == PokemapSharedPreferences.GOOGLE){
                setGoogleAuthTokenAndFinish();
            }else {
                attemptPTCLogin();
            }
        }
    }

    private void setGoogleAuthTokenAndFinish() {
        mNianticManager.setGoogleAuthToken(mPref.getGoogleAuthToken());
        finishLogin();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == REQUEST_USER_AUTH){
                showProgress(true);
                mGoogleManager.requestToken(mDeviceCode, mGoogleLoginListener);
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
        String username = "";
        String password = "";
        if(!mPref.getUsername().isEmpty()){
            username = mPref.getUsername();
            password = mPref.getPassword();
        }else{
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

    private void finishLogin(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
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

