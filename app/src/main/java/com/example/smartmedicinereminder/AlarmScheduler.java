package com.example.smartmedicinereminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AlarmScheduler {
    private static final String TAG = "AlarmScheduler";

    public static void scheduleAlarms(Context context, Medication medication) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (String timeStr : medication.getTimes()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date time = sdf.parse(timeStr);
                if (time == null) continue;

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(time);

                Calendar now = Calendar.getInstance();
                calendar.set(Calendar.YEAR, now.get(Calendar.YEAR));
                calendar.set(Calendar.MONTH, now.get(Calendar.MONTH));
                calendar.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

                if (calendar.before(now)) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                }

                Intent intent = new Intent(context, MedicationReminderReceiver.class);
                intent.putExtra("medication_id", medication.getId());
                intent.putExtra("medication_name", medication.getName());
                intent.putExtra("medication_dosage", medication.getDosage());
                intent.putExtra("time", timeStr);

                int requestCode = generateRequestCode(medication.getId(), timeStr);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent
                        );
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent
                        );
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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

                Log.d(TAG, "Set alarm: " + medication.getName() + " time: " + timeStr + " at " + calendar.getTime());

            } catch (ParseException e) {
                Log.e(TAG, "Time parsing error: " + timeStr, e);
            }
        }
    }

    public static void scheduleNextReminder(Context context, Medication medication) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Calendar nextReminder = Calendar.getInstance();
        nextReminder.add(Calendar.HOUR_OF_DAY, medication.getIntervalHours());

        Intent intent = new Intent(context, MedicationReminderReceiver.class);
        intent.putExtra("medication_id", medication.getId());
        intent.putExtra("medication_name", medication.getName());
        intent.putExtra("medication_dosage", medication.getDosage());
        intent.putExtra("time", new SimpleDateFormat("HH:mm", Locale.getDefault()).format(nextReminder.getTime()));
        intent.putExtra("is_interval_reminder", true);

        int requestCode = medication.getId() + 100000;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextReminder.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextReminder.getTimeInMillis(),
                    pendingIntent
            );
        }

        Log.d(TAG, "Set next interval reminder: " + medication.getName() + " in " + medication.getIntervalHours() + " hours");
    }

    public static void cancelAlarms(Context context, Medication medication) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (String timeStr : medication.getTimes()) {
            Intent intent = new Intent(context, MedicationReminderReceiver.class);

            int requestCode = generateRequestCode(medication.getId(), timeStr);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(pendingIntent);

            Log.d(TAG, "Cancel alarm: " + medication.getName() + " time: " + timeStr);
        }

        Intent intervalIntent = new Intent(context, MedicationReminderReceiver.class);
        int intervalRequestCode = medication.getId() + 100000;
        PendingIntent intervalPendingIntent = PendingIntent.getBroadcast(
                context,
                intervalRequestCode,
                intervalIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(intervalPendingIntent);
    }

    private static int generateRequestCode(int medicationId, String timeStr) {
        try {
            return (medicationId * 1000) + Integer.parseInt(timeStr.replace(":", ""));
        } catch (NumberFormatException e) {
            return medicationId * 1000 + timeStr.hashCode();
        }
    }

    public static void rescheduleAllAlarms(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        for (Medication medication : dbHelper.getAllMedications()) {
            if (medication.isActive()) {
                scheduleAlarms(context, medication);
            }
        }
    }
}