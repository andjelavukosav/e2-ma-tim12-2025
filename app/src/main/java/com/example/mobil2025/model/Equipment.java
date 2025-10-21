package com.example.mobil2025.model;


public abstract class Equipment {
    protected String uid;
    protected String name;
    protected int price;
    protected EquipmentType type;

    public Equipment(String id, String name, int price, EquipmentType type) {
        this.uid = id;
        this.name = name;
        this.price = price;
        this.type = type;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }


    public String getUid() { return uid; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public EquipmentType getType() { return type; }
}