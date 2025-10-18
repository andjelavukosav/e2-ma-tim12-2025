package com.example.mobil2025.model;

public class UserProfile {
    public String uid;
    public String email;
    public String username;   // IMMUTABLE
    public String avatarKey;  // IMMUTABLE (npr. "avatar_1")
    public long createdAt;
    public boolean enabled;   // ✅ novo polje

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
    }

    // ✅ Ako želiš dodatni konstruktor sa kontrolom enable polja:
    public UserProfile(String uid, String email, String username, String avatarKey, long createdAt, boolean enabled) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.avatarKey = avatarKey;
        this.createdAt = createdAt;
        this.enabled = enabled;
    }
}
