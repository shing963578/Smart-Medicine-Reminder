package com.example.smartmedicinereminder;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private DatabaseReference database;
    private static FirebaseManager instance;

    private FirebaseManager() {
        database = FirebaseDatabase.getInstance().getReference();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public interface FirebaseCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }

    public void saveMedication(Medication medication, FirebaseCallback<String> callback) {
        if (medication.getFirebaseId() != null && !medication.getFirebaseId().isEmpty()) {
            Log.d(TAG, "Medication already has Firebase ID, performing update: " + medication.getFirebaseId());
            updateMedication(medication, new FirebaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean success) {
                    callback.onSuccess(medication.getFirebaseId());
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }
            });
            return;
        }

        String key = database.child("medications").push().getKey();
        if (key != null) {
            Map<String, Object> medicationMap = medicationToMap(medication);
            database.child("medications").child(key).setValue(medicationMap)
                    .addOnSuccessListener(aVoid -> {
                        medication.setFirebaseId(key);
                        Log.d(TAG, "New medication saved to Firebase: " + key);
                        callback.onSuccess(key);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save medication", e);
                        callback.onError(e.getMessage());
                    });
        }
    }

    public void updateMedication(Medication medication, FirebaseCallback<Boolean> callback) {
        if (medication.getFirebaseId() != null) {
            Map<String, Object> medicationMap = medicationToMap(medication);
            database.child("medications").child(medication.getFirebaseId()).setValue(medicationMap)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Medication updated in Firebase");
                        callback.onSuccess(true);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update medication", e);
                        callback.onError(e.getMessage());
                    });
        } else {
            saveMedication(medication, new FirebaseCallback<String>() {
                @Override
                public void onSuccess(String firebaseId) {
                    callback.onSuccess(true);
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }
            });
        }
    }

    public void deleteMedication(String firebaseId, FirebaseCallback<Boolean> callback) {
        database.child("medications").child(firebaseId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Medication deleted from Firebase");
                    callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete medication", e);
                    callback.onError(e.getMessage());
                });
    }

    public void getAllMedications(FirebaseCallback<List<Medication>> callback) {
        database.child("medications").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Medication> medications = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Medication medication = mapToMedication(snapshot);
                    if (medication != null) {
                        medication.setFirebaseId(snapshot.getKey());
                        medications.add(medication);
                    }
                }
                Log.d(TAG, "Retrieved " + medications.size() + " medications from Firebase");
                callback.onSuccess(medications);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to read medications", databaseError.toException());
                callback.onError(databaseError.getMessage());
            }
        });
    }

    public void saveMedicationLog(MedicationLog log, FirebaseCallback<String> callback) {
        String key = database.child("medication_logs").push().getKey();
        if (key != null) {
            Map<String, Object> logMap = logToMap(log);
            database.child("medication_logs").child(key).setValue(logMap)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Medication log saved to Firebase: " + key);
                        callback.onSuccess(key);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save medication log", e);
                        callback.onError(e.getMessage());
                    });
        }
    }

    private Map<String, Object> medicationToMap(Medication medication) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", medication.getName());
        map.put("dosage", medication.getDosage());
        map.put("frequency", medication.getFrequency());
        map.put("times", medication.getTimes());
        map.put("startDate", medication.getStartDate());
        map.put("endDate", medication.getEndDate());
        map.put("notes", medication.getNotes());
        map.put("prescriptionImagePath", medication.getPrescriptionImagePath());
        map.put("isActive", medication.isActive());
        map.put("intervalHours", medication.getIntervalHours());
        map.put("localId", medication.getId());
        map.put("timestamp", System.currentTimeMillis());
        return map;
    }

    private Medication mapToMedication(DataSnapshot snapshot) {
        try {
            Medication medication = new Medication();
            medication.setName(snapshot.child("name").getValue(String.class));
            medication.setDosage(snapshot.child("dosage").getValue(String.class));
            medication.setFrequency(snapshot.child("frequency").getValue(String.class));

            List<String> times = new ArrayList<>();
            for (DataSnapshot timeSnapshot : snapshot.child("times").getChildren()) {
                String timeValue = timeSnapshot.getValue(String.class);
                if (timeValue != null) {
                    times.add(timeValue);
                }
            }
            medication.setTimes(times);

            medication.setStartDate(snapshot.child("startDate").getValue(String.class));
            medication.setEndDate(snapshot.child("endDate").getValue(String.class));
            medication.setNotes(snapshot.child("notes").getValue(String.class));
            medication.setPrescriptionImagePath(snapshot.child("prescriptionImagePath").getValue(String.class));
            medication.setActive(Boolean.TRUE.equals(snapshot.child("isActive").getValue(Boolean.class)));
            medication.setIntervalHours(snapshot.child("intervalHours").getValue(Integer.class) != null ?
                    snapshot.child("intervalHours").getValue(Integer.class) : 1);

            Integer localId = snapshot.child("localId").getValue(Integer.class);
            if (localId != null) {
                medication.setId(localId);
            }

            return medication;
        } catch (Exception e) {
            Log.e(TAG, "Error converting snapshot to medication", e);
            return null;
        }
    }

    private Map<String, Object> logToMap(MedicationLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("medicationId", log.getMedicationId());
        map.put("medicationName", log.getMedicationName());
        map.put("takenTime", log.getTakenTime());
        map.put("locationLat", log.getLocationLat());
        map.put("locationLng", log.getLocationLng());
        map.put("locationAddress", log.getLocationAddress());
        map.put("timestamp", System.currentTimeMillis());
        return map;
    }
}