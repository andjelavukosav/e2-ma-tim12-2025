package com.example.mobil2025.model;

import java.util.ArrayList;
import java.util.List;

public class Alliance {
    private String id; // Jedinstveni ID saveza
    private String name; // Naziv saveza
    private String leaderId; // ID korisnika koji je vođa
    private List<String> memberIds; // Lista članova saveza

    // Default constructor za Firestore/serialization
    public Alliance() {
        this.memberIds = new ArrayList<>();
    }

    public Alliance(String id, String name, String leaderId) {
        this.id = id;
        this.name = name;
        this.leaderId = leaderId;
        this.memberIds = new ArrayList<>();
        this.memberIds.add(leaderId); // Vođa je automatski član
    }

    // Getteri i setteri
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLeaderId() { return leaderId; }
    public void setLeaderId(String leaderId) { this.leaderId = leaderId; }

    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }

    public void addMember(String userId) {
        if (!memberIds.contains(userId)) {
            memberIds.add(userId);
        }
    }

    public void removeMember(String userId) {
        memberIds.remove(userId);
    }

    public boolean isMember(String userId) {
        return memberIds.contains(userId);
    }
}
