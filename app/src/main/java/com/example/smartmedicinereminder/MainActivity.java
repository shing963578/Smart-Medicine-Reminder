package com.example.smartmedicinereminder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MedicationAdapter.OnSelectionModeChangeListener {
    private static final int PERMISSION_REQUEST_CODE = 123;
    private RecyclerView medicationRecyclerView;
    private MedicationAdapter medicationAdapter;
    private List<Medication> medicationList;
    private Button btnAddMedication, btnAIAssistant, btnMedicationLog;
    private TextView tvEmptyMessage;
    private FloatingActionButton fabBatchDelete;
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        initializeViews();
        setupRecyclerView();
        setupClickListeners();

        checkAndRequestPermissions();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        String[] basicPermissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.VIBRATE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.RECEIVE_BOOT_COMPLETED
        };

        for (String permission : basicPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.SCHEDULE_EXACT_ALARM);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.USE_BIOMETRIC) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.USE_BIOMETRIC);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.USE_FINGERPRINT);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            onPermissionsGranted();
        }
    }

    private void onPermissionsGranted() {
        loadMedications();
    }

    private void initializeViews() {
        medicationRecyclerView = findViewById(R.id.recyclerViewMedications);
        btnAddMedication = findViewById(R.id.btnAddMedication);
        btnAIAssistant = findViewById(R.id.btnAIAssistant);
        btnMedicationLog = findViewById(R.id.btnMedicationLog);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        fabBatchDelete = findViewById(R.id.fabBatchDelete);
    }

    private void setupRecyclerView() {
        medicationList = new ArrayList<>();
        medicationAdapter = new MedicationAdapter(medicationList, this);
        medicationAdapter.setSelectionModeListener(this);
        medicationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        medicationRecyclerView.setAdapter(medicationAdapter);
        medicationRecyclerView.setItemAnimator(null);
    }

    private void setupClickListeners() {
        btnAddMedication.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddMedicationActivity.class);
            startActivity(intent);
        });

        btnAIAssistant.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AIAssistantActivity.class);
            startActivity(intent);
        });

        btnMedicationLog.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MedicationLogActivity.class);
            startActivity(intent);
        });

        if (fabBatchDelete != null) {
            fabBatchDelete.setOnClickListener(v -> {
                if (medicationAdapter.isSelectionMode()) {
                    List<Medication> selectedMedications = medicationAdapter.getSelectedMedications();
                    if (!selectedMedications.isEmpty()) {
                        showBatchDeleteConfirmation(selectedMedications.size());
                    } else {
                        Toast.makeText(this, "Please select medications to delete", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    medicationAdapter.setSelectionMode(true);
                }
            });
        }
    }

    private void showBatchDeleteConfirmation(int count) {
        new AlertDialog.Builder(this)
                .setTitle("Batch Delete Confirmation")
                .setMessage("Are you sure you want to delete " + count + " selected medications?\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    medicationAdapter.deleteSelectedMedications();
                    updateEmptyState();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onSelectionModeChanged(boolean isSelectionMode, int selectedCount) {
        if (fabBatchDelete != null) {
            if (isSelectionMode) {
                fabBatchDelete.setImageResource(R.drawable.ic_close);
                if (selectedCount > 0) {
                    fabBatchDelete.setContentDescription("Delete selected " + selectedCount + " medications");
                } else {
                    fabBatchDelete.setContentDescription("Exit selection mode");
                }
            } else {
                fabBatchDelete.setImageResource(R.drawable.ic_close);
                fabBatchDelete.setContentDescription("Batch delete");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_voice_settings) {
            Intent intent = new Intent(this, VoiceSettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_reminder_settings) {
            Intent intent = new Intent(this, ReminderSettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadMedications() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);

        dbHelper.removeDuplicateMedications();

        medicationList.clear();
        medicationList.addAll(dbHelper.getAllMedications());
        medicationAdapter.notifyDataSetChanged();

        updateEmptyState();

        Log.d("MainActivity", "Loaded " + medicationList.size() + " medications");
    }

    private void updateEmptyState() {
        if (medicationList.isEmpty()) {
            medicationRecyclerView.setVisibility(View.GONE);
            tvEmptyMessage.setVisibility(View.VISIBLE);
            if (fabBatchDelete != null) {
                fabBatchDelete.setVisibility(View.GONE);
            }
        } else {
            medicationRecyclerView.setVisibility(View.VISIBLE);
            tvEmptyMessage.setVisibility(View.GONE);
            if (fabBatchDelete != null) {
                fabBatchDelete.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasRequiredPermissions()) {
            loadMedications();
        }
    }

    @Override
    public void onBackPressed() {
        if (medicationAdapter != null && medicationAdapter.isSelectionMode()) {
            medicationAdapter.setSelectionMode(false);
        } else {
            super.onBackPressed();
        }
    }

    private void syncDataWithFirebase() {
        Log.d("MainActivity", "Starting data synchronization");
        DatabaseHelper dbHelper = new DatabaseHelper(this);

        dbHelper.removeDuplicateMedications();

        new android.os.Handler().postDelayed(() -> {
            dbHelper.uploadLocalDataToFirebase();

            new android.os.Handler().postDelayed(() -> {
                dbHelper.syncWithFirebase();

                new android.os.Handler().postDelayed(() -> {
                    loadMedications();
                }, 1000);
            }, 1500);
        }, 1000);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isFirstLoad) {
            syncDataWithFirebase();
            isFirstLoad = false;
        }
    }

    private boolean hasRequiredPermissions() {
        String[] requiredPermissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.VIBRATE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.RECEIVE_BOOT_COMPLETED
        };

        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allCriticalPermissionsGranted = true;
            List<String> deniedPermissions = new ArrayList<>();

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i]);

                    if (isCriticalPermission(permissions[i])) {
                        allCriticalPermissionsGranted = false;
                    }
                }
            }

            if (allCriticalPermissionsGranted) {
                Toast.makeText(this, "Permission setup completed", Toast.LENGTH_SHORT).show();
                onPermissionsGranted();
            } else {
                String message = "Some important permissions were not granted, which may affect app functionality:\n";
                for (String permission : deniedPermissions) {
                    if (isCriticalPermission(permission)) {
                        message += "• " + getPermissionDescription(permission) + "\n";
                    }
                }
                message += "\nYou can manually grant these permissions later in settings.";
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                onPermissionsGranted();
            }
        }
    }

    private boolean isCriticalPermission(String permission) {
        return permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) ||
                permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                permission.equals(Manifest.permission.CAMERA);
    }

    private String getPermissionDescription(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return "Location permission (for recording medication location)";
            case Manifest.permission.CAMERA:
                return "Camera permission (for taking prescription photos)";
            case Manifest.permission.POST_NOTIFICATIONS:
                return "Notification permission (for medication reminders)";
            case Manifest.permission.SCHEDULE_EXACT_ALARM:
                return "Exact alarm permission (for precise reminders)";
            case Manifest.permission.USE_BIOMETRIC:
            case Manifest.permission.USE_FINGERPRINT:
                return "Biometric permission (for security verification)";
            default:
                return permission;
        }
    }
}