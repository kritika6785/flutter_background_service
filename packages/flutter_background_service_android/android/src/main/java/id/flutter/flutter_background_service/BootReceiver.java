package id.flutter.flutter_background_service;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.content.ContextCompat;


public class BootReceiver extends BroadcastReceiver {
    @SuppressLint("WakelockTimeout")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        // Ensure the broadcast comes from the system, not a third-party app
        if (!isSystemBroadcast(context, intent)) {
            Log.w("Security", "Blocked untrusted broadcast: " + intent.getAction());
            return;
        }

        // Continue only if it's a trusted system broadcast
        if (intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED) ||
                intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON")) {

            final Config config = new Config(context);
            boolean autoStart = config.isAutoStartOnBoot();
            if (autoStart) {
                if (BackgroundService.lockStatic == null) {
                    BackgroundService.getLock(context).acquire();
                }

                Intent serviceIntent = new Intent(context, BackgroundService.class);
                if (config.isForeground()) {
                    ContextCompat.startForegroundService(context, serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
    }

    private boolean isSystemBroadcast(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        try {
            String senderPackage = context.getPackageManager().getNameForUid(getCallingUid(intent));
            if (senderPackage == null) {
                return false;
            }

            ApplicationInfo appInfo = pm.getApplicationInfo(senderPackage, 0);
            return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (Exception e) {
            return false;
        }
    }

    private int getCallingUid(Intent intent) {
        try {
            return intent.getIntExtra("android.intent.extra.UID", -1);
        } catch (Exception e) {
            return -1;
        }
    }


}
