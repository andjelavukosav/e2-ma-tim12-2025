package com.example.mobil2025.data.repo;

import androidx.annotation.NonNull;

import com.example.mobil2025.model.Occurrence;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Repo za zavrsene/zakazane pojave (occurrences).
 * Pravila u Firestore-u moraju imati match /occurrences/{occId} sa read/write samo vlasniku.
 *
 * Napomena „bez indeksa“:
 *  - Na serveru koristimo samo equality filtere (ownerUid, taskId),
 *  - Raspon po startAt filtriramo na klijentu.
 */
public class OccurrenceRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Kreiranje jedne pojave — setuje ownerUid i id prije upisa. */
    public void addOccurrence(@NonNull Occurrence oc,
                              @NonNull OnSuccessListener<DocumentReference> ok,
                              @NonNull OnFailureListener err) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { err.onFailure(new IllegalStateException("Not signed in")); return; }
        oc.ownerUid = uid;

        DocumentReference ref = db.collection("occurrences").document();
        oc.id = ref.getId();

        ref.set(oc)
                .addOnSuccessListener(aVoid -> ok.onSuccess(ref))
                .addOnFailureListener(err);
    }

    /**
     * Ucitaj pojave u opsegu [from, to] za trenutnog korisnika (npr. kalendar).
     * Server: whereEqualTo(ownerUid, uid), ostalo se filtrira na klijentu — BEZ composite indeksa.
     */
    public void loadOccurrencesInRange(long from, long to,
                                       @NonNull OnSuccessListener<List<Occurrence>> ok,
                                       @NonNull OnFailureListener err) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { err.onFailure(new IllegalStateException("Not signed in")); return; }

        db.collection("occurrences")
                .whereEqualTo("ownerUid", uid)
                .get()
                .addOnSuccessListener((QuerySnapshot q) -> {
                    List<Occurrence> all = q.toObjects(Occurrence.class);
                    List<Occurrence> filtered = new ArrayList<>();
                    for (Occurrence o : all) {
                        if ("done".equals(o.status) && o.startAt >= from && o.startAt <= to) {
                            filtered.add(o);
                        }
                    }
                    ok.onSuccess(filtered);
                })
                .addOnFailureListener(err);
    }

    /**
     * Ucitaj pojave za odredjeni task, a zatim filtriraj vremenski prozor na klijentu.
     * Korisno kada hoces „bez indeksa“ za kombinaciju taskId + raspon startAt.
     */
    public void loadOccurrencesForTaskInRange(@NonNull String taskId, long from, long to,
                                              @NonNull OnSuccessListener<List<Occurrence>> ok,
                                              @NonNull OnFailureListener err) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { err.onFailure(new IllegalStateException("Not signed in")); return; }

        db.collection("occurrences")
                .whereEqualTo("ownerUid", uid)     // obavezno zbog rules
                .whereEqualTo("taskId", taskId)    // equality — ne trazi composite indeks
                .get()
                .addOnSuccessListener(q -> {
                    List<Occurrence> all = q.toObjects(Occurrence.class);
                    List<Occurrence> filtered = new ArrayList<>();
                    for (Occurrence o : all) {
                        if (o.startAt > from && o.startAt <= to) {
                            filtered.add(o);
                        }
                    }
                    ok.onSuccess(filtered);
                })
                .addOnFailureListener(err);
    }
}
