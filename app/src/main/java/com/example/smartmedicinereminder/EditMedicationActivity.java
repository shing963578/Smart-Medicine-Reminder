package com.example.smartmedicinereminder;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.google.android.material.appbar.MaterialToolbar;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditMedicationActivity extends AppCompatActivity {
    private static final String TAG = "EditMedicationActivity";

    private EditText etMedicationName, etDosage, etFrequency, etStartDate, etEndDate, etNotes, etInitialTime;
    private Button btnTakePhoto, btnSaveMedication;
    private ImageView ivPrescription;
    private Spinner spinnerIntervalHours;
    private String prescriptionImagePath;
    private String prescriptionImageUrl;
    private String oldImageUrl;
    private File photoFile;
    private Medication currentMedication;
    private int medicationId;
    private int selectedIntervalHours = 1;
    private boolean isUploading = false;
    private boolean hasNewPrescriptionImage = false;

    private ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Camera result callback: " + result.getResultCode());
                if (result.getResultCode() == RESULT_OK) {
                    Log.d(TAG, "Camera capture successful, file path: " + photoFile.getAbsolutePath());
                    ivPrescription.setImageURI(Uri.fromFile(photoFile));
                    prescriptionImagePath = photoFile.getAbsolutePath();
                    hasNewPrescriptionImage = true;

                    btnTakePhoto.setText("Retake Prescription Photo");
                    Toast.makeText(this, "New photo ready, will be uploaded when updating medication", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Camera capture failed, result code: " + result.getResultCode());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_medication);

        medicationId = getIntent().getIntExtra("medication_id", -1);
        if (medicationId == -1) {
            Toast.makeText(this, "Error: Invalid medication ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        initializeViews();
        setupClickListeners();
        setupSpinner();
        loadMedicationData();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initializeViews() {
        etMedicationName = findViewById(R.id.etMedicationName);
        etDosage = findViewById(R.id.etDosage);
        etFrequency = findViewById(R.id.etFrequency);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etNotes = findViewById(R.id.etNotes);
        etInitialTime = findViewById(R.id.etInitialTime);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSaveMedication = findViewById(R.id.btnSaveMedication);
        ivPrescription = findViewById(R.id.ivPrescription);
        spinnerIntervalHours = findViewById(R.id.spinnerIntervalHours);
    }

    private void setupSpinner() {
        List<String> intervalOptions = new ArrayList<>();
        for (int i = 1; i <= 24; i++) {
            intervalOptions.add(i + " hours");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, intervalOptions) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view != null) {
                    view.setPadding(16, 16, 16, 16);
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIntervalHours.setAdapter(adapter);

        spinnerIntervalHours.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedIntervalHours = position + 1;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void validateIntervalWithFrequency() {
        String frequency = etFrequency.getText().toString().trim();
        if (!frequency.isEmpty()) {
            try {
                int timesPerDay = extractTimesPerDay(frequency);
                if (timesPerDay > 0) {
                    int maxInterval = 24 / timesPerDay;
                    updateSpinnerOptions(maxInterval);

                    if (selectedIntervalHours > maxInterval) {
                        selectedIntervalHours = maxInterval;
                        spinnerIntervalHours.setSelection(selectedIntervalHours - 1);
                        Toast.makeText(this, "Adjusted interval to " + maxInterval + " hours based on frequency", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                resetSpinnerToDefault();
            }
        } else {
            resetSpinnerToDefault();
        }
    }

    private int extractTimesPerDay(String frequency) {
        try {
            if (frequency.contains("daily") && frequency.contains("times")) {
                String[] parts = frequency.split("daily");
                if (parts.length >= 2) {
                    String timePart = parts[1].replace("times", "").trim();
                    return Integer.parseInt(timePart);
                }
            }

            if (frequency.matches(".*\\d+.*")) {
                String numbers = frequency.replaceAll("[^0-9]", "");
                if (!numbers.isEmpty()) {
                    return Integer.parseInt(numbers);
                }
            }
        } catch (Exception e) {
        }
        return 0;
    }

    private void updateSpinnerOptions(int maxHours) {
        List<String> intervalOptions = new ArrayList<>();
        for (int i = 1; i <= Math.min(maxHours, 24); i++) {
            intervalOptions.add(i + " hours");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, intervalOptions) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view != null) {
                    view.setPadding(16, 16, 16, 16);
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIntervalHours.setAdapter(adapter);

        if (selectedIntervalHours > maxHours) {
            selectedIntervalHours = maxHours;
        }
        spinnerIntervalHours.setSelection(selectedIntervalHours - 1);
    }

    private void resetSpinnerToDefault() {
        List<String> intervalOptions = new ArrayList<>();
        for (int i = 1; i <= 24; i++) {
            intervalOptions.add(i + " hours");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, intervalOptions) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view != null) {
                    view.setPadding(16, 16, 16, 16);
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIntervalHours.setAdapter(adapter);
        spinnerIntervalHours.setSelection(selectedIntervalHours - 1);
    }

    private void setupClickListeners() {
        btnTakePhoto.setOnClickListener(v -> takePhoto());
        btnSaveMedication.setOnClickListener(v -> updateMedication());

        etStartDate.setOnClickListener(v -> showDatePickerDialog(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePickerDialog(etEndDate));
        etInitialTime.setOnClickListener(v -> showTimePickerDialog());

        etFrequency.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateIntervalWithFrequency();
            }
        });
    }

    private void loadMedicationData() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        List<Medication> medications = dbHelper.getAllMedications();

        for (Medication medication : medications) {
            if (medication.getId() == medicationId) {
                currentMedication = medication;
                break;
            }
        }

        if (currentMedication == null) {
            Toast.makeText(this, "Medication not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etMedicationName.setText(currentMedication.getName());
        etDosage.setText(currentMedication.getDosage());
        etFrequency.setText(currentMedication.getFrequency());
        etStartDate.setText(currentMedication.getStartDate());
        etEndDate.setText(currentMedication.getEndDate());
        etNotes.setText(currentMedication.getNotes());

        selectedIntervalHours = currentMedication.getIntervalHours();
        spinnerIntervalHours.setSelection(selectedIntervalHours - 1);

        if (currentMedication.getTimes() != null && !currentMedication.getTimes().isEmpty()) {
            etInitialTime.setText(currentMedication.getTimes().get(0));
        }

        if (currentMedication.getPrescriptionImagePath() != null && !currentMedication.getPrescriptionImagePath().isEmpty()) {
            oldImageUrl = currentMedication.getPrescriptionImagePath();
            String imagePath = currentMedication.getPrescriptionImagePath();

            if (imagePath.startsWith("http")) {
                ImageLoader.loadImageFromUrl(imagePath, ivPrescription);
                ivPrescription.setVisibility(View.VISIBLE);
            } else {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    ivPrescription.setImageURI(Uri.fromFile(imageFile));
                    ivPrescription.setVisibility(View.VISIBLE);
                } else {
                    ivPrescription.setVisibility(View.GONE);
                }
            }
        }
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    etInitialTime.setText(time);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private void showDatePickerDialog(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    editText.setText(sdf.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void takePhoto() {
        Log.d(TAG, "takePhoto called");

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                photoFile = createImageFile();
                if (photoFile != null) {
                    Log.d(TAG, "Image file created successfully: " + photoFile.getAbsolutePath());
                    Uri photoURI = FileProvider.getUriForFile(this,
                            "com.example.smartmedicinereminder.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    cameraLauncher.launch(takePictureIntent);
                } else {
                    Log.e(TAG, "Failed to create image file");
                }
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "No camera application available");
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "PRESCRIPTION_" + timeStamp + "_";
        File storageDir = getExternalFilesDir("prescriptions");
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void updateMedication() {
        Log.d(TAG, "updateMedication called, isUploading: " + isUploading);

        if (isUploading) {
            Toast.makeText(this, "Image is uploading, please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etMedicationName.getText().toString().trim();
        String dosage = etDosage.getText().toString().trim();
        String frequency = etFrequency.getText().toString().trim();
        String startDate = etStartDate.getText().toString().trim();
        String endDate = etEndDate.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        String initialTime = etInitialTime.getText().toString().trim();

        if (name.isEmpty() || dosage.isEmpty() || frequency.isEmpty() || initialTime.isEmpty()) {
            Toast.makeText(this, "Please fill in required information", Toast.LENGTH_SHORT).show();
            return;
        }

        AlarmScheduler.cancelAlarms(this, currentMedication);

        List<String> times = new ArrayList<>();
        times.add(initialTime);

        currentMedication.setName(name);
        currentMedication.setDosage(dosage);
        currentMedication.setFrequency(frequency);
        currentMedication.setTimes(times);
        currentMedication.setStartDate(startDate);
        currentMedication.setEndDate(endDate);
        currentMedication.setNotes(notes);
        currentMedication.setIntervalHours(selectedIntervalHours);

        if (hasNewPrescriptionImage && prescriptionImagePath != null) {
            uploadImageAndUpdateMedication(currentMedication);
        } else {
            updateMedicationToDatabase(currentMedication);
        }
    }

    private void uploadImageAndUpdateMedication(Medication medication) {
        Log.d(TAG, "Starting new image upload and medication update");

        isUploading = true;
        btnSaveMedication.setText("Uploading image...");
        btnSaveMedication.setEnabled(false);
        btnTakePhoto.setEnabled(false);

        Toast.makeText(this, "Uploading new image, please wait...", Toast.LENGTH_SHORT).show();

        ImageUploadService.uploadImage(prescriptionImagePath, new ImageUploadService.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                Log.d(TAG, "New image upload successful: " + imageUrl);
                runOnUiThread(() -> {
                    prescriptionImageUrl = imageUrl;
                    medication.setPrescriptionImagePath(imageUrl);

                    if (oldImageUrl != null && oldImageUrl.startsWith("http") && !oldImageUrl.equals(imageUrl)) {
                        Log.d(TAG, "Preparing to delete old image: " + oldImageUrl);
                        deleteOldImageAndUpdateMedication(medication);
                    } else {
                        Log.d(TAG, "No old image to delete, updating medication directly");
                        btnSaveMedication.setText("Updating medication...");
                        updateMedicationToDatabase(medication);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "New image upload failed: " + error);
                runOnUiThread(() -> {
                    isUploading = false;
                    btnSaveMedication.setText("Update Medication");
                    btnSaveMedication.setEnabled(true);
                    btnTakePhoto.setEnabled(true);

                    new androidx.appcompat.app.AlertDialog.Builder(EditMedicationActivity.this)
                            .setTitle("Image Upload Failed")
                            .setMessage("New image upload failed: " + error + "\n\nYou can:")
                            .setPositiveButton("Continue with local image", (dialog, which) -> {
                                medication.setPrescriptionImagePath(prescriptionImagePath);
                                updateMedicationToDatabase(medication);
                            })
                            .setNegativeButton("Retry upload", (dialog, which) -> {
                                uploadImageAndUpdateMedication(medication);
                            })
                            .setNeutralButton("Keep original image", (dialog, which) -> {
                                medication.setPrescriptionImagePath(oldImageUrl);
                                updateMedicationToDatabase(medication);
                            })
                            .show();
                });
            }
        });
    }

    private void deleteOldImageAndUpdateMedication(Medication medication) {
        Log.d(TAG, "Starting old image deletion: " + oldImageUrl);

        ImageUploadService.deleteImage(oldImageUrl, new ImageUploadService.UploadCallback() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "Old image deletion successful: " + oldImageUrl);
                runOnUiThread(() -> {
                    btnSaveMedication.setText("Updating medication...");
                    updateMedicationToDatabase(medication);
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Old image deletion failed: " + oldImageUrl + ", error: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(EditMedicationActivity.this, "Old image deletion failed, but medication will be updated normally", Toast.LENGTH_SHORT).show();
                    btnSaveMedication.setText("Updating medication...");
                    updateMedicationToDatabase(medication);
                });
            }
        });

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isUploading) {
                Log.w(TAG, "Old image deletion timeout, updating medication directly");
                Toast.makeText(EditMedicationActivity.this, "Old image deletion timeout, continuing with medication update", Toast.LENGTH_SHORT).show();
                btnSaveMedication.setText("Updating medication...");
                updateMedicationToDatabase(medication);
            }
        }, 5000);
    }

    private void updateMedicationToDatabase(Medication medication) {
        Log.d(TAG, "Updating medication to database");

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        boolean result = dbHelper.updateMedication(medication);

        if (result) {
            AlarmScheduler.scheduleAlarms(this, medication);

            isUploading = false;
            btnSaveMedication.setText("Update Medication");
            btnSaveMedication.setEnabled(true);
            btnTakePhoto.setEnabled(true);

            Toast.makeText(this, "Medication updated successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            isUploading = false;
            btnSaveMedication.setText("Update Medication");
            btnSaveMedication.setEnabled(true);
            btnTakePhoto.setEnabled(true);

            Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
        }
    }
}