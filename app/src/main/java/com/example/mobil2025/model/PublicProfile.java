package com.example.mobil2025.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PublicProfile {
    public String username;
    public String avatarKey;
    public long level;
    public String title;
    public long xp;
    public long badgesCount;
    public List<String> badgesPreview;
    public Map<String, Object> equipmentCurrent;
    public String qrData;

    public PublicProfile() {}

    public PublicProfile(String username, String avatarKey) {
        this.username = username;
        this.avatarKey = avatarKey;
        this.level = 1;
        this.title = "Početnik";
        this.xp = 0;
        this.badgesCount = 0;
        this.equipmentCurrent = new HashMap<>();
    }
}
