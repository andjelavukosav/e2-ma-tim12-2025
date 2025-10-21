package com.example.mobil2025.model;

public class UserEquipment {
    public String equipmentId;    // UID opreme iz master kataloga
    public boolean active;        // za odeću ili trajni napitak
    public boolean consumed;      // za jednokratne napitke
    public int quantity;          // koliko napitaka korisnik ima
    public int remainingBattles;  // za odeću
    public long activationTime;   // timestamp kada je aktivirano

    // Prazan konstruktor potreban Firestore-u
    public UserEquipment() {}

    public UserEquipment(String equipmentId, boolean active, boolean consumed, int quantity, int remainingBattles, long activationTime) {
        this.equipmentId = equipmentId;
        this.active = active;
        this.consumed = consumed;
        this.quantity = quantity;
        this.remainingBattles = remainingBattles;
        this.activationTime = activationTime;
    }
}
