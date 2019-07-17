package com.bence886.keszenletiHelper;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.telephony.SmsMessage;

public class SMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Get the SMS message.
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs;
        String strMessage = "";
        String format = bundle.getString("format");
        // Retrieve the SMS message received.
        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus != null) {
            // Check the Android version.
            boolean isVersionM =
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
            // Fill the msgs array.
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

                if (msgs[i].getMessageBody().contains("<keszenletapp>"))
                {
                    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage("appinventor.ai_ipolymentok.KESZENLET");
                    if (launchIntent != null) {
                        PowerManager pm = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
                        PowerManager.WakeLock wakeLock = pm.newWakeLock((
                                PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                                        PowerManager.FULL_WAKE_LOCK |
                                        PowerManager.ACQUIRE_CAUSES_WAKEUP
                        ), "myapp:waketag");
                        wakeLock.acquire();
                        context.startActivity(launchIntent);//null pointer check in case package name was not found
                        KeyguardManager keyguardManager = (KeyguardManager) context.getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
                        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("myapp:waketag");
                        keyguardLock.disableKeyguard();
                    }
                }
            }

            }
        }
}
