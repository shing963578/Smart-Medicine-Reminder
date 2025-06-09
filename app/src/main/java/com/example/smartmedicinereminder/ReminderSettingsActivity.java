package com.example.smartmedicinereminder;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class ReminderSettingsActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "reminder_settings";
    private static final String KEY_SNOOZE_INTERVAL = "snooze_interval";
    private static final String KEY_VIBRATION_DURATION = "vibration_duration";

    private SeekBar seekBarSnoozeInterval;
    private SeekBar seekBarVibrationDuration;
    private TextView tvSnoozeInterval;
    private TextView tvVibrationDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_settings);

        setupToolbar();
        initializeViews();
        loadSettings();
        setupListeners();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initializeViews() {
        seekBarSnoozeInterval = findViewById(R.id.seekBarSnoozeInterval);
        seekBarVibrationDuration = findViewById(R.id.seekBarVibrationDuration);
        tvSnoozeInterval = findViewById(R.id.tvSnoozeInterval);
        tvVibrationDuration = findViewById(R.id.tvVibrationDuration);
    }

    private void setupListeners() {
        seekBarSnoozeInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int minutes = (progress + 1) * 5;
                tvSnoozeInterval.setText(minutes + " minutes");
                if (fromUser) saveInt(KEY_SNOOZE_INTERVAL, minutes);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarVibrationDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int seconds = (progress + 1) * 10;
                tvVibrationDuration.setText(seconds + " seconds");
                if (fromUser) saveInt(KEY_VIBRATION_DURATION, seconds);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        int snoozeMinutes = prefs.getInt(KEY_SNOOZE_INTERVAL, 5);
        seekBarSnoozeInterval.setProgress((snoozeMinutes / 5) - 1);
        tvSnoozeInterval.setText(snoozeMinutes + " minutes");

        int vibrationSeconds = prefs.getInt(KEY_VIBRATION_DURATION, 30);
        seekBarVibrationDuration.setProgress((vibrationSeconds / 10) - 1);
        tvVibrationDuration.setText(vibrationSeconds + " seconds");
    }

    private void saveInt(String key, int value) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putInt(key, value).apply();
    }

    public static int getSnoozeInterval(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_SNOOZE_INTERVAL, 5);
    }

    public static int getVibrationDuration(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_VIBRATION_DURATION, 30);
    }
}