package com.example.mobil2025.model;

import java.io.Serializable;
import java.util.UUID;

public class ClothingItem implements Serializable {
    private String uid;       // jedinstveni ID stavke u inventaru
    private String clothingId;; // referenca na master podatke iz prodavnice
    private int remainingBattles;
    private boolean activated;
    private long activationTime;

    public ClothingItem() {}
    public ClothingItem(String clothingId, int durability) {
        this.uid = UUID.randomUUID().toString();
        this.clothingId = clothingId;
        this.remainingBattles = durability; //inicijano neiskoristen pa je jednak duzini trajanja stavke - 2 borbe
        this.activated = false;
        this.activationTime = 0;
    }

    public String getUid() { return uid; }
    public String getClothingId() { return clothingId; }
    public int getRemainingBattles() { return remainingBattles; }

    public boolean isActivated() { return activated; }
    public void setActivated(boolean activated) {
        this.activated = activated;
        if (activated) this.activationTime = System.currentTimeMillis();
    }

    public long getActivationTime() { return activationTime; }
    public void decreaseDurability() {
        if (remainingBattles > 0) remainingBattles--;
    }

    public boolean isBroken() {
        return remainingBattles <= 0;
    }

    public void resetDurability(int durability) {
        this.remainingBattles = durability;
    }

    public boolean isReadyToUse() {
        return !activated && !isBroken();
    }

    public void reset() {
        this.activated = false;
        this.activationTime = 0;
    }
}
