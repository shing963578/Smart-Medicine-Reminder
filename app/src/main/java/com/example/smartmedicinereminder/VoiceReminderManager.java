package com.example.smartmedicinereminder;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import java.util.HashMap;
import java.util.Locale;

public class VoiceReminderManager implements TextToSpeech.OnInitListener {
    private static final String TAG = "VoiceReminderManager";
    private static final String PREFS_NAME = "voice_reminder_prefs";
    private static final String KEY_VOICE_ENABLED = "voice_enabled";
    private static final String UTTERANCE_ID = "medication_reminder";

    private static VoiceReminderManager instance;
    private Context context;
    private TextToSpeech textToSpeech;
    private boolean isVoiceEnabled;
    private boolean isTtsReady = false;

    private VoiceReminderManager(Context context) {
        this.context = context.getApplicationContext();
        loadSettings();
        initTextToSpeech();
    }

    public static synchronized VoiceReminderManager getInstance(Context context) {
        if (instance == null) {
            instance = new VoiceReminderManager(context);
        }
        return instance;
    }

    private void initTextToSpeech() {
        if (textToSpeech == null) {
            textToSpeech = new TextToSpeech(context, this);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.ENGLISH);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech.setLanguage(Locale.getDefault());
                }
            }

            textToSpeech.setSpeechRate(0.9f);
            textToSpeech.setPitch(1.0f);

            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    Log.d(TAG, "Voice playback started: " + utteranceId);
                }

                @Override
                public void onDone(String utteranceId) {
                    Log.d(TAG, "Voice playback completed: " + utteranceId);
                }

                @Override
                public void onError(String utteranceId) {
                    Log.e(TAG, "Voice playback error: " + utteranceId);
                }
            });

            isTtsReady = true;
            Log.d(TAG, "TextToSpeech initialization successful");
        } else {
            Log.e(TAG, "TextToSpeech initialization failed, status: " + status);
        }
    }

    public void speakMedicationReminder(String medicationName, String dosage) {
        if (!isVoiceEnabled) {
            Log.d(TAG, "Voice reminder is disabled");
            return;
        }

        if (!isTtsReady) {
            Log.d(TAG, "TextToSpeech not ready");
            return;
        }

        String message = String.format("Reminder: It's time to take your medication. Medication name: %s, Dosage: %s",
                medicationName, dosage != null ? dosage : "Not specified");

        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID);

        int result = textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, params);

        if (result == TextToSpeech.SUCCESS) {
            Log.d(TAG, "Voice playback request successful: " + message);
        } else {
            Log.e(TAG, "Voice playback request failed, result: " + result);
        }
    }

    public void setVoiceEnabled(boolean enabled) {
        this.isVoiceEnabled = enabled;
        saveSettings();
        Log.d(TAG, "Voice reminder setting: " + (enabled ? "Enabled" : "Disabled"));
    }

    public boolean isVoiceEnabled() {
        return isVoiceEnabled;
    }

    private void loadSettings() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isVoiceEnabled = prefs.getBoolean(KEY_VOICE_ENABLED, false);
        Log.d(TAG, "Loaded voice setting: " + (isVoiceEnabled ? "Enabled" : "Disabled"));
    }

    private void saveSettings() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_VOICE_ENABLED, isVoiceEnabled).apply();
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        isTtsReady = false;
        instance = null;
    }
}