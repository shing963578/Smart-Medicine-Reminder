package com.example.smartmedicinereminder;

import android.Manifest;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TakeMedicationActivity extends AppCompatActivity {
    private static final String TAG = "TakeMedicationActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private TextView tvMedicationInfo;
    private Button btnTaken, btnSnooze, btnSkip;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private int medicationId;
    private String medicationName;
    private String medicationDosage;
    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_medication);

        initializeViews();
        setupData();
        setupClickListeners();
        setupLocationServices();
    }

    private void initializeViews() {
        tvMedicationInfo = findViewById(R.id.tvMedicationInfo);
        btnTaken = findViewById(R.id.btnTaken);
        btnSnooze = findViewById(R.id.btnSnooze);
        btnSkip = findViewById(R.id.btnSkip);
    }

    private void setupData() {
        medicationId = getIntent().getIntExtra("medication_id", -1);
        medicationName = getIntent().getStringExtra("medication_name");
        medicationDosage = getIntent().getStringExtra("medication_dosage");
        String time = getIntent().getStringExtra("time");

        String info = "Medication: " + medicationName + "\n" +
                "Dosage: " + medicationDosage + "\n" +
                "Scheduled time: " + time;
        tvMedicationInfo.setText(info);
    }

    private void setupClickListeners() {
        btnTaken.setOnClickListener(v -> markAsTaken());
        btnSnooze.setOnClickListener(v -> snoozeReminder());
        btnSkip.setOnClickListener(v -> skipMedication());
    }

    private void setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdateDelayMillis(10000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        currentLocation = location;
                        Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
                        stopLocationUpdates();
                        break;
                    }
                }
            }
        };

        requestLocationPermissionAndStartUpdates();
    }

    private void requestLocationPermissionAndStartUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (currentLocation == null) {
                    getLastKnownLocation();
                }
            }, 8000);
        }
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            currentLocation = location;
                            Log.d(TAG, "Using last known location: " + location.getLatitude() + ", " + location.getLongitude());
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Unable to get last known location", e));
        }
    }

    private void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void markAsTaken() {
        Toast.makeText(this, "Recording medication information...", Toast.LENGTH_SHORT).show();
        saveMedicationLog();
    }

    private void saveMedicationLog() {
        MedicationLog log = new MedicationLog();
        log.setMedicationId(medicationId);
        log.setMedicationName(medicationName);
        log.setTakenTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

        if (currentLocation != null) {
            double lat = currentLocation.getLatitude();
            double lng = currentLocation.getLongitude();
            log.setLocationLat(lat);
            log.setLocationLng(lng);

            Log.d(TAG, "Saving location: " + lat + ", " + lng);

            new Thread(() -> {
                try {
                    Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);
                    List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String finalAddress = "";

                        if (address.getAddressLine(0) != null) {
                            finalAddress = address.getAddressLine(0);
                            Log.d(TAG, "Using full address line: " + finalAddress);
                        } else {
                            StringBuilder addressText = new StringBuilder();

                            if (address.getSubThoroughfare() != null) {
                                addressText.append(address.getSubThoroughfare()).append(" ");
                            }

                            if (address.getThoroughfare() != null) {
                                addressText.append(address.getThoroughfare()).append(", ");
                            }

                            if (address.getSubLocality() != null) {
                                addressText.append(address.getSubLocality()).append(", ");
                            }

                            if (address.getLocality() != null) {
                                addressText.append(address.getLocality()).append(", ");
                            }

                            if (address.getSubAdminArea() != null) {
                                addressText.append(address.getSubAdminArea()).append(", ");
                            }

                            if (address.getAdminArea() != null) {
                                addressText.append(address.getAdminArea()).append(", ");
                            }

                            if (address.getCountryName() != null) {
                                addressText.append(address.getCountryName());
                            }

                            finalAddress = addressText.toString();
                            if (finalAddress.endsWith(", ")) {
                                finalAddress = finalAddress.substring(0, finalAddress.length() - 2);
                            }
                        }

                        if (finalAddress.isEmpty()) {
                            finalAddress = String.format(Locale.getDefault(), "%.6f, %.6f", lat, lng);
                        }

                        log.setLocationAddress(finalAddress);
                        Log.d(TAG, "English address parsing successful: " + finalAddress);
                    } else {
                        String coords = String.format(Locale.getDefault(), "%.6f, %.6f", lat, lng);
                        log.setLocationAddress(coords);
                        Log.d(TAG, "Unable to parse address, using coordinates: " + coords);
                    }
                } catch (IOException e) {
                    String coords = String.format(Locale.getDefault(), "%.6f, %.6f", lat, lng);
                    log.setLocationAddress(coords);
                    Log.e(TAG, "Geocoding failed, using coordinates: " + coords, e);
                }

                runOnUiThread(() -> {
                    DatabaseHelper dbHelper = new DatabaseHelper(this);
                    long result = dbHelper.addMedicationLog(log);

                    if (result != -1) {
                        String message = currentLocation != null ?
                                "Medication recorded - " + log.getLocationAddress() :
                                "Medication recorded - Location information unavailable";
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        cancelNotification();
                        finish();
                    } else {
                        Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        } else {
            log.setLocationLat(0.0);
            log.setLocationLng(0.0);
            log.setLocationAddress("Unable to get location information");
            Log.d(TAG, "No location information available");

            DatabaseHelper dbHelper = new DatabaseHelper(this);
            long result = dbHelper.addMedicationLog(log);

            if (result != -1) {
                Toast.makeText(this, "Medication recorded - Location information unavailable", Toast.LENGTH_LONG).show();
                cancelNotification();
                finish();
            } else {
                Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void cancelNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(medicationId);
        }
    }

    private void snoozeReminder() {
        Toast.makeText(this, "Reminder in 5 minutes", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void skipMedication() {
        cancelNotification();
        Toast.makeText(this, "Medication skipped", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied, unable to record location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }
}