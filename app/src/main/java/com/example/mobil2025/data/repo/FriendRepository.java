package com.example.mobil2025.data.repo;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;

public class FriendRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FriendRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    // Dodavanje prijatelja
    public void addFriend(String friendUid, OnFriendActionListener listener) {
        String currentUid = auth.getCurrentUser().getUid();

        if (friendUid == null || friendUid.isEmpty()) {
            listener.onFailure(new Exception("Nevažeći UID prijatelja."));
            return;
        }
        if (currentUid == null) {
            listener.onFailure(new Exception("Korisnik nije prijavljen."));
            return;
        }
        if (currentUid.equals(friendUid)) {
            listener.onFailure(new Exception("Ne možeš dodati samog sebe."));
            return;
        }

        // Dodaj prijatelja u listu trenutnog korisnika
        db.collection("users").document(currentUid)
                .update("friends", FieldValue.arrayUnion(friendUid))
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    // Provera da li je već prijatelj
    public void isFriend(String friendUid, OnCheckFriendListener listener) {
        String currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (friendUid == null || friendUid.isEmpty() || currentUid == null) {
            listener.onResult(false);
            return;
        }

        db.collection("users").document(currentUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> friends = (List<String>) doc.get("friends");
                        boolean isFriend = friends != null && friends.contains(friendUid);
                        listener.onResult(isFriend);
                    } else {
                        listener.onResult(false);
                    }
                })
                .addOnFailureListener(e -> listener.onResult(false));
    }

    // Uklanjanje prijatelja
    public void removeFriend(String friendUid, OnFriendActionListener listener) {
        String currentUid = auth.getCurrentUser().getUid();

        if (friendUid == null || friendUid.isEmpty()) {
            listener.onFailure(new Exception("Nevažeći UID prijatelja."));
            return;
        }
        if (currentUid == null) {
            listener.onFailure(new Exception("Korisnik nije prijavljen."));
            return;
        }

        db.collection("users").document(currentUid)
                .update("friends", FieldValue.arrayRemove(friendUid))
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }


    // Callback interfejsi
    public interface OnFriendActionListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnCheckFriendListener {
        void onResult(boolean isFriend);
    }
}