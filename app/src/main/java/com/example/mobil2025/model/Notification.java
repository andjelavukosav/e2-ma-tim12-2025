package com.example.mobil2025.model;

import com.google.firebase.Timestamp;

public class Notification {
    private String id;
    private String receiverId; // Korisnik kome je notifikacija namenjena
    private String relatedInvitationId; // ID AllianceInvitation na koji se odnosi
    private String type; // npr. "ALLIANCE_INVITE", "INVITE_ACCEPTED"
    private String message; // Tekstualna poruka za korisnika
    private boolean read; // Da li je notifikacija pročitana
    private Timestamp timestamp; // Kada je notifikacija kreirana

    // Default constructor za Firestore
    public Notification() {}

    public Notification(String id, String receiverId, String relatedInvitationId, String type, String message) {
        this.id = id;
        this.receiverId = receiverId;
        this.relatedInvitationId = relatedInvitationId;
        this.type = type;
        this.message = message;
        this.read = false;
        this.timestamp = Timestamp.now();
    }

    // Getteri i setteri
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getRelatedInvitationId() { return relatedInvitationId; }
    public void setRelatedInvitationId(String relatedInvitationId) { this.relatedInvitationId = relatedInvitationId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
