package com.example.smartmedicinereminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) ||
                Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {

            DatabaseHelper dbHelper = new DatabaseHelper(context);
            List<Medication> medications = dbHelper.getAllMedications();

            for (Medication medication : medications) {
                if (medication.isActive()) {
                    AlarmScheduler.scheduleAlarms(context, medication);
                }
            }
        }
    }
}