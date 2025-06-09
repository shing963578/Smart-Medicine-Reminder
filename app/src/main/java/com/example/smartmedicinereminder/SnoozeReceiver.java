package com.example.smartmedicinereminder;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Vibrator;
import android.widget.Toast;
import java.util.Calendar;

public class SnoozeReceiver extends BroadcastReceiver {
    private static final String PREFS_NAME = "reminder_settings";
    private static final String KEY_SNOOZE_INTERVAL = "snooze_interval";

    @Override
    public void onReceive(Context context, Intent intent) {
        int medicationId = intent.getIntExtra("medication_id", -1);
        String medicationName = intent.getStringExtra("medication_name");
        String medicationDosage = intent.getStringExtra("medication_dosage");
        String time = intent.getStringExtra("time");

        if (medicationId != -1) {
            stopVibration(context);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(medicationId);
            }

            scheduleSnoozeAlarm(context, medicationId, medicationName, medicationDosage, time);

            int snoozeMinutes = getSnoozeInterval(context);
            Toast.makeText(context,
                    String.format("Reminder in %d minutes: %s", snoozeMinutes, medicationName),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void stopVibration(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private void scheduleSnoozeAlarm(Context context, int medicationId, String medicationName,
                                     String medicationDosage, String time) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        int snoozeMinutes = getSnoozeInterval(context);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, snoozeMinutes);

        Intent intent = new Intent(context, MedicationReminderReceiver.class);
        intent.putExtra("medication_id", medicationId);
        intent.putExtra("medication_name", medicationName);
        intent.putExtra("medication_dosage", medicationDosage);
        intent.putExtra("time", time);
        intent.putExtra("is_snoozed", true);

        int requestCode = medicationId + 50000;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }
    }

    private int getSnoozeInterval(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_SNOOZE_INTERVAL, 5);
    }
}