package com.example.mobil2025.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.mobil2025.model.Boss;

public class BossManager {
    public static final String PREFS_NAME = "BossPrefs";
    public static final String KEY_CURRENT_BOSS_LEVEL = "current_boss_level";
    public static final String KEY_CURRENT_BOSS_HP = "current_boss_hp";

    public SharedPreferences prefs;
    public Boss currentBoss;

    public BossManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadCurrentBoss();
    }

    /**
     * Učitava trenutnog boss-a iz SharedPreferences
     */
    private void loadCurrentBoss() {
        int bossLevel = prefs.getInt(KEY_CURRENT_BOSS_LEVEL, 1);
        currentBoss = new Boss(bossLevel);

        // Učitaj sačuvane HP ako postoje
        int savedHp = prefs.getInt(KEY_CURRENT_BOSS_HP, -1);
        if (savedHp != -1) {
            currentBoss.setCurrentHp(savedHp);
        }
    }

    /**
     * Čuva trenutno stanje boss-a
     */
    private void saveBossState() {
        prefs.edit()
                .putInt(KEY_CURRENT_BOSS_LEVEL, currentBoss.getLevel())
                .putInt(KEY_CURRENT_BOSS_HP, currentBoss.getCurrentHp())
                .apply();
    }

    /**
     * Napad na trenutnog boss-a
     * @param playerPP - snaga igrača
     * @return BossFightResult sa informacijama o ishodu borbe
     */
    public BossFightResult attackBoss(int playerPP) {
        boolean wasDefeated = currentBoss.takeDamage(playerPP);

        BossFightResult result = new BossFightResult();
        result.bossDefeated = wasDefeated;
        result.damageDealt = playerPP;
        result.remainingHp = currentBoss.getCurrentHp();
        result.bossLevel = currentBoss.getLevel();

        if (wasDefeated) {
            // Boss je poražen, pripremi sledećeg
            result.nextBossLevel = currentBoss.getLevel() + 1;
            advanceToNextBoss();
        } else {
            saveBossState();
        }

        return result;
    }

    /**
     * Prelazi na sledećeg boss-a
     */
    private void advanceToNextBoss() {
        int nextLevel = currentBoss.getLevel() + 1;
        currentBoss = new Boss(nextLevel);
        saveBossState();
    }

    /**
     * Resetuje boss sistem (za testiranje ili reset igre)
     */
    public void resetBosses() {
        currentBoss = new Boss(1);
        saveBossState();
    }

    public Boss getCurrentBoss() {
        return currentBoss;
    }

    /**
     * Vraća broj napada potreban da se porazi trenutni boss
     */
    public int getAttacksNeededToDefeat(int playerPP) {
        if (playerPP <= 0) return -1;
        return (int) Math.ceil((double) currentBoss.getCurrentHp() / playerPP);
    }

    /**
     * Klasa koja sadrži rezultat borbe sa boss-om
     */
    public static class BossFightResult {
        public boolean bossDefeated;
        public int damageDealt;
        public int remainingHp;
        public int bossLevel;
        public int nextBossLevel;

        @Override
        public String toString() {
            return "BossFightResult{" +
                    "bossDefeated=" + bossDefeated +
                    ", damageDealt=" + damageDealt +
                    ", remainingHp=" + remainingHp +
                    ", bossLevel=" + bossLevel +
                    ", nextBossLevel=" + nextBossLevel +
                    '}';
        }
    }
}