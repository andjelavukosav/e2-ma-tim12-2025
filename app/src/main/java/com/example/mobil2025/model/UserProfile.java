package com.example.mobil2025.model;

import com.example.mobil2025.data.repo.LevelRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserProfile implements Serializable {
    public String uid;
    public String email;
    public String username;   // IMMUTABLE
    public String avatarKey;  // IMMUTABLE (npr. "avatar_1")
    public long createdAt;
    public boolean enabled;   // ✅ novo polje


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


    public UserProfile(String uid, String email, String username, String avatarKey, long createdAt, List<String> friends) {
        this(uid, email, username, avatarKey, createdAt);
        this.friends = friends;
    }

    public void addXP(int xpGained) {
        this.xp += xpGained;
        updateLevel();  // automatski ažurira nivo i PP
    }

    private void updateLevel() {
        Level currentLevel = LevelRepository.getLevelForXP(this.xp);
        this.level = currentLevel.getLevel();
        this.title = currentLevel.getTitle();
        this.powerPoints = currentLevel.getPowerPoints();
    }

    public int getXp() { return xp; }
    public int getLevel() { return level; }
    public String getTitle() { return title; }
    public int getPowerPoints() { return powerPoints; }

    public int addXPAndGetRemainingToNextLevel(int xpGained) {
        this.xp += xpGained;

        Level currentLevel;
        Level nextLevel;

        if (this.xp < LevelRepository.getLevels().get(0).getRequiredXP()) {
            // korisnik još nije dostigao prvi nivo
            currentLevel = new Level(0, 0, 0, "Nema nivoa");
            nextLevel = LevelRepository.getLevels().get(0); // prvi nivo kao "sljedeći"
        } else {
            currentLevel = LevelRepository.getLevelForXP(this.xp);
            nextLevel = LevelRepository.getNextLevel(currentLevel);
        }

        this.level = currentLevel.getLevel();
        this.title = currentLevel.getTitle();
        this.powerPoints = currentLevel.getPowerPoints();

        if(nextLevel != null){
            return nextLevel.getRequiredXP() - this.xp;
        } else {
            return 0; // ako je korisnik na max nivou
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

