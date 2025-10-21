package com.example.mobil2025.model;

public class StageStats {
    private int completedTasks;
    private int totalTasks; // Isključuje pauzirane, otkazane i iznad kvote
    private double successRate;

    public StageStats(int completedTasks, int totalTasks) {
        this.completedTasks = completedTasks;
        this.totalTasks = totalTasks;
        this.successRate = calculateSuccessRate();
    }

    /**
     * Kalkuliše procenat uspešnosti
     */
    private double calculateSuccessRate() {
        if (totalTasks == 0) {
            return 0.0;
        }
        return ((double) completedTasks / totalTasks) * 100.0;
    }

    /**
     * Vraća uspešnost kao procenat (0-100)
     */
    public double getSuccessRate() {
        return successRate;
    }

    /**
     * Vraća uspešnost kao decimalu (0.0-1.0) za proveru šanse
     */
    public double getSuccessChance() {
        return successRate / 100.0;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    /**
     * Formatiran string za prikaz
     */
    public String getSuccessRateFormatted() {
        return String.format("%.0f%%", successRate);
    }

    /**
     * Provera da li je napad uspešan na osnovu šanse
     */
    public boolean isAttackSuccessful() {
        // Generiši random broj između 0.0 i 1.0
        double random = Math.random();
        return random <= getSuccessChance();
    }
}
