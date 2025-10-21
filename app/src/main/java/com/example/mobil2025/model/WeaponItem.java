package com.example.mobil2025.model;

import java.util.UUID;

public class WeaponItem {

    private String uid;          // jedinstveni ID stavke kod korisnika
    private String weaponId;     // referenca na master Weapon
    private int upgradeLevel;    // broj unapređenja
    private double probability;  // verovatnoća dobijanja pri dropu
    private boolean activated;   // da li korisnik trenutno koristi oružje u borbi

    public WeaponItem() {}

    public WeaponItem(String weaponId) {
        this.uid = UUID.randomUUID().toString();
        this.weaponId = weaponId;
        this.upgradeLevel = 0;
        this.probability = 0.0;
        this.activated = false;
    }

    // --- Getteri i setteri ---
    public String getUid() { return uid; }
    public String getWeaponId() { return weaponId; }
    public int getUpgradeLevel() { return upgradeLevel; }
    public double getProbability() { return probability; }
    public boolean isActivated() { return activated; }

    public void setActivated(boolean activated) { this.activated = activated; }

    // Povećavanje verovatnoće ako korisnik dobije isto oružje
    public void increaseProbabilityOnDuplicate() {
        this.probability += 0.02;
    }

    // Unapređenje oružja
    public void upgrade(int coinsRewardFromBoss) {
        this.upgradeLevel++;
        this.probability += 0.01;
    }
}
