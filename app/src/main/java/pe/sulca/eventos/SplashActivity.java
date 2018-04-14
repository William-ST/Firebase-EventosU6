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
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import io.fabric.sdk.android.Fabric;

import static pe.sulca.eventos.Comun.acercaDe;
import static pe.sulca.eventos.Comun.colorFondo;
import static pe.sulca.eventos.Comun.mFirebaseRemoteConfig;
import static pe.sulca.eventos.PreferenceUtil.RC_FIRST_TIME;
import static pe.sulca.eventos.PreferenceUtil.RC_NOT;
import static pe.sulca.eventos.PreferenceUtil.RC_YES;

public class SplashActivity extends AppCompatActivity {

    private final String TAG = SplashActivity.class.getCanonicalName();

    TextView tvMessage;
    Button btnStart;

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        initializeGUI();
        initialize();

    }

    private void initializeGUI() {
        tvMessage = findViewById(R.id.tv_message);
        btnStart = findViewById(R.id.btn_start);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    private void initialize() {
        handler = new Handler(Looper.getMainLooper());

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
                btnStart.setVisibility(View.VISIBLE);
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
}
