package com.example.smartmedicinereminder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder> {
    private List<Medication> medications;
    private Context context;
    private boolean isSelectionMode = false;
    private List<Integer> selectedPositions = new ArrayList<>();
    private OnSelectionModeChangeListener selectionModeListener;

    public interface OnSelectionModeChangeListener {
        void onSelectionModeChanged(boolean isSelectionMode, int selectedCount);
    }

    public MedicationAdapter(List<Medication> medications, Context context) {
        this.medications = medications;
        this.context = context;
    }

    public void setSelectionModeListener(OnSelectionModeChangeListener listener) {
        this.selectionModeListener = listener;
    }

    public void setSelectionMode(boolean enabled) {
        isSelectionMode = enabled;
        if (!enabled) {
            selectedPositions.clear();
        }
        notifyDataSetChanged();
        if (selectionModeListener != null) {
            selectionModeListener.onSelectionModeChanged(isSelectionMode, selectedPositions.size());
        }
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public List<Medication> getSelectedMedications() {
        List<Medication> selected = new ArrayList<>();
        for (int position : selectedPositions) {
            if (position < medications.size()) {
                selected.add(medications.get(position));
            }
        }
        return selected;
    }

    public void deleteSelectedMedications() {
        List<Medication> toDelete = getSelectedMedications();
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        for (Medication medication : toDelete) {
            AlarmScheduler.cancelAlarms(context, medication);

            if (medication.getPrescriptionImagePath() != null &&
                    medication.getPrescriptionImagePath().startsWith("http")) {
                ImageUploadService.deleteImage(medication.getPrescriptionImagePath(),
                        new ImageUploadService.UploadCallback() {
                            @Override
                            public void onSuccess(String result) {}

                            @Override
                            public void onError(String error) {}
                        });
            }

            dbHelper.deleteMedication(medication.getId());
        }

        List<Integer> sortedPositions = new ArrayList<>(selectedPositions);
        sortedPositions.sort((a, b) -> b.compareTo(a));

        for (int position : sortedPositions) {
            if (position < medications.size()) {
                medications.remove(position);
            }
        }

        selectedPositions.clear();
        setSelectionMode(false);
        notifyDataSetChanged();

        Toast.makeText(context, "Deleted " + toDelete.size() + " medications", Toast.LENGTH_SHORT).show();
    }

    @NonNull
    @Override
    public MedicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_medication, parent, false);
        return new MedicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicationViewHolder holder, int position) {
        Medication medication = medications.get(position);

        holder.tvMedicationName.setText(medication.getName());
        holder.tvDosage.setText("Dosage: " + medication.getDosage());
        holder.tvFrequency.setText("Frequency: " + medication.getFrequency());

        StringBuilder timesText = new StringBuilder("Time: ");
        if (medication.getTimes() != null && !medication.getTimes().isEmpty()) {
            for (int i = 0; i < medication.getTimes().size(); i++) {
                timesText.append(medication.getTimes().get(i));
                if (i < medication.getTimes().size() - 1) {
                    timesText.append(", ");
                }
            }
        } else {
            timesText.append("Not set");
        }
        holder.tvTimes.setText(timesText.toString());

        if (medication.getNotes() != null && !medication.getNotes().isEmpty()) {
            holder.tvNotes.setText("Notes: " + medication.getNotes());
            holder.tvNotes.setVisibility(View.VISIBLE);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }

        if (medication.getPrescriptionImagePath() != null && !medication.getPrescriptionImagePath().isEmpty()) {
            String imagePath = medication.getPrescriptionImagePath();
            if (imagePath.startsWith("http")) {
                ImageLoader.loadImageFromUrl(imagePath, holder.ivPrescription);
                holder.ivPrescription.setVisibility(View.VISIBLE);
            } else {
                ImageLoader.loadLocalImage(imagePath, holder.ivPrescription);
                holder.ivPrescription.setVisibility(View.VISIBLE);
            }
        } else {
            holder.ivPrescription.setVisibility(View.GONE);
        }

        if (isSelectionMode) {
            holder.checkbox.setVisibility(View.VISIBLE);
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
            holder.checkbox.setChecked(selectedPositions.contains(position));

            holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedPositions.contains(position)) {
                        selectedPositions.add(position);
                    }
                } else {
                    selectedPositions.remove(Integer.valueOf(position));
                }
                if (selectionModeListener != null) {
                    selectionModeListener.onSelectionModeChanged(isSelectionMode, selectedPositions.size());
                }
            });
        } else {
            holder.checkbox.setVisibility(View.GONE);
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.checkbox.setOnCheckedChangeListener(null);
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(position);
            } else {
                showMedicationDetails(medication);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                setSelectionMode(true);
                toggleSelection(position);
                return true;
            }
            return false;
        });

        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditMedicationActivity.class);
            intent.putExtra("medication_id", medication.getId());
            context.startActivity(intent);
        });

        holder.btnDelete.setOnClickListener(v -> showDeleteConfirmation(medication, position));
    }

    private void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(Integer.valueOf(position));
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
        if (selectionModeListener != null) {
            selectionModeListener.onSelectionModeChanged(isSelectionMode, selectedPositions.size());
        }
    }

    private void showMedicationDetails(Medication medication) {
        StringBuilder details = new StringBuilder();
        details.append("Medication Name: ").append(medication.getName()).append("\n\n");
        details.append("Dosage: ").append(medication.getDosage()).append("\n\n");
        details.append("Frequency: ").append(medication.getFrequency()).append("\n\n");
        details.append("Interval: ").append(medication.getIntervalHours()).append(" hours\n\n");

        if (medication.getTimes() != null && !medication.getTimes().isEmpty()) {
            details.append("Medication Times: ");
            for (int i = 0; i < medication.getTimes().size(); i++) {
                details.append(medication.getTimes().get(i));
                if (i < medication.getTimes().size() - 1) {
                    details.append(", ");
                }
            }
            details.append("\n\n");
        }

        if (medication.getStartDate() != null && !medication.getStartDate().isEmpty()) {
            details.append("Start Date: ").append(medication.getStartDate()).append("\n\n");
        }

        if (medication.getEndDate() != null && !medication.getEndDate().isEmpty()) {
            details.append("End Date: ").append(medication.getEndDate()).append("\n\n");
        }

        if (medication.getNotes() != null && !medication.getNotes().isEmpty()) {
            details.append("Notes: ").append(medication.getNotes()).append("\n\n");
        }

        new AlertDialog.Builder(context)
                .setTitle("Medication Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showDeleteConfirmation(Medication medication, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Medication")
                .setMessage("Are you sure you want to delete \"" + medication.getName() + "\"?\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteMedication(medication, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMedication(Medication medication, int position) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        AlarmScheduler.cancelAlarms(context, medication);

        if (medication.getPrescriptionImagePath() != null &&
                medication.getPrescriptionImagePath().startsWith("http")) {
            ImageUploadService.deleteImage(medication.getPrescriptionImagePath(),
                    new ImageUploadService.UploadCallback() {
                        @Override
                        public void onSuccess(String result) {
                        }

                        @Override
                        public void onError(String error) {
                        }
                    });
        }

        if (dbHelper.deleteMedication(medication.getId())) {
            medications.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, medications.size());
            Toast.makeText(context, "Deleted " + medication.getName(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return medications.size();
    }

    static class MedicationViewHolder extends RecyclerView.ViewHolder {
        TextView tvMedicationName, tvDosage, tvFrequency, tvTimes, tvNotes;
        ImageButton btnEdit, btnDelete;
        ImageView ivPrescription;
        CheckBox checkbox;

        public MedicationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMedicationName = itemView.findViewById(R.id.tvMedicationName);
            tvDosage = itemView.findViewById(R.id.tvDosage);
            tvFrequency = itemView.findViewById(R.id.tvFrequency);
            tvTimes = itemView.findViewById(R.id.tvTimes);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            ivPrescription = itemView.findViewById(R.id.ivPrescription);
            checkbox = itemView.findViewById(R.id.checkbox);
        }
    }
}