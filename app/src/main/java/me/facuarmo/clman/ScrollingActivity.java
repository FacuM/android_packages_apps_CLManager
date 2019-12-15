package me.facuarmo.clman;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;

public class ScrollingActivity extends AppCompatActivity {

    private final String TAG = "ScrollingActivity";
    private final String CHANNEL_ID = "CL Manager test notifications";
    private final int NOTIFICATION_ID = 4096;

    private final String path = "/sys/class/leds/charging/";


    private RadioGroup mRadioGroup;
    private EditText mDelayOn;
    private EditText mDelayOff;
    private TextView mHintBug;
    private boolean usesVirtualNotificationsMode;
    private SharedPreferences sharedPreferences;

    private void testTimerData() {
        pushAndLog("Testing timer configuration...");

        if (mRadioGroup.getCheckedRadioButtonId() == R.id.radio_timer || mRadioGroup.getCheckedRadioButtonId() == R.id.radio_notifications) {
            long delayOn = sharedPreferences.getLong(getString(R.string.settings_virtual_delay_on), 500);
            long delayOff = sharedPreferences.getLong(getString(R.string.settings_virtual_delay_off), 500);

            mDelayOn.setText(String.valueOf(delayOn));
            mDelayOff.setText(String.valueOf(delayOff));

            pushAndLog("delayOn: " + delayOn);
            pushAndLog("delayOff: " + delayOff);

            try {
                Runtime.getRuntime().exec("su -c echo " + delayOn + " > " + path + getString(R.string.trigger_timer_delay_on));
                Runtime.getRuntime().exec("su -c echo " + delayOff + " > " + path + getString(R.string.trigger_timer_delay_off));
            } catch (IOException e) {
                e.printStackTrace();
            }

            mDelayOn.setVisibility(View.VISIBLE);
            mDelayOff.setVisibility(View.VISIBLE);
        } else {
            if (!BuildConfig.DEBUG) {
                mDelayOn.setVisibility(View.GONE);
                mDelayOff.setVisibility(View.GONE);
            }
        }

        pushAndLog("Done setting up the timer.");
    }

