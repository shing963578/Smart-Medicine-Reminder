package com.example.smartmedicinereminder;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class VibrationManager {
    private static final String TAG = "VibrationManager";
    private static VibrationManager instance;
    private Map<Integer, Handler> vibrationHandlers;
    private Vibrator vibrator;

    private VibrationManager(Context context) {
        vibrationHandlers = new HashMap<>();
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public static synchronized VibrationManager getInstance(Context context) {
        if (instance == null) {
            instance = new VibrationManager(context.getApplicationContext());
        }
        return instance;
    }

    public void startVibrationWithTimeout(Context context, int medicationId) {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        stopVibration(medicationId);

        int vibrationDuration = ReminderSettingsActivity.getVibrationDuration(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 1000, 500, 1000}, 0));
        } else {
            vibrator.vibrate(new long[]{0, 1000, 500, 1000}, 0);
        }

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            stopVibration(medicationId);
            Log.d(TAG, "Auto stopped vibration for medication: " + medicationId + " after " + vibrationDuration + " seconds");
        }, vibrationDuration * 1000L);

        vibrationHandlers.put(medicationId, handler);
        Log.d(TAG, "Started vibration for medication: " + medicationId + " duration: " + vibrationDuration + " seconds");
    }

    public void stopVibration(int medicationId) {
        Handler handler = vibrationHandlers.get(medicationId);
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            vibrationHandlers.remove(medicationId);
        }

        if (vibrator != null) {
            vibrator.cancel();
        }

        Log.d(TAG, "Stopped vibration for medication: " + medicationId);
    }

    public void stopAllVibrations() {
        for (Handler handler : vibrationHandlers.values()) {
            handler.removeCallbacksAndMessages(null);
        }
        vibrationHandlers.clear();

        if (vibrator != null) {
            vibrator.cancel();
        }

        Log.d(TAG, "Stopped all vibrations");
    }
}