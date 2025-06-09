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

public class AddMedicationActivity extends AppCompatActivity {
    private static final String TAG = "AddMedicationActivity";

    private EditText etMedicationName, etDosage, etFrequency, etStartDate, etEndDate, etNotes, etInitialTime;
    private Button btnTakePhoto, btnSaveMedication;
    private ImageView ivPrescription;
    private Spinner spinnerIntervalHours;
    private String prescriptionImagePath;
    private String prescriptionImageUrl;
    private File photoFile;
    private int selectedIntervalHours = 1;
    private boolean isUploading = false;
    private boolean hasPrescriptionImage = false;

    private ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Camera result callback: " + result.getResultCode());
                if (result.getResultCode() == RESULT_OK) {
                    Log.d(TAG, "Camera capture successful, file path: " + photoFile.getAbsolutePath());
                    ivPrescription.setImageURI(Uri.fromFile(photoFile));
                    prescriptionImagePath = photoFile.getAbsolutePath();
                    hasPrescriptionImage = true;

                    btnTakePhoto.setText("Retake Prescription Photo");
                    Toast.makeText(this, "Photo ready, will be uploaded when saving medication", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Camera capture failed, result code: " + result.getResultCode());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medication);

        setupToolbar();
        initializeViews();
        setupClickListeners();
        setupSpinner();
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
        btnSaveMedication.setOnClickListener(v -> saveMedication());

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

    private void saveMedication() {
        Log.d(TAG, "saveMedication called, isUploading: " + isUploading);

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

        List<String> times = new ArrayList<>();
        times.add(initialTime);

        Medication medication = new Medication(name, dosage, frequency, times,
                startDate, endDate, notes, selectedIntervalHours);

        if (hasPrescriptionImage && prescriptionImagePath != null) {
            uploadImageAndSaveMedication(medication);
        } else {
            saveMedicationToDatabase(medication);
        }
    }

    private void uploadImageAndSaveMedication(Medication medication) {
        Log.d(TAG, "Starting image upload and save medication");

        isUploading = true;
        btnSaveMedication.setText("Uploading image...");
        btnSaveMedication.setEnabled(false);
        btnTakePhoto.setEnabled(false);

        Toast.makeText(this, "Uploading image, please wait...", Toast.LENGTH_SHORT).show();

        ImageUploadService.uploadImage(prescriptionImagePath, new ImageUploadService.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                Log.d(TAG, "Image upload successful: " + imageUrl);
                runOnUiThread(() -> {
                    prescriptionImageUrl = imageUrl;
                    medication.setPrescriptionImagePath(imageUrl);

                    btnSaveMedication.setText("Saving medication...");
                    saveMedicationToDatabase(medication);
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Image upload failed: " + error);
                runOnUiThread(() -> {
                    isUploading = false;
                    btnSaveMedication.setText("Save Medication");
                    btnSaveMedication.setEnabled(true);
                    btnTakePhoto.setEnabled(true);

                    new androidx.appcompat.app.AlertDialog.Builder(AddMedicationActivity.this)
                            .setTitle("Image Upload Failed")
                            .setMessage("Image upload failed: " + error + "\n\nYou can:")
                            .setPositiveButton("Continue with local image", (dialog, which) -> {
                                medication.setPrescriptionImagePath(prescriptionImagePath);
                                saveMedicationToDatabase(medication);
                            })
                            .setNegativeButton("Retry upload", (dialog, which) -> {
                                uploadImageAndSaveMedication(medication);
                            })
                            .setNeutralButton("Cancel", null)
                            .show();
                });
            }
        });
    }

    private void saveMedicationToDatabase(Medication medication) {
        Log.d(TAG, "Save medication to database");

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        long result = dbHelper.addMedication(medication);

        if (result != -1) {
            medication.setId((int) result);
            AlarmScheduler.scheduleAlarms(this, medication);

            isUploading = false;
            btnSaveMedication.setText("Save Medication");
            btnSaveMedication.setEnabled(true);
            btnTakePhoto.setEnabled(true);

            Toast.makeText(this, "Medication added successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            isUploading = false;
            btnSaveMedication.setText("Save Medication");
            btnSaveMedication.setEnabled(true);
            btnTakePhoto.setEnabled(true);

            Toast.makeText(this, "Failed to add medication", Toast.LENGTH_SHORT).show();
        }
    }
}