package com.example.mobil2025.model;

public class Weapon extends Equipment {

    private WeaponType weaponType;  // Mač, Luk i strela
    private double bonusValue;      // bonus efekat (npr. 0.05 = +5%)
    private String effectType;      // "PP" ili "COINS" - šta povećava

    public Weapon(String id, String name, WeaponType weaponType, double bonusValue, String effectType) {
        super(id, name, 0, EquipmentType.WEAPON); // cena 0, jer se ne kupuje
        this.weaponType = weaponType;
        this.bonusValue = bonusValue;
        this.effectType = effectType;
    }

    // --- Getteri ---
    public WeaponType getWeaponType() { return weaponType; }
    public double getBonusValue() { return bonusValue; }
    public String getEffectType() { return effectType; }
}
