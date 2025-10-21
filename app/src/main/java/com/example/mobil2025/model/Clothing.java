package com.example.mobil2025.model;

public class Clothing extends Equipment {
    private ClothingType clothingType;
    private double bonusValue; // npr. 0.1 = +10%
    private int durability;    // broj borbi (2)

    public Clothing(String id, String name, int price, double bonusValue, ClothingType clothingType) {
        super(id, name, price, EquipmentType.CLOTHING);
        this.clothingType = clothingType;
        this.bonusValue = bonusValue;
        this.durability = 2;
    }

    public ClothingType getClothingType() { return clothingType; }
    public double getBonusValue() { return bonusValue; }
    public int getDurability() { return durability; }


    // Ako korisnik aktivira istu odeću ponovo → sabira se bonus
    public void addBonus(double additionalBonus) {
        this.bonusValue += additionalBonus;
    }


}
