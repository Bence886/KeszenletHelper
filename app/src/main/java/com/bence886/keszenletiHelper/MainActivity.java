package com.bence886.keszenletiHelper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseRemoteConfig firebaseRemoteConfig;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        FirebaseRemoteConfigSettings.Builder configBuilder = new FirebaseRemoteConfigSettings.Builder();
        if (BuildConfig.DEBUG)
        {
            long cacheInterval = 0;
            configBuilder.setMinimumFetchIntervalInSeconds(cacheInterval);
        }
        // finally build config settings and sets to Remote Config
        firebaseRemoteConfig.setConfigSettingsAsync(configBuilder.build());
        firebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
    }

    @Override
    protected void onStart() {
        super.onStart();

        TextView version = (TextView) findViewById(R.id.ver);
        version.setText("Version: " + BuildConfig.VERSION_NAME);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            }, 0);
        }

        try (FileInputStream fis = getApplicationContext().openFileInput("userData")) {
            InputStreamReader inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8);
            StringBuilder stringBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                deviceId = reader.readLine();
            } catch (IOException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        } catch (IOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            try (FileOutputStream fos = getApplicationContext().openFileOutput("userData", Context.MODE_PRIVATE)) {
                fos.write(deviceId.getBytes());
            } catch (IOException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }

        mFirebaseAnalytics.setUserId(deviceId);

        fetchRemoteDelay();
    }

    public void onUidClicked(View v) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Uid", deviceId);
        clipboard.setPrimaryClip(clip);

        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, deviceId, Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int i = 0; i < permissions.length; i++) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, permissions[i]);
            bundle.putString(FirebaseAnalytics.Param.CONTENT, (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "PERMISSION_GRANTED" : "PERMISSION_DENIED"));
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.POST_SCORE, bundle);
        }
    }

    private void fetchRemoteDelay()
    {
        firebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            boolean updated = task.getResult();
                            Toast.makeText(MainActivity.this, "Konfiguráció frissítve.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}