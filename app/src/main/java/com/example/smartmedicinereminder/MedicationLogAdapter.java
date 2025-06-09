package com.example.smartmedicinereminder;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MedicationLogAdapter extends RecyclerView.Adapter<MedicationLogAdapter.LogViewHolder> {
    private List<MedicationLog> logs;
    private Context context;

    public MedicationLogAdapter(List<MedicationLog> logs, Context context) {
        this.logs = logs;
        this.context = context;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medication_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        MedicationLog log = logs.get(position);

        holder.tvMedicationName.setText(log.getMedicationName());

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(log.getTakenTime());
            holder.tvTakenTime.setText("Taken at: " + outputFormat.format(date));
        } catch (ParseException e) {
            holder.tvTakenTime.setText("Taken at: " + log.getTakenTime());
        }

        String locationText = "";
        boolean hasValidCoordinates = (log.getLocationLat() != 0.0 || log.getLocationLng() != 0.0);
        String addressText = log.getLocationAddress();

        if (hasValidCoordinates) {
            if (addressText != null && !addressText.isEmpty() && !addressText.equals("null")) {
                locationText = "Location: " + addressText;
            } else {
                locationText = String.format(Locale.getDefault(), "Location: %.6f, %.6f",
                        log.getLocationLat(), log.getLocationLng());
            }
        } else if (addressText != null && !addressText.isEmpty() &&
                !addressText.equals("null") &&
                !addressText.equals("Marked as taken via notification") &&
                !addressText.contains("Location permission") &&
                !addressText.contains("Unable to get location") &&
                !addressText.contains("Failed to get location")) {
            locationText = "Location: " + addressText;
        } else {
            locationText = "Location: Location information unavailable";
        }

        holder.tvLocation.setText(locationText);
        holder.tvLocation.setVisibility(View.VISIBLE);

        holder.btnDelete.setOnClickListener(v -> showDeleteConfirmation(log, position));
    }

    private void showDeleteConfirmation(MedicationLog log, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Record")
                .setMessage("Are you sure you want to delete this medication record?\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteLog(log, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteLog(MedicationLog log, int position) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        if (dbHelper.deleteMedicationLog(log.getId())) {
            logs.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, logs.size());
            Toast.makeText(context, "Medication record deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvMedicationName, tvTakenTime, tvLocation;
        ImageButton btnDelete;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMedicationName = itemView.findViewById(R.id.tvMedicationName);
            tvTakenTime = itemView.findViewById(R.id.tvTakenTime);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}