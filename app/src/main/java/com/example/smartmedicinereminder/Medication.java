package com.example.smartmedicinereminder;

import java.util.List;

public class Medication {
    private int id;
    private String firebaseId;
    private String name;
    private String dosage;
    private String frequency;
    private List<String> times;
    private String startDate;
    private String endDate;
    private String notes;
    private String prescriptionImagePath;
    private boolean isActive;
    private int intervalHours;

    public Medication() {}

    public Medication(String name, String dosage, String frequency, List<String> times,
                      String startDate, String endDate, String notes, int intervalHours) {
        this.name = name;
        this.dosage = dosage;
        this.frequency = frequency;
        this.times = times;
        this.startDate = startDate;
        this.endDate = endDate;
        this.notes = notes;
        this.intervalHours = intervalHours;
        this.isActive = true;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFirebaseId() { return firebaseId; }
    public void setFirebaseId(String firebaseId) { this.firebaseId = firebaseId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public List<String> getTimes() { return times; }
    public void setTimes(List<String> times) { this.times = times; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPrescriptionImagePath() { return prescriptionImagePath; }
    public void setPrescriptionImagePath(String prescriptionImagePath) {
        this.prescriptionImagePath = prescriptionImagePath;
    }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getIntervalHours() { return intervalHours; }
    public void setIntervalHours(int intervalHours) { this.intervalHours = intervalHours; }
}