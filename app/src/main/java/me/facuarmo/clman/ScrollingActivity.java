package me.facuarmo.clman;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ScrollingActivity extends AppCompatActivity {

    private final String TAG = "ScrollingActivity";

    private RadioGroup mRadioGroup;
    private EditText mDelayOn;
    private EditText mDelayOff;

    private void testTimerData() {
        if (mRadioGroup.getCheckedRadioButtonId() == R.id.radio_timer) {
            try {
                String path = "cat /sys/class/leds/charging/";

                String delayOn = getString(R.string.trigger_timer_delay_on);
                String delayOff = getString(R.string.trigger_timer_delay_off);

                Process process;
                BufferedReader bufferedReader;
                StringBuilder result;
                String line;

                process = Runtime.getRuntime().exec(path + delayOn);

                bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                result = new StringBuilder();

                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line);
                }

                if (!result.toString().isEmpty()) {
                    mDelayOn.setText(result.toString().trim());
                }

                process = Runtime.getRuntime().exec(path + delayOff);

                bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                result = new StringBuilder();

                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line);
                }

                if (!result.toString().isEmpty()) {
                    mDelayOff.setText(result.toString().trim());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            mDelayOn.setVisibility(View.VISIBLE);
            mDelayOff.setVisibility(View.VISIBLE);
        } else {
            mDelayOn.setVisibility(View.GONE);
            mDelayOff.setVisibility(View.GONE);
        }
    }

    private boolean isRooted() {
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
        Log.d(TAG, "setTrigger: configuring trigger to '" + trigger + "'...");

        String path = "/sys/class/leds/charging/";

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(getString(R.string.settings_selection_key), trigger);

        editor.apply();

        try {
            Runtime.getRuntime().exec("su -c echo " + trigger + " > " + path + "trigger");

            if (trigger.equals(getString(R.string.trigger_timer))) {
                Runtime.getRuntime().exec("su -c echo " + mDelayOn.getText() + " > " + path + getString(R.string.trigger_timer_delay_on));
                Runtime.getRuntime().exec("su -c echo " + mDelayOff.getText() + " > " + path + getString(R.string.trigger_timer_delay_off));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
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

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String selectedTrigger = sharedPreferences.getString(getString(R.string.settings_selection_key), null);

        if (selectedTrigger == null) {
            try {
                String cmd = "cat /sys/class/leds/charging/trigger";

                Log.d(TAG, "onCreate: built command was: " + cmd);

                Process process = Runtime.getRuntime().exec(cmd);

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                StringBuilder result = new StringBuilder();

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line);
                }

                selectedTrigger = result.toString().split("\\[")[1].replaceAll("].*", "").trim();

                Log.d(TAG, "onCreate: selectedTrigger: " + selectedTrigger);
            } catch (IOException e) {
                selectedTrigger = getString(R.string.trigger_none);
                e.printStackTrace();
            }
        }

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

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRooted()) {
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
                    }

                    Snackbar.make(view, R.string.settings_saved, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {
                    Snackbar.make(view, R.string.settings_failure, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }
        });

        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
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
        }
        return super.onOptionsItemSelected(item);
    }
}
