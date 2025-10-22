package com.example.mobil2025.data.repo;

import com.example.mobil2025.model.Alliance;
import com.example.mobil2025.model.AllianceInvitation;
import com.example.mobil2025.model.InvitationStatus;
import com.example.mobil2025.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AllianceRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public AllianceRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public void createAlliance(String allianceName, List<String> friendIds, AllianceCallback callback) {
        String currentUserId = auth.getCurrentUser().getUid();

        // Proveri da li korisnik već ima savez
        db.collection("alliances")
                .whereArrayContains("memberIds", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        callback.onFailure("Već si član saveza i ne možeš kreirati novi.");
                        return;
                    }

                    String allianceId = UUID.randomUUID().toString();
                    Alliance alliance = new Alliance(allianceId, allianceName, currentUserId);

                    // Kreiraj batch za save i pozive
                    WriteBatch batch = db.batch();
                    batch.set(db.collection("alliances").document(allianceId), alliance);

                    // Kreiranje poziva prijateljima
                    for (String friendId : friendIds) {
                        String invitationId = UUID.randomUUID().toString();
                        AllianceInvitation invitation = new AllianceInvitation(invitationId, allianceId, currentUserId, friendId);

                        System.out.println("Invitation JSON: " +
                                "id=" + invitation.getId() +
                                ", allianceId=" + invitation.getAllianceId() +
                                ", senderId=" + invitation.getSenderId() +
                                ", receiverId=" + invitation.getReceiverId() +
                                ", status=" + invitation.getStatus()
                        );
                        batch.set(db.collection("alliance_invitations").document(invitationId), invitation);
                        String notificationId = UUID.randomUUID().toString();

                        Map<String, Object> notificationData = new HashMap<>();
                        notificationData.put("id", notificationId);
                        notificationData.put("receiverId", friendId); // kome je notifikacija namenjena
                        notificationData.put("relatedInvitationId", invitationId); // veza sa pozivom
                        notificationData.put("type", "ALLIANCE_INVITE");
                        notificationData.put("message", "Pozvani ste u savez '" + alliance.getName() + "' od strane " + currentUserId);
                        notificationData.put("read", false);
                        notificationData.put("timestamp", FieldValue.serverTimestamp());

// dodaj u batch
                        batch.set(db.collection("notifications").document(notificationId), notificationData);
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess(alliance))
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));    }


    public void sendInvitesToFriends(Alliance alliance, List<String> friendIds, AllianceCallback callback) {
        String currentUserId = auth.getCurrentUser().getUid();
        WriteBatch batch = db.batch();

        for (String friendId : friendIds) {
            String invitationId = UUID.randomUUID().toString();
            AllianceInvitation invitation = new AllianceInvitation(invitationId, alliance.getId(), currentUserId, friendId);
            batch.set(db.collection("alliance_invitations").document(invitationId), invitation);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess(alliance))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }


    public void acceptInvitation(AllianceInvitation invitation, AllianceCallback callback) {
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection("alliances").document(invitation.getAllianceId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Alliance alliance = documentSnapshot.toObject(Alliance.class);
                    if (alliance == null) {
                        callback.onFailure("Savez ne postoji.");
                        return;
                    }

                    // Dodaj korisnika u savez
                    alliance.addMember(currentUserId);

                    WriteBatch batch = db.batch();
                    batch.set(db.collection("alliances").document(alliance.getId()), alliance);

                    // Ažuriraj status poziva
                    invitation.setStatus(InvitationStatus.ACCEPTED);
                    batch.set(db.collection("alliance_invitations").document(invitation.getId()), invitation);

                    //  Napravi notifikaciju za kreatora saveza
                    String notificationId = UUID.randomUUID().toString();
                    Map<String, Object> notificationData = new HashMap<>();
                    notificationData.put("id", notificationId);
                    notificationData.put("receiverId", invitation.getSenderId()); // kreator saveza
                    notificationData.put("relatedInvitationId", invitation.getId());
                    notificationData.put("type", "INVITE_ACCEPTED");
                    notificationData.put("message", "Korisnik " + currentUserId +
                            " je prihvatio tvoj poziv u savez '" + alliance.getName() + "'");
                    notificationData.put("read", false);
                    notificationData.put("timestamp", FieldValue.serverTimestamp());

                    batch.set(db.collection("notifications").document(notificationId), notificationData);

                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess(alliance))
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }


    public void rejectInvitation(AllianceInvitation invitation, AllianceCallback callback) {
        invitation.setStatus(InvitationStatus.REJECTED);
        db.collection("alliance_invitations").document(invitation.getId())
                .set(invitation)
                .addOnSuccessListener(aVoid -> callback.onRejected(invitation))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Callback interfejs
    public interface AllianceCallback {
        void onSuccess(Alliance alliance);
        void onRejected(AllianceInvitation invitation);
        void onFailure(String error);
    }



}
