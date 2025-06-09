package com.example.smartmedicinereminder;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MedicationTakenReceiver extends BroadcastReceiver {
    private static final String TAG = "MedicationTakenReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        int medicationId = intent.getIntExtra("medication_id", -1);
        String medicationName = intent.getStringExtra("medication_name");

        stopAllNotificationsAndVibrations(context, medicationId);

        if (medicationId != -1) {
            getLocationAndSaveLog(context, medicationId, medicationName);

            DatabaseHelper dbHelper = new DatabaseHelper(context);
            for (Medication med : dbHelper.getAllMedications()) {
                if (med.getId() == medicationId) {
                    AlarmScheduler.scheduleNextReminder(context, med);
                    break;
                }
            }

            Toast.makeText(context, medicationName + " marked as taken", Toast.LENGTH_SHORT).show();
        }
    }

    private void getLocationAndSaveLog(Context context, int medicationId, String medicationName) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            saveMedicationLogWithoutLocation(context, medicationId, medicationName, "Location permission not granted");
            return;
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        Log.d(TAG, "Location obtained: " + location.getLatitude() + ", " + location.getLongitude());
                        getAddressFromLocation(context, medicationId, medicationName, location);
                    } else {
                        Log.d(TAG, "Unable to get location information");
                        saveMedicationLogWithoutLocation(context, medicationId, medicationName, "Unable to get location information");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get location", e);
                    saveMedicationLogWithoutLocation(context, medicationId, medicationName, "Failed to get location: " + e.getMessage());
                });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "Location retrieval timeout, using default information");
        }, 5000);
    }

    private void getAddressFromLocation(Context context, int medicationId, String medicationName, Location location) {
        new Thread(() -> {
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            String addressText = "";

            try {
                Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);

                    if (address.getAddressLine(0) != null) {
                        addressText = address.getAddressLine(0);
                        Log.d(TAG, "Using full address line: " + addressText);
                    } else {
                        StringBuilder addressBuilder = new StringBuilder();

                        if (address.getSubThoroughfare() != null) {
                            addressBuilder.append(address.getSubThoroughfare()).append(" ");
                        }

                        if (address.getThoroughfare() != null) {
                            addressBuilder.append(address.getThoroughfare()).append(", ");
                        }

                        if (address.getSubLocality() != null) {
                            addressBuilder.append(address.getSubLocality()).append(", ");
                        }

                        if (address.getLocality() != null) {
                            addressBuilder.append(address.getLocality()).append(", ");
                        }

                        if (address.getSubAdminArea() != null) {
                            addressBuilder.append(address.getSubAdminArea()).append(", ");
                        }

                        if (address.getAdminArea() != null) {
                            addressBuilder.append(address.getAdminArea()).append(", ");
                        }

                        if (address.getCountryName() != null) {
                            addressBuilder.append(address.getCountryName());
                        }

                        addressText = addressBuilder.toString();
                        if (addressText.endsWith(", ")) {
                            addressText = addressText.substring(0, addressText.length() - 2);
                        }
                    }

                    if (addressText.isEmpty()) {
                        addressText = String.format(Locale.getDefault(), "%.6f, %.6f", lat, lng);
                    }

                    Log.d(TAG, "English address information: " + addressText);
                } else {
                    addressText = String.format(Locale.getDefault(), "%.6f, %.6f", lat, lng);
                    Log.d(TAG, "Unable to get address, using coordinates");
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoding failed", e);
                addressText = String.format(Locale.getDefault(), "%.6f, %.6f", lat, lng);
            }

            saveMedicationLogWithLocation(context, medicationId, medicationName, lat, lng, addressText);
        }).start();
    }

    private void saveMedicationLogWithLocation(Context context, int medicationId, String medicationName,
                                               double lat, double lng, String address) {
        MedicationLog log = new MedicationLog();
        log.setMedicationId(medicationId);
        log.setMedicationName(medicationName);
        log.setTakenTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        log.setLocationLat(lat);
        log.setLocationLng(lng);
        log.setLocationAddress(address);

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        long result = dbHelper.addMedicationLog(log);

        Log.d(TAG, "Medication log saved, result: " + result + ", location: " + address);
    }

    private void saveMedicationLogWithoutLocation(Context context, int medicationId, String medicationName, String reason) {
        MedicationLog log = new MedicationLog();
        log.setMedicationId(medicationId);
        log.setMedicationName(medicationName);
        log.setTakenTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        log.setLocationLat(0.0);
        log.setLocationLng(0.0);
        log.setLocationAddress(reason);

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        dbHelper.addMedicationLog(log);

        Log.d(TAG, "Medication log saved (no location), reason: " + reason);
    }

    private void stopAllNotificationsAndVibrations(Context context, int medicationId) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.cancel();
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(medicationId);
            notificationManager.cancelAll();
        }
    }
}