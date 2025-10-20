package com.example.mobil2025.model;

import  com.google.firebase.Timestamp;
public class UserPrivate {
    public String email;
    public long coins; // novcici
    public double pp; // snaga
    public Timestamp createdAt;
    public Timestamp updatedAt;

    public UserPrivate() {}

    public UserPrivate(String email) {
        this.email = email;
        this.coins = 0;
        this.pp = 40.0;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }
}
