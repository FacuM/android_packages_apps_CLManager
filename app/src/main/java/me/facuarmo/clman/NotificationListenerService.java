package me.facuarmo.clman;

import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.io.IOException;

public class NotificationListenerService extends android.service.notification.NotificationListenerService {

    private final String TAG = "NLS";
    private long notificationsCount;
    private String currentTrigger;

    private SharedPreferences sharedPreferences;

    public NotificationListenerService() {}

    private void setTrigger(String trigger) {
        Log.d(TAG, "setTrigger: configuring trigger to '" + trigger + "'...");

        if (currentTrigger.equals(trigger)) {
            Log.d(TAG, "setTrigger: skipping operation because it'd be redundant.");
        } else {
            String path = "/sys/class/leds/charging/";

            try {
                Runtime.getRuntime().exec("su -c echo " + trigger + " > " + path + "trigger");

                long delayOn = sharedPreferences.getLong(getString(R.string.settings_virtual_delay_on), 500);
                long delayOff = sharedPreferences.getLong(getString(R.string.settings_virtual_delay_off), 500);

                Runtime.getRuntime().exec("su -c echo " + delayOn + " > " + path + getString(R.string.trigger_timer_delay_on));
                Runtime.getRuntime().exec("su -c echo " + delayOff + " > " + path + getString(R.string.trigger_timer_delay_off));
            } catch (IOException e) {
                e.printStackTrace();
            }

            currentTrigger = trigger;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationsCount = 0;
        currentTrigger = getString(R.string.trigger_none);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Log.d(TAG, "onCreate: success starting service.");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG, "onNotificationPosted: notification received.");

        if (sbn.getNotification().flags != Notification.FLAG_ONGOING_EVENT) {
            notificationsCount++;

            setTrigger(getString(R.string.trigger_timer));
        }

        super.onNotificationPosted(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (notificationsCount > 0) {
            notificationsCount--;
        }

        if (notificationsCount == 0) {
            setTrigger(getString(R.string.trigger_none));
        }

        Log.d(TAG, "onNotificationRemoved: notification removed (" + notificationsCount + " left).");

        super.onNotificationRemoved(sbn);
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }
}
