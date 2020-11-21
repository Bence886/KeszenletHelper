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
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private String deviceId;
    private boolean testInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        testInProgress = false;

        final Button button = findViewById(R.id.test);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               if (testInProgress)
               {
                   Toast toast = Toast.makeText(getApplicationContext(), "Próbariasztás már folyamatban.", 10);
                   toast.show();
               }else{
                   testInProgress = true;
                   Handler handler = new Handler();
                   handler.postDelayed(new Runnable() {
                       @Override
                       public void run() {
                           testInProgress = false;
                           SMSReceiver.OpenIpolymentokApp(getApplicationContext(), "", "<riasztasfigyeloteszt>");
                       }
                   }, 10000);
                   Toast toast = Toast.makeText(getApplicationContext(), "Próbariasztás 10 másodperc múlva.", 10);
                   toast.show();
               }
            }
        });
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
            }
        } catch (IOException e) {
        }

        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            try (FileOutputStream fos = getApplicationContext().openFileOutput("userData", Context.MODE_PRIVATE)) {
                fos.write(deviceId.getBytes());
            } catch (IOException e) {
            }
        }
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
    }
}