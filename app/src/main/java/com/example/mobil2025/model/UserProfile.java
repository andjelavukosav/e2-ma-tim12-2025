package com.example.mobil2025.model;

import android.util.Log;

import com.example.mobil2025.data.repo.LevelRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserProfile implements Serializable {
    public String uid;
    public String email;
    public String username;   // IMMUTABLE

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public String avatarKey;  // IMMUTABLE (npr. "avatar_1")
    public long createdAt;
    public boolean enabled;   // ✅ novo polje
    public double successRate; // uspešnost korisnika (0.0 - 100.0)

    public long lastLevelUpAt; // vreme kada je korisnik prešao na trenutni nivo

    public int level;           // trenutni nivo korisnika
    public String title;        // titula (npr. "Početnik", "Iskusni igrač", "Majstor")
    public int powerPoints;     // snaga (PP)
    public int xp; // XP - experience points
    private int coins;           // broj sakupljenih novčića
    public List<String> badges; // lista osvojenih bedževa (npr. ["Explorer", "Winner"])
    public List<String> equipment; // lista opreme koju korisnik poseduje (npr. ["Helmet", "Sword"])
    public String qrCodeUrl;    // link na QR kod korisnika

    public List<String> ownedEquipment;
    public List<String> friends;

    private List<ClothingItem> clothingInventory = new ArrayList<>();
    private List<PotionItem> potionInventory = new ArrayList<>();

    private String allianceId;


    // Prazan konstruktor potreban Firestore-u
    public UserProfile() {}

    // Glavni konstruktor
    public UserProfile(String uid, String email, String username, String avatarKey, long createdAt) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.avatarKey = avatarKey;
        this.createdAt = createdAt;
        this.enabled = true; // podrazumevano aktivan

        // 🔹 Podrazumevane vrednosti
        this.level = 1;
        this.title = "Početnik";
        this.powerPoints = 0;
        this.xp = 0; // inicijalno
        this.coins = 0;
        this.badges = List.of();
        this.equipment = List.of();
        this.qrCodeUrl = "";
        this.friends = List.of();
        this.lastLevelUpAt = createdAt; // prvi nivo = vreme kreiranja profila
        this.successRate = 0.0;

    }

    // ✅ Ako želiš dodatni konstruktor sa kontrolom enable polja:
    public UserProfile(String uid, String email, String username, String avatarKey, long createdAt, boolean enabled) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.avatarKey = avatarKey;
        this.createdAt = createdAt;
        this.enabled = enabled;
        this.xp = 0; // inicijalno
    }

    public UserProfile(String uid, String email, String username, String avatarKey, long createdAt, boolean enabled,
                       int level, String title, int powerPoints, int experiencePoints, int coins,
                       List<String> badges, List<String> equipment, String qrCodeUrl) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.avatarKey = avatarKey;
        this.createdAt = createdAt;
        this.enabled = enabled;
        this.level = level;
        this.title = title;
        this.powerPoints = powerPoints;
        this.xp = experiencePoints;
        this.coins = coins;
        this.badges = badges;
        this.equipment = equipment;
        this.qrCodeUrl = qrCodeUrl;
    }



    public void addXP(int xpGained) {
        this.xp += xpGained;

        Log.d("UserProfile", "Dodato XP: " + xpGained + ", Ukupno XP pre update-a: " + this.xp);

        updateLevel();  // automatski ažurira nivo i PP
    }

    private void updateLevel() {
        Level currentLevel = LevelRepository.getLevelForXP(this.xp);

        // 🔹 Ispis trenutnog level-a pre update-a polja
        Log.d("UserProfile", "CurrentLevel pre update-a: level=" + currentLevel.getLevel()
                + ", title=" + currentLevel.getTitle()
                + ", PP=" + currentLevel.getPowerPoints());

        this.level = currentLevel.getLevel();
        this.title = currentLevel.getTitle();
        this.powerPoints = currentLevel.getPowerPoints();

        // 🔹 Ispis nakon update-a polja
        Log.d("UserProfile", "UserProfile posle update-a: level=" + this.level
                + ", title=" + this.title
                + ", PP=" + this.powerPoints);
    }

    public int getXp() { return xp; }
    public int getLevel() { return level; }
    public String getTitle() { return title; }
    public int getPowerPoints() { return powerPoints; }

    public int addXPAndGetRemainingToNextLevel(int xpGained) {
        // 🔹 Zapamti XP pre dodavanja da bismo mogli da proverimo prelazak nivoa
        int previousXP = this.xp;
        this.xp += xpGained;

        Level previousLevel;
        Level currentLevel;
        Level nextLevel;

        if (previousXP < LevelRepository.getLevels().get(0).getRequiredXP()) {
            // Korisnik pre dodavanja XP-a nije imao ni prvi nivo
            previousLevel = new Level(0, 0, 0, "Nema nivoa");
        } else {
            previousLevel = LevelRepository.getLevelForXP(previousXP);
        }

        if (this.xp < LevelRepository.getLevels().get(0).getRequiredXP()) {
            currentLevel = new Level(0, 0, 0, "Nema nivoa");
            nextLevel = LevelRepository.getLevels().get(0);
        } else {
            currentLevel = LevelRepository.getLevelForXP(this.xp);
            nextLevel = LevelRepository.getNextLevel(currentLevel);
        }

        // 🔹 Ako je korisnik prešao na novi nivo
        if (currentLevel.getLevel() > previousLevel.getLevel()) {
            this.lastLevelUpAt = System.currentTimeMillis(); // zabeleži datum prelaska

        }

        // 🔹 Ažuriraj osnovne podatke o nivou
        this.level = currentLevel.getLevel();
        this.title = currentLevel.getTitle();
        this.powerPoints = currentLevel.getPowerPoints();

        // 🔹 Vrati XP potreban do sledećeg nivoa
        if (nextLevel != null) {
            return nextLevel.getRequiredXP() - this.xp;
        } else {
            return 0; // ako je korisnik na maksimalnom nivou
        }
    }

    public List<ClothingItem> getClothingInventory() { return clothingInventory; }
    public List<PotionItem> getPotionInventory() { return potionInventory; }

    // Dodavanje stavki u inventar
    public void addClothing(Clothing clothing) {
        clothingInventory.add(new ClothingItem(clothing.getUid(), clothing.getDurability()));
    }


    public void addPotion(Potion potion) {
        potionInventory.add(new PotionItem(potion.getUid()));
    }

    public int getCoins(){ return this.coins; }


    public void setCoins(int coins) {
        this.coins = coins;
    }

    public void removeBrokenClothing() {
        clothingInventory.removeIf(ClothingItem::isBroken);
    }


}

