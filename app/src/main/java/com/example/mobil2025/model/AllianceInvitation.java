package com.example.mobil2025.model;

public class AllianceInvitation {
    private String id; // Jedinstveni ID poziva
    private String allianceId; // Savez u koji se poziva
    private String senderId; // Ko je poslao poziv
    private String receiverId; // Kome je poslat poziv
    private InvitationStatus status; // PENDING, ACCEPTED, REJECTED

    // Default constructor za Firestore/serialization
    public AllianceInvitation() {}

    public AllianceInvitation(String id, String allianceId, String senderId, String receiverId) {
        this.id = id;
        this.allianceId = allianceId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.status = InvitationStatus.PENDING;
    }


    // Getteri i setteri
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAllianceId() { return allianceId; }
    public void setAllianceId(String allianceId) { this.allianceId = allianceId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public InvitationStatus getStatus() { return status; }
    public void setStatus(InvitationStatus status) { this.status = status; }
}
