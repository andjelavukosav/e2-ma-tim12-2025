// com.example.mobil2025.data.repo.OccurrenceRepository
package com.example.mobil2025.data.repo;

import com.example.mobil2025.model.Occurrence;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.List;

public class OccurrenceRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void addOccurrence(Occurrence oc,
                              OnSuccessListener<DocumentReference> ok,
                              OnFailureListener err) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { err.onFailure(new IllegalStateException("Not signed in")); return; }
        oc.ownerUid = uid;
        DocumentReference ref = db.collection("occurrences").document();
        oc.id = ref.getId();
        ref.set(oc).addOnSuccessListener(aVoid -> ok.onSuccess(ref)).addOnFailureListener(err);
    }

    /** Učitaj pojave u opsegu [from,to] za korisnika (za kalendar). */
    public void loadOccurrencesInRange(long from, long to,
                                       OnSuccessListener<List<Occurrence>> ok,
                                       OnFailureListener err) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { err.onFailure(new IllegalStateException("Not signed in")); return; }
        db.collection("occurrences")
                .whereEqualTo("ownerUid", uid)
                .whereGreaterThanOrEqualTo("startAt", from)
                .whereLessThanOrEqualTo("startAt", to)
                .get()
                .addOnSuccessListener(q -> ok.onSuccess(q.toObjects(Occurrence.class)))
                .addOnFailureListener(err);
    }
}