    private boolean isRooted() {
        pushAndLog("Making sure that the device is rooted and we're allowed to run as such...");

        try {
            int returnCode;

            Process process = Runtime.getRuntime().exec("su -c '' ; echo $?");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder result = new StringBuilder();

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }

            returnCode = (result.toString().equals("") ? 1 : Integer.parseInt(result.toString()));

            Log.d(TAG, "setTrigger: root test: " + returnCode);

            return returnCode == 126;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void setTrigger(String trigger) {
        pushAndLog("Configuring trigger to '" + trigger + "'...");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(getString(R.string.settings_selection_key), trigger);

        editor.apply();

        try {
            Runtime.getRuntime().exec("su -c echo " + trigger + " > " + path + "trigger");

            if (trigger.equals(getString(R.string.trigger_timer))) {
                sendTimerSettings();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendTimerSettings() {
        pushAndLog("Writing timer settings to the real files.");

        try {
            Runtime.getRuntime().exec("su -c echo " + mDelayOn.getText() + " > " + path + getString(R.string.trigger_timer_delay_on));
            Runtime.getRuntime().exec("su -c echo " + mDelayOff.getText() + " > " + path + getString(R.string.trigger_timer_delay_off));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pushAndLog(String msg) {
        if (BuildConfig.DEBUG) {
            mHintBug.append(msg + "\n");
        }

        Log.d(TAG, "pushAndLog: " + msg);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRadioGroup = findViewById(R.id.radio_group);
        mDelayOn = findViewById(R.id.delay_on);
        mDelayOff = findViewById(R.id.delay_off);
        mHintBug = findViewById(R.id.hint_bug);

        if (BuildConfig.DEBUG) {
            mHintBug.setText("");
        }

        pushAndLog("Debug mode started.");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        usesVirtualNotificationsMode = sharedPreferences.getBoolean(getString(R.string.settings_virtual_mode_key), false);

        if (usesVirtualNotificationsMode) {
            pushAndLog("Virtual notifications mode enabled.");

            mRadioGroup.check(R.id.radio_notifications);

            setTrigger(getString(R.string.trigger_none));
            testTimerData();

            mHintBug.setVisibility(View.VISIBLE);

            pushAndLog("Setting up the service intent...");
            Intent notificationListenerIntent = new Intent(ScrollingActivity.this, NotificationListenerService.class);
            pushAndLog("Stopping the service (if started)...");
            stopService(notificationListenerIntent);
            pushAndLog("Starting the service...");
            startService(notificationListenerIntent);
        } else {
            String selectedTrigger = sharedPreferences.getString(getString(R.string.settings_selection_key), null);

            if (selectedTrigger == null) {
                try {
                    String cmd = "cat /sys/class/leds/charging/trigger";

                    pushAndLog("No trigger saved, first time? Grabbing it off the real file at '" + cmd + "'...");

                    Log.d(TAG, "onCreate: built command was: " + cmd);

                    Process process = Runtime.getRuntime().exec(cmd);

                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                    StringBuilder result = new StringBuilder();

                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        result.append(line);
                    }

                    selectedTrigger = result.toString().split("\\[")[1].replaceAll("].*", "").trim();
                } catch (IOException e) {
                    selectedTrigger = getString(R.string.trigger_none);
                    e.printStackTrace();
                }
            }

            pushAndLog("Success setting up the trigger. - selectedTrigger: " + selectedTrigger);

            if (selectedTrigger.equals(getString(R.string.trigger_none))) {
                mRadioGroup.check(R.id.radio_none);
            } else if (selectedTrigger.equals(getString(R.string.trigger_charging_multi))) {
                mRadioGroup.check(R.id.radio_charging_multi);
            } else if (selectedTrigger.equals(getString(R.string.trigger_charging_or_full))) {
                mRadioGroup.check(R.id.radio_charging_or_full);
            } else if (selectedTrigger.equals(getString(R.string.trigger_charging))) {
                mRadioGroup.check(R.id.radio_charging);
            } else if (selectedTrigger.equals(getString(R.string.trigger_full))) {
                mRadioGroup.check(R.id.radio_full);
            } else if (selectedTrigger.equals(getString(R.string.trigger_internal_memory))) {
                mRadioGroup.check(R.id.radio_internal_memory);
            } else if (selectedTrigger.equals(getString(R.string.trigger_external_memory))) {
                mRadioGroup.check(R.id.radio_external_memory);
            } else if (selectedTrigger.equals(getString(R.string.trigger_timer))) {
                mRadioGroup.check(R.id.radio_timer);

                testTimerData();
            }

            if (!BuildConfig.DEBUG) {
                mHintBug.setVisibility(View.GONE);
            }
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRooted()) {
                    pushAndLog("We're rooted, it's time to keep going!");

                    usesVirtualNotificationsMode = false;

                    switch (mRadioGroup.getCheckedRadioButtonId()) {
                        case R.id.radio_none:
                            setTrigger(getString(R.string.trigger_none));
                            break;
                        case R.id.radio_charging_multi:
                            setTrigger(getString(R.string.trigger_charging_multi));
                            break;
                        case R.id.radio_charging_or_full:
                            setTrigger(getString(R.string.trigger_charging_or_full));
                            break;
                        case R.id.radio_charging:
                            setTrigger(getString(R.string.trigger_charging));
                            break;
                        case R.id.radio_full:
                            setTrigger(getString(R.string.trigger_full));
                            break;
                        case R.id.radio_internal_memory:
                            setTrigger(getString(R.string.trigger_internal_memory));
                            break;
                        case R.id.radio_external_memory:
                            setTrigger(getString(R.string.trigger_external_memory));
                            break;
                        case R.id.radio_timer:
                            setTrigger(getString(R.string.trigger_timer));
                            break;
                        case R.id.radio_notifications:
                            if (usesVirtualNotificationsMode) {
                                sendTimerSettings();
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                if (Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners").contains(getPackageName())) {
                                    pushAndLog("Fine, I'm allowed to read your notifications!");
                                } else {
                                    pushAndLog("I'm not in the list of allowed notification listeners, please let me read them.");

                                    Toast.makeText(ScrollingActivity.this, getString(R.string.notifications_request), Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                                }
                            }

                            sharedPreferences.edit().putLong(getString(R.string.settings_virtual_delay_on), Long.parseLong(mDelayOn.getText().toString())).apply();
                            sharedPreferences.edit().putLong(getString(R.string.settings_virtual_delay_off), Long.parseLong(mDelayOff.getText().toString())).apply();

                            usesVirtualNotificationsMode = true;

                            Intent notificationListenerIntent = new Intent(ScrollingActivity.this, NotificationListenerService.class);
                            stopService(notificationListenerIntent);
                            startService(notificationListenerIntent);

                            break;
                    }

                    if (usesVirtualNotificationsMode) {
                        sharedPreferences.edit().putBoolean(getString(R.string.settings_virtual_mode_key), true).apply();
                    } else {
                        sharedPreferences.edit().putBoolean(getString(R.string.settings_virtual_mode_key), false).apply();
                    }

                    pushAndLog("Your changes have been saved!");

                    Snackbar.make(view, R.string.settings_saved, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {
                    pushAndLog("Unable to acquire root, please check your allowed apps in your root manager.");

                    Snackbar.make(view, R.string.settings_failure, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }
        });

        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i == R.id.radio_notifications) {
                    mHintBug.setVisibility(View.VISIBLE);
                } else {
                    if (!BuildConfig.DEBUG) {
                        mHintBug.setVisibility(View.GONE);
                    }
                }

                testTimerData();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_test_notification) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT);

                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(notificationChannel);
                }
            }

            if (notificationManager != null) {
                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.notification_test_text))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
            }

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
