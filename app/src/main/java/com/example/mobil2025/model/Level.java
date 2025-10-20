package com.example.mobil2025.model;

public class Level {
    private int level;
    private int requiredXP;
    private int powerPoints;
    private String title;

    public Level(int level, int requiredXP, int powerPoints, String title) {
        this.level = level;
        this.requiredXP = requiredXP;
        this.powerPoints = powerPoints;
        this.title = title;
    }

    public int getLevel() { return level; }
    public int getRequiredXP() { return requiredXP; }
    public int getPowerPoints() { return powerPoints; }
    public String getTitle() { return title; }
}
