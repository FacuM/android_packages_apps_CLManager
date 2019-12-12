package me.facuarmo.clman;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

    private void setTrigger(String trigger) {
        Log.d(TAG, "setTrigger: configuring trigger to '" + trigger + "'...");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(getString(R.string.settings_selection_key), trigger);

        editor.apply();

        try {
            Process process = Runtime.getRuntime().exec("su -c echo " + trigger + " > /sys/class/leds/charging/trigger");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder result = new StringBuilder();

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }

            Log.d(TAG, "setTrigger: " + result);
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
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (mRadioGroup.getCheckedRadioButtonId()) {
                    case R.id.radio_none:
                        setTrigger(getString(R.string.trigger_none));
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
                }

                Snackbar.make(view, R.string.settings_saved, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
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
