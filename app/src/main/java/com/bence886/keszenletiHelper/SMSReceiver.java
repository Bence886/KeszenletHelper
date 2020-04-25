package com.bence886.keszenletiHelper;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.telephony.SmsMessage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;


import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class SMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs;
        String strMessage = "";
        String format = null;
        if (bundle != null) {
            format = bundle.getString("format");
        }
        Object[] pdus = new Object[0];
        if (bundle != null) {
            pdus = (Object[]) bundle.get("pdus");
        }
        if (pdus != null) {
            boolean isVersionM = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
            msgs = new SmsMessage[pdus.length];
            for (int i = 0; i < msgs.length; i++) {
                // Check Android version and use appropriate createFromPdu.
                if (isVersionM) {
                    // If Android version M or newer:
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                } else {
                    // If Android version L or older:
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                }
            }

            String msmmsg = "";
            for (int i = 0; i < msgs.length; i++) {
                msmmsg += msgs[i].getMessageBody();
            }

            if (msmmsg.contains("<keszenletapp>")) {
                WriteSMSTOFile(context, msgs[0].getOriginatingAddress(), msmmsg);
                OpenIpolymentokApp(context, msgs[0].getOriginatingAddress(), msmmsg);
            } else {
                File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/pajzs.keszenletapp");
                StringBuilder text = new StringBuilder();
                try {
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    String line;
                    while ((line = br.readLine()) != null) {
                        text.append(line);
                    }
                    br.close();
                } catch (IOException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                }

                String result = text.toString();
                msmmsg = msmmsg.concat("<pajzsriasztas>");

                List<String> numbers = Arrays.asList(result.split(";"));
                String sender = msgs[0].getOriginatingAddress();
                if (sender != null) {
                    sender = sender.replace("+", "");
                    for (int j = 0; j < numbers.size(); j++) {
                        if (sender.equals(numbers.get(j))) {
                            WriteSMSTOFile(context, msgs[0].getOriginatingAddress(), msmmsg);
                            OpenIpolymentokApp(context, msgs[0].getOriginatingAddress(), msmmsg);
                            break;
                        }
                    }
                } else {
                    FirebaseCrashlytics.getInstance().log("Missing sms sender phone number");
                }
            }
        }
    }

    private void WriteSMSTOFile(Context context, String originatingAddress, String text) {
        String deviceId = "NULL";
        try (FileInputStream fis = context.openFileInput("userData")) {
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

        String out = deviceId + "|" + originatingAddress + "|" + text;

        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {

            //If it isn't mounted - we can't write into it.
            return;
        }

        try {
            File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/sms.keszenletapp");
            if (file.createNewFile()) {
                FileWriter fw = new FileWriter(file);
                fw.write(out);
                fw.flush();
                fw.close();
            } else {
                FirebaseCrashlytics.getInstance().log("Cant create file!");
            }
        } catch (IOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void OpenIpolymentokApp(final Context context, final String originatingAddress, final String text) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage("appinventor.ai_ipolymentok.KESZENLET");
        if (launchIntent != null) {
            PowerManager pm = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = null;
            if (pm != null) {
                wakeLock = pm.newWakeLock((
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                                PowerManager.FULL_WAKE_LOCK |
                                PowerManager.ACQUIRE_CAUSES_WAKEUP
                ), "myapp:waketag");
            }
            if (wakeLock != null) {
                wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
            }
            launchIntent.putExtra("APP_INVENTOR_START", originatingAddress + "|" + text);
            context.startActivity(launchIntent);//null pointer check in case package name was not found
            KeyguardManager keyguardManager = (KeyguardManager) context.getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
            KeyguardManager.KeyguardLock keyguardLock = null;
            if (keyguardManager != null) {
                keyguardLock = keyguardManager.newKeyguardLock("myapp:waketag");
            }
            if (keyguardLock != null) {
                keyguardLock.disableKeyguard();
            }
        } else {
            FirebaseCrashlytics.getInstance().log("Can't find appinventor.ai_ipolymentok.KESZENLET app.");
        }
    }
}
