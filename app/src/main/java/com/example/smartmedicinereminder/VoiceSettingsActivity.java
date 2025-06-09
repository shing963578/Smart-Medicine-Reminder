package com.example.smartmedicinereminder;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class VoiceSettingsActivity extends AppCompatActivity {
    private Switch switchVoiceReminder;
    private Button btnTestVoice;
    private VoiceReminderManager voiceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_settings);

        setupToolbar();
        initializeViews();
        setupClickListeners();

        voiceManager = VoiceReminderManager.getInstance(this);
        loadSettings();
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
        switchVoiceReminder = findViewById(R.id.switchVoiceReminder);
        btnTestVoice = findViewById(R.id.btnTestVoice);
    }

    private void setupClickListeners() {
        switchVoiceReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            voiceManager.setVoiceEnabled(isChecked);
            Toast.makeText(this,
                    isChecked ? "Voice reminder enabled" : "Voice reminder disabled",
                    Toast.LENGTH_SHORT).show();
        });

        btnTestVoice.setOnClickListener(v -> {
            if (switchVoiceReminder.isChecked()) {
                voiceManager.speakMedicationReminder("Test medication", "100mg");
                Toast.makeText(this, "Playing test voice...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enable voice reminder first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSettings() {
        switchVoiceReminder.setChecked(voiceManager.isVoiceEnabled());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}