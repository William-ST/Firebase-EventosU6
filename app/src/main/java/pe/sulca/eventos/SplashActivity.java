package pe.sulca.eventos;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import io.fabric.sdk.android.Fabric;

import static pe.sulca.eventos.Comun.acercaDe;
import static pe.sulca.eventos.Comun.colorFondo;
import static pe.sulca.eventos.Comun.mFirebaseRemoteConfig;
import static pe.sulca.eventos.PreferenceUtil.RC_FIRST_TIME;
import static pe.sulca.eventos.PreferenceUtil.RC_NOT;
import static pe.sulca.eventos.PreferenceUtil.RC_YES;

public class SplashActivity extends AppCompatActivity {

    private final String TAG = SplashActivity.class.getCanonicalName();

    LinearLayout llActions;
    TextView tvMessage;
    Button btnStart;

    Handler handler;
    CallbackManager callbackManager;
    LoginButton loginButton;

    TwitterLoginButton botonLoginTwitter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Twitter.initialize(new TwitterConfig.Builder(this)
                .logger(new DefaultLogger(Log.DEBUG))
                .twitterAuthConfig(new TwitterAuthConfig(getString(R.string.CONSUMER_KEY),
                        getString(R.string.CONSUMER_SECRET)))
                .debug(true)
                .build());

        setContentView(R.layout.activity_splash);

        initializeGUI();
        initialize();

    }

    private void initializeGUI() {
        llActions = findViewById(R.id.ll_actions);
        tvMessage = findViewById(R.id.tv_message);
        btnStart = findViewById(R.id.btn_start);
        loginButton = findViewById(R.id.login_button);
        //loginButton.setPublishPermissions("publish_actions");
        loginButton.setReadPermissions("email");
        loginButton.setReadPermissions("publish_actions");

        //botonEnviarATwitter = findViewById(R.id.boton_EnviarATwitter);
        botonLoginTwitter = findViewById(R.id.twitter_login_button);
        botonLoginTwitter.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                Toast.makeText(SplashActivity.this, "Autenticado en twitter: " + result.data.getUserName(), Toast.LENGTH_LONG).show();
                twitterAuth(result.data);
            }

            @Override
            public void failure(TwitterException e) {
                Toast.makeText(SplashActivity.this, "Fallo en autentificación: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    private void twitterAuth(TwitterSession session) {

    }

    private void initialize() {
        handler = new Handler(Looper.getMainLooper());
        callbackManager = CallbackManager.Factory.create();

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings
                .Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);

        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_default);

        long cacheExpiration = 3600;

        if (BuildConfig.DEBUG) {
            cacheExpiration = 0;
        }

        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mFirebaseRemoteConfig.activateFetched();
                        getColorFondo();
                        getAcercaDe();
                        processRemote();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        colorFondo = mFirebaseRemoteConfig.getString("color_fondo");
                        acercaDe = mFirebaseRemoteConfig.getBoolean("acerca_de");
                        processRemote();
                    }
                });

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                boolean loggedIn = AccessToken.getCurrentAccessToken() != null;
                if (loggedIn) {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                }
            }

            @Override
            public void onCancel() {
                // App code

            }

            @Override
            public void onError(FacebookException exception) {
                // App code

            }
        });

        showEnableReportCrashDialog();
    }

    private void showEnableReportCrashDialog() {
        if (PreferenceUtil.getReportCrashesStatus(SplashActivity.this) == RC_FIRST_TIME) {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Recopilación de errores");
            alertDialog.setMessage("¿Desea proporcionar el informe de errores de la aplicación?\nEsto mejoraría su experiencia en la aplicación");
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "SI", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    PreferenceUtil.setReportCrashesStatus(SplashActivity.this, RC_YES);
                    Fabric.with(SplashActivity.this, new Crashlytics());
                    Crashlytics.log(Log.VERBOSE, "screen", "MainActivity");
                    Crashlytics.setString("LOGIN_STATUS", "logged_out");
                    dialog.dismiss();
                }
            });
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    PreferenceUtil.setReportCrashesStatus(SplashActivity.this, RC_NOT);
                    dialog.dismiss();
                }
            });
            alertDialog.show();
        }
    }

    private void processRemote() {

        final boolean knowSpain = mFirebaseRemoteConfig.getBoolean("know_spain");
        if (knowSpain) {
            tvMessage.setText("Esta aplicación muestra información sobre eventos");
        } else {
            tvMessage.setText("Esta aplicación muestra eventos de España");
        }

        final long enablePerformanceMonitoring = mFirebaseRemoteConfig.getLong("performance_monitoring");
        if (enablePerformanceMonitoring == 0L) {
            Log.d(TAG, "performance_monitoring enable false");
            FirebasePerformance.getInstance().setPerformanceCollectionEnabled(false);
        } else {
            Log.d(TAG, "performance_monitoring enable true");
            FirebasePerformance.getInstance().setPerformanceCollectionEnabled(true);
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                llActions.setVisibility(View.VISIBLE);
            }
        }, 1000);
    }

    private void getColorFondo() {
        colorFondo = mFirebaseRemoteConfig.getString("color_fondo");
    }

    private void getAcercaDe() {
        Log.d(TAG, "getAcercaDe 1");
        acercaDe = mFirebaseRemoteConfig.getBoolean("acerca_de");
        Log.d(TAG, "getAcercaDe 2: " + acercaDe);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, data.toString());
        callbackManager.onActivityResult(requestCode, resultCode, data);
        botonLoginTwitter.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

}
