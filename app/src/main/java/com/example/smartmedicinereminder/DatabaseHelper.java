package com.example.smartmedicinereminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "smart_medicine.db";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_MEDICATIONS = "medications";
    private static final String TABLE_MEDICATION_LOGS = "medication_logs";

    private static final String COL_ID = "id";
    private static final String COL_FIREBASE_ID = "firebase_id";
    private static final String COL_NAME = "name";
    private static final String COL_DOSAGE = "dosage";
    private static final String COL_FREQUENCY = "frequency";
    private static final String COL_TIMES = "times";
    private static final String COL_START_DATE = "start_date";
    private static final String COL_END_DATE = "end_date";
    private static final String COL_NOTES = "notes";
    private static final String COL_PRESCRIPTION_IMAGE = "prescription_image";
    private static final String COL_IS_ACTIVE = "is_active";
    private static final String COL_INTERVAL_HOURS = "interval_hours";

    private static final String COL_MEDICATION_ID = "medication_id";
    private static final String COL_TAKEN_TIME = "taken_time";
    private static final String COL_LOCATION_LAT = "location_lat";
    private static final String COL_LOCATION_LNG = "location_lng";
    private static final String COL_LOCATION_ADDRESS = "location_address";

    private FirebaseManager firebaseManager;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        firebaseManager = FirebaseManager.getInstance();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MEDICATIONS_TABLE = "CREATE TABLE " + TABLE_MEDICATIONS + "("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_FIREBASE_ID + " TEXT,"
                + COL_NAME + " TEXT NOT NULL,"
                + COL_DOSAGE + " TEXT,"
                + COL_FREQUENCY + " TEXT,"
                + COL_TIMES + " TEXT,"
                + COL_START_DATE + " TEXT,"
                + COL_END_DATE + " TEXT,"
                + COL_NOTES + " TEXT,"
                + COL_PRESCRIPTION_IMAGE + " TEXT,"
                + COL_IS_ACTIVE + " INTEGER DEFAULT 1,"
                + COL_INTERVAL_HOURS + " INTEGER DEFAULT 1)";

        String CREATE_MEDICATION_LOGS_TABLE = "CREATE TABLE " + TABLE_MEDICATION_LOGS + "("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_MEDICATION_ID + " INTEGER,"
                + COL_TAKEN_TIME + " TEXT,"
                + COL_LOCATION_LAT + " REAL,"
                + COL_LOCATION_LNG + " REAL,"
                + COL_LOCATION_ADDRESS + " TEXT,"
                + "FOREIGN KEY(" + COL_MEDICATION_ID + ") REFERENCES " + TABLE_MEDICATIONS + "(" + COL_ID + "))";

        db.execSQL(CREATE_MEDICATIONS_TABLE);
        db.execSQL(CREATE_MEDICATION_LOGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_MEDICATIONS + " ADD COLUMN " + COL_INTERVAL_HOURS + " INTEGER DEFAULT 1");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_MEDICATIONS + " ADD COLUMN " + COL_FIREBASE_ID + " TEXT");
        }
    }

    public long addMedication(Medication medication) {
        Log.d("DatabaseHelper", "Preparing to add medication: " + medication.getName());

        if (isDuplicateMedication(medication)) {
            Log.w("DatabaseHelper", "Found duplicate medication, skipping: " + medication.getName());
            return -1;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, medication.getName());
        values.put(COL_DOSAGE, medication.getDosage());
        values.put(COL_FREQUENCY, medication.getFrequency());
        values.put(COL_TIMES, String.join(",", medication.getTimes()));
        values.put(COL_START_DATE, medication.getStartDate());
        values.put(COL_END_DATE, medication.getEndDate());
        values.put(COL_NOTES, medication.getNotes());
        values.put(COL_PRESCRIPTION_IMAGE, medication.getPrescriptionImagePath());
        values.put(COL_IS_ACTIVE, medication.isActive() ? 1 : 0);
        values.put(COL_INTERVAL_HOURS, medication.getIntervalHours());
        values.put(COL_FIREBASE_ID, medication.getFirebaseId());

        long result = db.insert(TABLE_MEDICATIONS, null, values);
        db.close();

        if (result != -1) {
            medication.setId((int) result);
            Log.d("DatabaseHelper", "Medication added successfully, local ID: " + result);

            firebaseManager.saveMedication(medication, new FirebaseManager.FirebaseCallback<String>() {
                @Override
                public void onSuccess(String firebaseId) {
                    medication.setFirebaseId(firebaseId);
                    updateMedicationFirebaseId((int) result, firebaseId);
                    Log.d("DatabaseHelper", "Firebase save successful: " + firebaseId);
                }

                @Override
                public void onError(String error) {
                    Log.e("DatabaseHelper", "Firebase save failed: " + error);
                }
            });
        }

        return result;
    }

    private boolean isDuplicateMedication(Medication newMedication) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = COL_NAME + " = ? AND " + COL_DOSAGE + " = ? AND " + COL_IS_ACTIVE + " = 1";
        String[] selectionArgs = {
                newMedication.getName() != null ? newMedication.getName() : "",
                newMedication.getDosage() != null ? newMedication.getDosage() : ""
        };

        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_MEDICATIONS + " WHERE " + selection, selectionArgs);

        boolean isDuplicate = false;
        if (cursor.moveToFirst()) {
            isDuplicate = cursor.getInt(0) > 0;
        }

        cursor.close();
        db.close();

        return isDuplicate;
    }

    public boolean updateMedication(Medication medication) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, medication.getName());
        values.put(COL_DOSAGE, medication.getDosage());
        values.put(COL_FREQUENCY, medication.getFrequency());
        values.put(COL_TIMES, String.join(",", medication.getTimes()));
        values.put(COL_START_DATE, medication.getStartDate());
        values.put(COL_END_DATE, medication.getEndDate());
        values.put(COL_NOTES, medication.getNotes());
        values.put(COL_PRESCRIPTION_IMAGE, medication.getPrescriptionImagePath());
        values.put(COL_IS_ACTIVE, medication.isActive() ? 1 : 0);
        values.put(COL_INTERVAL_HOURS, medication.getIntervalHours());

        int result = db.update(TABLE_MEDICATIONS, values, COL_ID + " = ?",
                new String[]{String.valueOf(medication.getId())});
        db.close();

        if (result > 0 && medication.getFirebaseId() != null) {
            firebaseManager.updateMedication(medication, new FirebaseManager.FirebaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean success) {
                    Log.d("DatabaseHelper", "Firebase update successful");
                }

                @Override
                public void onError(String error) {
                    Log.e("DatabaseHelper", "Firebase update failed: " + error);
                }
            });
        }

        return result > 0;
    }

    public boolean deleteMedication(int medicationId) {
        Medication medication = getMedicationById(medicationId);

        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_MEDICATIONS, COL_ID + " = ?", new String[]{String.valueOf(medicationId)});
        db.close();

        if (result > 0 && medication != null && medication.getFirebaseId() != null) {
            firebaseManager.deleteMedication(medication.getFirebaseId(), new FirebaseManager.FirebaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean success) {
                    Log.d("DatabaseHelper", "Firebase deletion successful");
                }

                @Override
                public void onError(String error) {
                    Log.e("DatabaseHelper", "Firebase deletion failed: " + error);
                }
            });
        }

        return result > 0;
    }

    public List<Medication> getAllMedications() {
        List<Medication> medications = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_MEDICATIONS + " WHERE " + COL_IS_ACTIVE + " = 1", null);

        if (cursor.moveToFirst()) {
            do {
                Medication medication = new Medication();
                medication.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
                medication.setFirebaseId(cursor.getString(cursor.getColumnIndexOrThrow(COL_FIREBASE_ID)));
                medication.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)));
                medication.setDosage(cursor.getString(cursor.getColumnIndexOrThrow(COL_DOSAGE)));
                medication.setFrequency(cursor.getString(cursor.getColumnIndexOrThrow(COL_FREQUENCY)));

                String timesStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_TIMES));
                if (timesStr != null && !timesStr.isEmpty()) {
                    medication.setTimes(Arrays.asList(timesStr.split(",")));
                } else {
                    medication.setTimes(new ArrayList<>());
                }

                medication.setStartDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_START_DATE)));
                medication.setEndDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_END_DATE)));
                medication.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTES)));
                medication.setPrescriptionImagePath(cursor.getString(cursor.getColumnIndexOrThrow(COL_PRESCRIPTION_IMAGE)));
                medication.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_ACTIVE)) == 1);

                int intervalHours = cursor.getInt(cursor.getColumnIndexOrThrow(COL_INTERVAL_HOURS));
                medication.setIntervalHours(intervalHours > 0 ? intervalHours : 1);

                medications.add(medication);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        Log.d("DatabaseHelper", "Loaded " + medications.size() + " medications");
        return medications;
    }

    public long addMedicationLog(MedicationLog log) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_MEDICATION_ID, log.getMedicationId());
        values.put(COL_TAKEN_TIME, log.getTakenTime());
        values.put(COL_LOCATION_LAT, log.getLocationLat());
        values.put(COL_LOCATION_LNG, log.getLocationLng());
        values.put(COL_LOCATION_ADDRESS, log.getLocationAddress());

        long result = db.insert(TABLE_MEDICATION_LOGS, null, values);
        db.close();

        if (result != -1) {
            firebaseManager.saveMedicationLog(log, new FirebaseManager.FirebaseCallback<String>() {
                @Override
                public void onSuccess(String firebaseId) {
                    Log.d("DatabaseHelper", "Medication log saved to Firebase");
                }

                @Override
                public void onError(String error) {
                    Log.e("DatabaseHelper", "Firebase medication log save failed: " + error);
                }
            });
        }

        return result;
    }

    public boolean deleteMedicationLog(int logId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_MEDICATION_LOGS, COL_ID + " = ?", new String[]{String.valueOf(logId)});
        db.close();
        return result > 0;
    }

    public List<MedicationLog> getAllMedicationLogs() {
        List<MedicationLog> logs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT ml.*, m.name as medication_name FROM " + TABLE_MEDICATION_LOGS +
                " ml JOIN " + TABLE_MEDICATIONS + " m ON ml." + COL_MEDICATION_ID + " = m." + COL_ID +
                " ORDER BY ml." + COL_TAKEN_TIME + " DESC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                MedicationLog log = new MedicationLog();
                log.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
                log.setMedicationId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_MEDICATION_ID)));
                log.setMedicationName(cursor.getString(cursor.getColumnIndexOrThrow("medication_name")));
                log.setTakenTime(cursor.getString(cursor.getColumnIndexOrThrow(COL_TAKEN_TIME)));
                log.setLocationLat(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LOCATION_LAT)));
                log.setLocationLng(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LOCATION_LNG)));
                log.setLocationAddress(cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATION_ADDRESS)));
                logs.add(log);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return logs;
    }

    private void updateMedicationFirebaseId(int localId, String firebaseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_FIREBASE_ID, firebaseId);
        db.update(TABLE_MEDICATIONS, values, COL_ID + " = ?", new String[]{String.valueOf(localId)});
        db.close();
    }

    private Medication getMedicationById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_MEDICATIONS + " WHERE " + COL_ID + " = ?",
                new String[]{String.valueOf(id)});

        Medication medication = null;
        if (cursor.moveToFirst()) {
            medication = new Medication();
            medication.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
            medication.setFirebaseId(cursor.getString(cursor.getColumnIndexOrThrow(COL_FIREBASE_ID)));
            medication.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)));
            medication.setDosage(cursor.getString(cursor.getColumnIndexOrThrow(COL_DOSAGE)));
            medication.setFrequency(cursor.getString(cursor.getColumnIndexOrThrow(COL_FREQUENCY)));

            String timesStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_TIMES));
            if (timesStr != null && !timesStr.isEmpty()) {
                medication.setTimes(Arrays.asList(timesStr.split(",")));
            } else {
                medication.setTimes(new ArrayList<>());
            }

            medication.setStartDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_START_DATE)));
            medication.setEndDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_END_DATE)));
            medication.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTES)));
            medication.setPrescriptionImagePath(cursor.getString(cursor.getColumnIndexOrThrow(COL_PRESCRIPTION_IMAGE)));
            medication.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_ACTIVE)) == 1);

            int intervalHours = cursor.getInt(cursor.getColumnIndexOrThrow(COL_INTERVAL_HOURS));
            medication.setIntervalHours(intervalHours > 0 ? intervalHours : 1);
        }
        cursor.close();
        db.close();
        return medication;
    }

    public void removeDuplicateMedications() {
        Log.d("DatabaseHelper", "Starting duplicate medication cleanup");
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            Cursor cursor = db.rawQuery("SELECT " + COL_NAME + ", " + COL_DOSAGE + ", COUNT(*) as count FROM " + TABLE_MEDICATIONS +
                    " WHERE " + COL_IS_ACTIVE + " = 1 GROUP BY " + COL_NAME + ", " + COL_DOSAGE + " HAVING count > 1", null);

            int duplicatesRemoved = 0;
            if (cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME));
                    String dosage = cursor.getString(cursor.getColumnIndexOrThrow(COL_DOSAGE));

                    Cursor duplicateCursor = db.rawQuery("SELECT * FROM " + TABLE_MEDICATIONS +
                                    " WHERE " + COL_NAME + " = ? AND " + COL_DOSAGE + " = ? AND " + COL_IS_ACTIVE + " = 1 ORDER BY " + COL_ID,
                            new String[]{name, dosage});

                    boolean isFirst = true;
                    while (duplicateCursor.moveToNext()) {
                        if (!isFirst) {
                            int duplicateId = duplicateCursor.getInt(duplicateCursor.getColumnIndexOrThrow(COL_ID));
                            db.delete(TABLE_MEDICATIONS, COL_ID + " = ?", new String[]{String.valueOf(duplicateId)});
                            duplicatesRemoved++;
                            Log.d("DatabaseHelper", "Deleted duplicate medication: " + name + " ID: " + duplicateId);
                        }
                        isFirst = false;
                    }
                    duplicateCursor.close();

                } while (cursor.moveToNext());
            }
            cursor.close();

            Log.d("DatabaseHelper", "Cleanup completed, deleted " + duplicatesRemoved + " duplicate medications");
        } finally {
            db.close();
        }
    }

    public void syncWithFirebase() {
        Log.d("DatabaseHelper", "Starting Firebase sync");

        firebaseManager.getAllMedications(new FirebaseManager.FirebaseCallback<List<Medication>>() {
            @Override
            public void onSuccess(List<Medication> firebaseMedications) {
                Log.d("DatabaseHelper", "Retrieved " + firebaseMedications.size() + " medications from Firebase");

                SQLiteDatabase db = null;
                try {
                    db = getWritableDatabase();

                    for (Medication firebaseMed : firebaseMedications) {
                        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_MEDICATIONS + " WHERE " + COL_FIREBASE_ID + " = ?",
                                new String[]{firebaseMed.getFirebaseId()});

                        if (!cursor.moveToFirst()) {
                            Cursor nameCheckCursor = db.rawQuery("SELECT * FROM " + TABLE_MEDICATIONS + " WHERE " + COL_NAME + " = ? AND " + COL_DOSAGE + " = ? AND " + COL_IS_ACTIVE + " = 1",
                                    new String[]{firebaseMed.getName(), firebaseMed.getDosage()});

                            if (!nameCheckCursor.moveToFirst()) {
                                ContentValues values = new ContentValues();
                                values.put(COL_FIREBASE_ID, firebaseMed.getFirebaseId());
                                values.put(COL_NAME, firebaseMed.getName());
                                values.put(COL_DOSAGE, firebaseMed.getDosage());
                                values.put(COL_FREQUENCY, firebaseMed.getFrequency());
                                values.put(COL_TIMES, String.join(",", firebaseMed.getTimes()));
                                values.put(COL_START_DATE, firebaseMed.getStartDate());
                                values.put(COL_END_DATE, firebaseMed.getEndDate());
                                values.put(COL_NOTES, firebaseMed.getNotes());
                                values.put(COL_PRESCRIPTION_IMAGE, firebaseMed.getPrescriptionImagePath());
                                values.put(COL_IS_ACTIVE, firebaseMed.isActive() ? 1 : 0);
                                values.put(COL_INTERVAL_HOURS, firebaseMed.getIntervalHours());

                                long localId = db.insert(TABLE_MEDICATIONS, null, values);
                                firebaseMed.setId((int) localId);

                                Log.d("DatabaseHelper", "Synced medication from Firebase: " + firebaseMed.getName());
                            } else {
                                int existingId = nameCheckCursor.getInt(nameCheckCursor.getColumnIndexOrThrow(COL_ID));
                                ContentValues updateValues = new ContentValues();
                                updateValues.put(COL_FIREBASE_ID, firebaseMed.getFirebaseId());
                                db.update(TABLE_MEDICATIONS, updateValues, COL_ID + " = ?", new String[]{String.valueOf(existingId)});
                                Log.d("DatabaseHelper", "Updated existing medication Firebase ID: " + firebaseMed.getName());
                            }
                            nameCheckCursor.close();
                        }
                        cursor.close();
                    }
                } finally {
                    if (db != null) {
                        db.close();
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.e("DatabaseHelper", "Firebase sync failed: " + error);
            }
        });
    }

    public void uploadLocalDataToFirebase() {
        List<Medication> localMedications = getAllMedications();
        Log.d("DatabaseHelper", "Checking medications to upload, total " + localMedications.size());

        int needUploadCount = 0;
        for (Medication medication : localMedications) {
            if (medication.getFirebaseId() == null || medication.getFirebaseId().isEmpty()) {
                needUploadCount++;
                Log.d("DatabaseHelper", "Need to upload medication to Firebase: " + medication.getName());

                firebaseManager.saveMedication(medication, new FirebaseManager.FirebaseCallback<String>() {
                    @Override
                    public void onSuccess(String firebaseId) {
                        SQLiteDatabase db = null;
                        try {
                            db = getWritableDatabase();
                            ContentValues values = new ContentValues();
                            values.put(COL_FIREBASE_ID, firebaseId);
                            db.update(TABLE_MEDICATIONS, values, COL_ID + " = ?", new String[]{String.valueOf(medication.getId())});
                            Log.d("DatabaseHelper", "Medication upload successful: " + medication.getName() + " -> " + firebaseId);
                        } finally {
                            if (db != null) {
                                db.close();
                            }
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("DatabaseHelper", "Medication upload failed: " + medication.getName() + " - " + error);
                    }
                });
            } else {
                Log.d("DatabaseHelper", "Medication already has Firebase ID, skipping upload: " + medication.getName() + " -> " + medication.getFirebaseId());
            }
        }

        Log.d("DatabaseHelper", "Total need to upload " + needUploadCount + " medications to Firebase");
    }
}