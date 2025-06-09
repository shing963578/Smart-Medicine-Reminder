package com.example.smartmedicinereminder;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

public class SyncService extends Service {
    private static final String TAG = "SyncService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting sync service");

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.syncWithFirebase();
        dbHelper.uploadLocalDataToFirebase();

        stopSelf();
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}