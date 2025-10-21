package com.example.mobil2025.model;

public class Boss {
    private int level;
    private int maxHp;
    private int currentHp;

    public Boss(int level) {
        this.level = level;
        this.maxHp = calculateBossHp(level);
        this.currentHp = maxHp;
    }

    /**
     * Kalkulacija HP za boss-a na osnovu nivoa
     * Nivo 1: 200 HP
     * Nivo 2+: HP prethodnog * 2 + HP prethodnog / 2
     */
    private int calculateBossHp(int level) {
        if (level == 1) {
            return 200;
        }

        int previousHp = calculateBossHp(level - 1);
        return previousHp * 2 + previousHp / 2;
    }

    /**
     * Napad na boss-a
     * @param damage - PP (snaga igrača)
     * @return true ako je boss poražen, false ako nije
     */
    public boolean takeDamage(int damage) {
        currentHp -= damage;
        if (currentHp < 0) {
            currentHp = 0;
        }
        return isDefeated();
    }

    public boolean isDefeated() {
        return currentHp <= 0;
    }

    /**
     * Vraća procenat preostale HP
     */
    public float getHpPercentage() {
        return ((float) currentHp / maxHp) * 100;
    }

    // Getteri
    public int getLevel() {
        return level;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(int hp) {
        this.currentHp = Math.max(0, Math.min(hp, maxHp));
    }
}