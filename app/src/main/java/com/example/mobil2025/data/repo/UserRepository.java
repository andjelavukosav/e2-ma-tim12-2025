package com.example.mobil2025.data.repo;

import android.util.Log;

import com.example.mobil2025.model.UserProfile;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public void createUserProfileWithUniqueUsername(
            String uid,
            String email,
            String username,
            String avatarKey,
            OnSuccessListener<Void> ok,
            OnFailureListener err
    ) {
        String unameKey = username.toLowerCase(Locale.ROOT);
        DocumentReference unameRef = db.collection("usernames").document(unameKey);
        DocumentReference userRef = db.collection("users").document(uid);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot unameSnap = transaction.get(unameRef);
            if (unameSnap.exists()) {
                throw new FirebaseFirestoreException(
                        "Korisničko ime je zauzeto",
                        FirebaseFirestoreException.Code.ALREADY_EXISTS
                );
            }

            long now = System.currentTimeMillis();
            long activationDeadline = now + 2L * 60L * 1000L; // ✅ 2 minuta

            Map<String, Object> unameDoc = new HashMap<>();
            unameDoc.put("uid", uid);
            transaction.set(unameRef, unameDoc);

            UserProfile profile = new UserProfile(uid, email, username, avatarKey, now);
            profile.enabled = false;
            transaction.set(userRef, profile);

            Map<String, Object> extra = new HashMap<>();
            extra.put("activationDeadline", activationDeadline);
            transaction.set(userRef, extra, SetOptions.merge());

            return null;
        }).addOnSuccessListener(ok).addOnFailureListener(err);
    }

    public void addXP(String uid, long xpToAdd, OnLevelUpListener listener) {
        DocumentReference ref = db.collection("users").document(uid);

        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(ref);
            if (!snap.exists()) {
                throw new FirebaseFirestoreException("Korisnik ne postoji",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            UserProfile user = snap.toObject(UserProfile.class);
            if (user == null) throw new FirebaseFirestoreException("Greška pri čitanju profila",
                    FirebaseFirestoreException.Code.ABORTED);

            int previousLevel = user.getLevel();
            int previousXP = user.getXp();
            int previousPP = user.getPowerPoints();

            // 🔹 Ispiši trenutne vrednosti pre update-a
            Log.d("AddXP", "Pre update-a: XP=" + previousXP + ", Level=" + previousLevel + ", PP=" + previousPP);

            // 🔹 Dodaj XP i automatski update nivo i PP
            user.addXP((int) xpToAdd);

            int newLevel = user.getLevel();
            int newXP = user.getXp();
            int newPP = user.getPowerPoints();

            // 🔹 Ispiši vrednosti posle dodavanja XP-a, pre update-a u bazi
            Log.d("AddXP", "Posle dodavanja XP: XP=" + newXP + ", Level=" + newLevel + ", PP=" + newPP);

            Map<String, Object> updates = new HashMap<>();
            updates.put("xp", newXP);
            updates.put("level", newLevel);
            updates.put("title", user.getTitle());
            updates.put("powerPoints", newPP);

            boolean leveledUp = false;

            if (newLevel > previousLevel) {
                user.lastLevelUpAt = System.currentTimeMillis();
                updates.put("lastLevelUpAt", user.lastLevelUpAt);
                leveledUp = true;
                Log.d("AddXP", "Korisnik je prešao nivo! Novi nivo: " + newLevel);
            }

            transaction.update(ref, updates);

            return leveledUp;
        }).addOnSuccessListener(leveledUp -> {
            listener.onSuccess((Boolean) leveledUp);
        }).addOnFailureListener(listener::onFailure);
    }

    public void getCurrentUserProfile(OnSuccessListener<UserProfile> success, OnFailureListener failure) {
        String uid = auth.getCurrentUser().getUid();
        if(uid == null) {
            failure.onFailure(new Exception("Korisnik nije prijavljen"));
            return;
        }

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserProfile profile = documentSnapshot.toObject(UserProfile.class);
                        success.onSuccess(profile);
                    } else {
                        failure.onFailure(new Exception("Profil ne postoji"));
                    }
                })
                .addOnFailureListener(failure);
    }

    public void getUsersByIds(List<String> uids, OnSuccessListener<List<UserProfile>> success, OnFailureListener failure) {
        if (uids == null || uids.isEmpty()) {
            success.onSuccess(new ArrayList<>());
            return;
        }

        CollectionReference usersRef = db.collection("users");
        List<UserProfile> result = new ArrayList<>();
        final int total = uids.size();
        final int[] count = {0};

        for (String uid : uids) {
            usersRef.document(uid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            UserProfile profile = doc.toObject(UserProfile.class);
                            if(profile != null)
                                result.add(profile);
                        }
                        count[0]++;
                        if(count[0] == total) {
                            success.onSuccess(result);
                        }
                    })
                    .addOnFailureListener(e -> {
                        count[0]++;
                        if(count[0] == total) {
                            success.onSuccess(result); // vraća sve što je dohvaćeno
                        }
                    });
        }
    }
    // Definiši interfejs za callback
    public interface OnCompleteListener {
        void onSuccess();

        void onFailure(Exception e);
    }

    public void updateSuccessRate(String uid, double newRate,
                                  OnSuccessListener<Void> ok, OnFailureListener err) {
        db.collection("users").document(uid)
                .update("successRate", newRate)
                .addOnSuccessListener(ok)
                .addOnFailureListener(err);
    }

    public void getUserLevel(String userId, OnSuccessListener<Integer> success, OnFailureListener failure) {
        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long levelLong = documentSnapshot.getLong("level");
                        int level = (levelLong != null) ? levelLong.intValue() : 1; // default 1
                        success.onSuccess(level);
                    } else {
                        success.onSuccess(1); // default level
                    }
                })
                .addOnFailureListener(failure);
    }

}
