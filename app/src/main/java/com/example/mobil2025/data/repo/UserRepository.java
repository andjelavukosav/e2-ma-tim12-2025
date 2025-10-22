package com.example.mobil2025.data.repo;

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

    public void addXP(String uid, long xpToAdd, OnCompleteListener listener) {
        DocumentReference ref = db.collection("users").document(uid);
        ref.update("xp", FieldValue.increment(xpToAdd))
                .addOnSuccessListener(v -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e));
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
}
