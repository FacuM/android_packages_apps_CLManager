package me.facuarmo.clman;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BootCompletedReceiver extends BroadcastReceiver {

    private final String TAG = "BootCompletedReceiver";

    private final String path = "/sys/class/leds/charging/";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action != null) {
            if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

                String selectedTrigger = sharedPreferences.getString(context.getString(R.string.settings_selection_key), null);

                if (selectedTrigger != null) {
                    Log.d(TAG, "setTrigger: configuring trigger to previously selected '" + selectedTrigger + "'...");

                    try {

                        Process process = Runtime.getRuntime().exec("su -c echo " + selectedTrigger + " > " + path + "trigger");

                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                        StringBuilder result = new StringBuilder();

                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            result.append(line);
                        }

                        long delayOn = sharedPreferences.getLong(context.getString(R.string.settings_virtual_delay_on), 500);
                        long delayOff = sharedPreferences.getLong(context.getString(R.string.settings_virtual_delay_off), 500);

                        if (selectedTrigger.equals(context.getString(R.string.trigger_timer))) {
                            Runtime.getRuntime().exec("su -c echo " + delayOn + " > " + path + context.getString(R.string.trigger_timer_delay_on));
                            Runtime.getRuntime().exec("su -c echo " + delayOff + " > " + path + context.getString(R.string.trigger_timer_delay_off));
                        }

                        Log.d(TAG, "setTrigger: " + result);

                        Toast.makeText(context, String.format(context.getString(R.string.settings_boot_succeeded), selectedTrigger), Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        Intent notificationListenerIntent = new Intent(context, NotificationListenerService.class);
        context.startService(notificationListenerIntent);
    }
}
