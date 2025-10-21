package com.example.mobil2025.model;

public class Boss {
    private String id;          // Jedinstveni ID bosa, npr. "boss_1"
    private String ownerUid;
    private boolean defeated;   // Da li je bos pobijeđen

    private int level;

    private int maxHp;
    private int currentHp;


    public Boss() {
        // Firestore zahteva prazni konstruktor
    }

    public Boss(int hp, int maxHp){
        this.currentHp = hp;
        this.maxHp = maxHp;
    }

    public Boss(String id, String ownerUid, int maxHp, int level) {
        this.id = id;
        this.ownerUid = ownerUid;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.defeated = false;
        this.level = level;
    }

    public Boss(String id, String ownerUid, int hp, int level, int maxHp, boolean defeated) {
        this.id = id;
        this.ownerUid = ownerUid;
        this.currentHp = hp;
        this.level = level;
        this.maxHp = maxHp;
        this.defeated = defeated;
    }

        // Getteri i setteri
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }

    public int getHp() { return currentHp; }
    public void setHp(int hp) { this.currentHp = hp; }

    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }

    public boolean isDefeated() { return defeated; }
    public void setDefeated(boolean defeated) { this.defeated = defeated; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
}