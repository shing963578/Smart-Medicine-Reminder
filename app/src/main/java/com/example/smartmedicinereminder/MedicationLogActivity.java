package com.example.smartmedicinereminder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.List;
import java.util.concurrent.Executor;

public class MedicationLogActivity extends AppCompatActivity {
    private RecyclerView recyclerViewLogs;
    private MedicationLogAdapter logAdapter;
    private List<MedicationLog> logList;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication_log);

        setupToolbar();
        setupBiometricAuthentication();
        authenticateUser();
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

    private void setupBiometricAuthentication() {
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(MedicationLogActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(MedicationLogActivity.this, "Authentication successful", Toast.LENGTH_SHORT).show();
                initializeViews();
                loadMedicationLogs();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(MedicationLogActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Fingerprint Authentication")
                .setSubtitle("Please use fingerprint to verify identity to view medication records")
                .setNegativeButtonText("Cancel")
                .build();
    }

    private void authenticateUser() {
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                biometricPrompt.authenticate(promptInfo);
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this, "Device does not support fingerprint authentication", Toast.LENGTH_SHORT).show();
                initializeViews();
                loadMedicationLogs();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, "Fingerprint hardware unavailable", Toast.LENGTH_SHORT).show();
                initializeViews();
                loadMedicationLogs();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this, "No fingerprint enrolled", Toast.LENGTH_SHORT).show();
                initializeViews();
                loadMedicationLogs();
                break;
            default:
                Toast.makeText(this, "Fingerprint authentication unavailable", Toast.LENGTH_SHORT).show();
                initializeViews();
                loadMedicationLogs();
                break;
        }
    }

    private void initializeViews() {
        recyclerViewLogs = findViewById(R.id.recyclerViewLogs);
        recyclerViewLogs.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadMedicationLogs() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        logList = dbHelper.getAllMedicationLogs();
        logAdapter = new MedicationLogAdapter(logList, this);
        recyclerViewLogs.setAdapter(logAdapter);
    }
}