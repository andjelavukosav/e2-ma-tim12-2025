package com.example.mobil2025.model;

public class Potion extends Equipment {
    private double powerBoost;      // npr. 0.2 za 20%
    private boolean permanent;      // true = trajni

    public Potion(String id, String name, int price, double powerBoost, boolean permanent) {
        super(id, name, price, EquipmentType.POTION);
        this.powerBoost = powerBoost;
        this.permanent = permanent;
    }

    // --- Getteri i setteri ---
    public double getPowerBoost() { return powerBoost; }
    public boolean isPermanent() { return permanent; }


}
