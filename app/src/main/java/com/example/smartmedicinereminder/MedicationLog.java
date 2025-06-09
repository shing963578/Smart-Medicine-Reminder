package com.example.smartmedicinereminder;

public class MedicationLog {
    private int id;
    private int medicationId;
    private String medicationName;
    private String takenTime;
    private double locationLat;
    private double locationLng;
    private String locationAddress;

    public MedicationLog() {}

    public MedicationLog(int medicationId, String medicationName, String takenTime,
                         double locationLat, double locationLng, String locationAddress) {
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.takenTime = takenTime;
        this.locationLat = locationLat;
        this.locationLng = locationLng;
        this.locationAddress = locationAddress;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMedicationId() { return medicationId; }
    public void setMedicationId(int medicationId) { this.medicationId = medicationId; }

    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }

    public String getTakenTime() { return takenTime; }
    public void setTakenTime(String takenTime) { this.takenTime = takenTime; }

    public double getLocationLat() { return locationLat; }
    public void setLocationLat(double locationLat) { this.locationLat = locationLat; }

    public double getLocationLng() { return locationLng; }
    public void setLocationLng(double locationLng) { this.locationLng = locationLng; }

    public String getLocationAddress() { return locationAddress; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }
}