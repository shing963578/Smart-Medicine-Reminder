package com.example.smartmedicinereminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class MedicationReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "medication_reminder_channel";
    private static final String TAG = "MedicationReminder";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received medication reminder broadcast");

        int medicationId = intent.getIntExtra("medication_id", -1);
        String medicationName = intent.getStringExtra("medication_name");
        String medicationDosage = intent.getStringExtra("medication_dosage");
        String time = intent.getStringExtra("time");
        boolean isRepeating = intent.getBooleanExtra("is_repeating", false);
        boolean isIntervalReminder = intent.getBooleanExtra("is_interval_reminder", false);

        if (medicationId == -1 || medicationName == null) {
            Log.e(TAG, "Invalid medication reminder data");
            return;
        }

        createNotificationChannel(context);
        showNotification(context, medicationId, medicationName, medicationDosage, time);
        vibrateDevice(context, medicationId);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            VoiceReminderManager voiceManager = VoiceReminderManager.getInstance(context);
            voiceManager.speakMedicationReminder(medicationName, medicationDosage);
        }, 1000);

        if (isRepeating && !isIntervalReminder) {
            AlarmScheduler.scheduleAlarms(context, getMedicationById(context, medicationId));
        }
    }

    private Medication getMedicationById(Context context, int id) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        for (Medication med : dbHelper.getAllMedications()) {
            if (med.getId() == id) {
                return med;
            }
        }
        return null;
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Medication Reminder";
            String description = "Notification channel for medication reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(Context context, int medicationId, String medicationName,
                                  String medicationDosage, String time) {
        Intent notificationIntent = new Intent(context, TakeMedicationActivity.class);
        notificationIntent.putExtra("medication_id", medicationId);
        notificationIntent.putExtra("medication_name", medicationName);
        notificationIntent.putExtra("medication_dosage", medicationDosage);
        notificationIntent.putExtra("time", time);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                medicationId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent takenIntent = new Intent(context, MedicationTakenReceiver.class);
        takenIntent.putExtra("medication_id", medicationId);
        takenIntent.putExtra("medication_name", medicationName);
        PendingIntent takenPendingIntent = PendingIntent.getBroadcast(
                context,
                medicationId + 10000,
                takenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent snoozeIntent = new Intent(context, SnoozeReceiver.class);
        snoozeIntent.putExtra("medication_id", medicationId);
        snoozeIntent.putExtra("medication_name", medicationName);
        snoozeIntent.putExtra("medication_dosage", medicationDosage);
        snoozeIntent.putExtra("time", time);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                medicationId + 20000,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String contentText = String.format("Time to take %s (%s)",
                medicationName, medicationDosage != null ? medicationDosage : "");
        String bigText = String.format("Reminder time: %s\nMedication: %s\nDosage: %s\n\nPlease take your medication on time to stay healthy!",
                time != null ? time : "Now",
                medicationName,
                medicationDosage != null ? medicationDosage : "Not specified");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_medication)
                .setContentTitle("🔔 Medication Reminder")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .setVibrate(new long[]{0, 1000, 500, 1000})
                .setLights(0xFF0000FF, 3000, 3000)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_check, "✓ Taken", takenPendingIntent)
                .addAction(R.drawable.ic_snooze, "💤 Snooze", snoozePendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(medicationId, builder.build());
            Log.d(TAG, "Show notification: " + medicationName);
        }
    }

    private void vibrateDevice(Context context, int medicationId) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            int vibrationDuration = ReminderSettingsActivity.getVibrationDuration(context);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 1000, 500, 1000}, 0));
            } else {
                vibrator.vibrate(new long[]{0, 1000, 500, 1000}, 0);
            }

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (vibrator != null) {
                    vibrator.cancel();
                    Log.d(TAG, "Auto stopped vibration for medication: " + medicationId + " after " + vibrationDuration + " seconds");
                }
            }, vibrationDuration * 1000L);
        }
    }
}