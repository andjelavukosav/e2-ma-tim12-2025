package com.example.mobil2025.model;

import java.util.List;

public class UserProfile {
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
    public int coins;           // broj sakupljenih novčića
    public List<String> badges; // lista osvojenih bedževa (npr. ["Explorer", "Winner"])
    public List<String> equipment; // lista opreme koju korisnik poseduje (npr. ["Helmet", "Sword"])
    public String qrCodeUrl;    // link na QR kod korisnika

    public List<String> friends;
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

}

