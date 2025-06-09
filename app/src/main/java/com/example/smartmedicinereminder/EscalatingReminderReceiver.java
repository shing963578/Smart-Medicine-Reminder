package com.example.smartmedicinereminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class EscalatingReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "escalating_reminder_channel";
    private static final String TAG = "EscalatingReminder";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received escalating reminder broadcast");

        int medicationId = intent.getIntExtra("medication_id", -1);
        String medicationName = intent.getStringExtra("medication_name");
        String medicationDosage = intent.getStringExtra("medication_dosage");
        int reminderNumber = intent.getIntExtra("reminder_number", 1);
        int maxReminders = intent.getIntExtra("max_reminders", 3);

        if (medicationId == -1 || medicationName == null) {
            Log.e(TAG, "Invalid escalating reminder data");
            return;
        }

        createNotificationChannel(context);
        showEscalatingNotification(context, medicationId, medicationName, medicationDosage, reminderNumber, maxReminders);
        escalatingVibration(context, reminderNumber);

        Log.d(TAG, "Displayed escalating reminder " + reminderNumber + "/" + maxReminders + ": " + medicationName);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Escalating Medication Reminder";
            String description = "Notification channel for escalating medication reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showEscalatingNotification(Context context, int medicationId, String medicationName,
                                            String medicationDosage, int reminderNumber, int maxReminders) {
        Intent notificationIntent = new Intent(context, TakeMedicationActivity.class);
        notificationIntent.putExtra("medication_id", medicationId);
        notificationIntent.putExtra("medication_name", medicationName);
        notificationIntent.putExtra("medication_dosage", medicationDosage);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                medicationId + reminderNumber * 1000,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent takenIntent = new Intent(context, MedicationTakenReceiver.class);
        takenIntent.putExtra("medication_id", medicationId);
        takenIntent.putExtra("medication_name", medicationName);
        PendingIntent takenPendingIntent = PendingIntent.getBroadcast(
                context,
                medicationId + reminderNumber * 1000 + 10000,
                takenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String urgencyLevel = getUrgencyLevel(reminderNumber, maxReminders);
        String contentTitle = urgencyLevel + " Medication Reminder";
        String contentText = String.format("【Reminder %d】Time to take %s (%s)",
                reminderNumber, medicationName, medicationDosage != null ? medicationDosage : "");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_medication)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_check, "✓ Taken", takenPendingIntent);

        if (reminderNumber >= maxReminders / 2) {
            builder.setLights(0xFFFF0000, 1000, 1000);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(medicationId + reminderNumber * 100, builder.build());
            Log.d(TAG, "Displayed escalating notification: " + medicationName + " (reminder " + reminderNumber + ")");
        }
    }

    private String getUrgencyLevel(int reminderNumber, int maxReminders) {
        if (reminderNumber == 1) {
            return "🔔";
        } else if (reminderNumber <= maxReminders / 2) {
            return "⚠️ Important";
        } else if (reminderNumber < maxReminders) {
            return "🚨 Urgent";
        } else {
            return "🆘 Final";
        }
    }

    private void escalatingVibration(Context context, int reminderNumber) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern;
            switch (reminderNumber) {
                case 1:
                    pattern = new long[]{0, 500, 200, 500};
                    break;
                case 2:
                    pattern = new long[]{0, 1000, 300, 1000, 300, 1000};
                    break;
                default:
                    pattern = new long[]{0, 1500, 200, 1500, 200, 1500, 200, 1500};
                    break;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }
}