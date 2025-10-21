package com.example.mobil2025.model;

import java.io.Serializable;
import java.util.UUID;

public class PotionItem implements Serializable {
    private String uid;       // jedinstveni ID stavke u inventaru
    private String potionId;   // referenca na master stavku    private boolean activated;
    private boolean activated;
    private boolean consumed;
    private long activationTime;

    public PotionItem() {}
    public PotionItem(String potionId) {
        this.uid = UUID.randomUUID().toString(); // jedinstveni ID
        this.potionId = potionId;
        this.activated = false;
        this.consumed = false;
        this.activationTime = 0;
    }

    // --- Getteri i setteri ---
    public String getUid() { return uid; }
    public String getPotionId() { return potionId; }
    public boolean isActivated() { return activated; }
    public void setActivated(boolean activated) {
        this.activated = activated;
        if (activated) this.activationTime = System.currentTimeMillis();
    }

    public boolean isConsumed() { return consumed; }
    public void setConsumed(boolean consumed) { this.consumed = consumed; }

    public long getActivationTime() { return activationTime; }
    public void reset() {
        this.activated = false;
        this.consumed = false;
        this.activationTime = 0;
    }

    public boolean isReadyToUse() {
        return !activated && !consumed;
    }


}
