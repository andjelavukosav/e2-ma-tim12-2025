// com.example.mobil2025.data.repo.CategoryRepository
package com.example.mobil2025.data.repo;

import androidx.annotation.NonNull;

import com.example.mobil2025.model.Category;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CategoryRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static String colorKey(String ownerUid, String colorHex) {
        return ownerUid + "_" + colorHex.toUpperCase(Locale.ROOT);
    }

    /** Kreiraj kategoriju sa jedinstvenom bojom (po korisniku). */
    public void createCategoryUniqueColor(String name, String colorHex,
                                          OnSuccessListener<DocumentReference> ok,
                                          OnFailureListener err) {
        String ownerUid = FirebaseAuth.getInstance().getUid();
        if (ownerUid == null) { err.onFailure(new IllegalStateException("Not signed in")); return; }

        DocumentReference newCatRef = db.collection("categories").document();
        DocumentReference colorRef  = db.collection("category_colors").document(colorKey(ownerUid, colorHex));

        db.runTransaction((Transaction.Function<DocumentReference>) tr -> {
            // 1) Proveri da li boja već postoji
            DocumentSnapshot colorSnap = tr.get(colorRef);
            if (colorSnap.exists()) {
                throw new FirebaseFirestoreException("Boja je već zauzeta",
                        FirebaseFirestoreException.Code.ALREADY_EXISTS);
            }

            // 2) Upis kategorije
            Category c = new Category();
            c.id = newCatRef.getId();
            c.ownerUid = ownerUid;
            c.name = name.trim();
            c.colorHex = colorHex;
            c.createdAt = System.currentTimeMillis();
            tr.set(newCatRef, c);

            // 3) Rezerviši boju
            Map<String, Object> colDoc = new HashMap<>();
            colDoc.put("ownerUid", ownerUid);
            colDoc.put("categoryId", c.id);
            tr.set(colorRef, colDoc);

            return newCatRef;
        }).addOnSuccessListener(ok).addOnFailureListener(err);
    }

    /** Promeni boju kategorije, uz zadržavanje jedinstvenosti boje. */
    public void updateCategoryColor(String categoryId, String newColorHex,
                                    OnSuccessListener<Void> ok,
                                    OnFailureListener err) {
        String ownerUid = FirebaseAuth.getInstance().getUid();
        if (ownerUid == null) { err.onFailure(new IllegalStateException("Not signed in")); return; }

        DocumentReference catRef   = db.collection("categories").document(categoryId);

        db.runTransaction((Transaction.Function<Void>) tr -> {
            DocumentSnapshot catSnap = tr.get(catRef);
            if (!catSnap.exists()) {
                throw new FirebaseFirestoreException("Kategorija ne postoji",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            Category c = catSnap.toObject(Category.class);
            if (c == null || !ownerUid.equals(c.ownerUid)) {
                throw new FirebaseFirestoreException("Zabranjeno",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED);
            }

            String oldColor = c.colorHex;
            String newKey   = colorKey(ownerUid, newColorHex);
            String oldKey   = colorKey(ownerUid, oldColor);

            // 1) Proveri novu boju (mora biti slobodna ili je ista kao stara)
            if (!newColorHex.equalsIgnoreCase(oldColor)) {
                DocumentReference newColRef = db.collection("category_colors").document(newKey);
                if (tr.get(newColRef).exists()) {
                    throw new FirebaseFirestoreException("Boja je već zauzeta",
                            FirebaseFirestoreException.Code.ALREADY_EXISTS);
                }
                // 2) Ukloni staru rezervaciju
                DocumentReference oldColRef = db.collection("category_colors").document(oldKey);
                tr.delete(oldColRef);

                // 3) Upisi novu rezervaciju
                Map<String, Object> colDoc = new HashMap<>();
                colDoc.put("ownerUid", ownerUid);
                colDoc.put("categoryId", categoryId);
                tr.set(newColRef, colDoc);

                // 4) Updatuj kategoriju
                tr.update(catRef, "colorHex", newColorHex);
            }
            return null;
        }).addOnSuccessListener(ok).addOnFailureListener(err);
    }

    /** Izbriši kategoriju ako NEMA aktivnih taskova koji je koriste. */
    public void deleteCategoryIfUnused(String categoryId,
                                       OnSuccessListener<Void> ok,
                                       OnFailureListener err) {
        String ownerUid = FirebaseAuth.getInstance().getUid();
        if (ownerUid == null) { err.onFailure(new IllegalStateException("Not signed in")); return; }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference catRef = db.collection("categories").document(categoryId);

        // 1) Pročitaj kategoriju (doc get - OK u klijentu)
        catRef.get()
                .addOnSuccessListener(catSnap -> {
                    if (!catSnap.exists()) {
                        err.onFailure(new FirebaseFirestoreException(
                                "Kategorija ne postoji",
                                FirebaseFirestoreException.Code.NOT_FOUND));
                        return;
                    }
                    Category c = catSnap.toObject(Category.class);
                    if (c == null || !ownerUid.equals(c.ownerUid)) {
                        err.onFailure(new FirebaseFirestoreException(
                                "Zabranjeno",
                                FirebaseFirestoreException.Code.PERMISSION_DENIED));
                        return;
                    }

                    // 2) Napravi QUERY (van transakcije) da vidiš da li postoje AKTIVNI taskovi
                    db.collection("tasks")
                            .whereEqualTo("ownerUid", ownerUid)
                            .whereEqualTo("categoryId", categoryId)
                            .whereEqualTo("active", true)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(qs -> {
                                if (!qs.isEmpty()) {
                                    err.onFailure(new FirebaseFirestoreException(
                                            "Kategorija se ne može obrisati: postoje aktivni zadaci u ovoj kategoriji.",
                                            FirebaseFirestoreException.Code.FAILED_PRECONDITION));
                                    return;
                                }

                                // 3) Nema aktivnih taskova -> briši kategoriju + rezervaciju boje u batch-u
                                String key = ownerUid + "_" + c.colorHex.toUpperCase(java.util.Locale.ROOT);
                                DocumentReference colorRef = db.collection("category_colors").document(key);

                                com.google.firebase.firestore.WriteBatch batch = db.batch();
                                batch.delete(catRef);
                                batch.delete(colorRef);

                                batch.commit()
                                        .addOnSuccessListener(v -> ok.onSuccess(null))
                                        .addOnFailureListener(err);
                            })
                            .addOnFailureListener(err);
                })
                .addOnFailureListener(err);
    }

    /** STREAM: slušaj moje kategorije; vrati ListenerRegistration za odjavu. */
    public ListenerRegistration listenMyCategories(
            EventListener<List<Category>> listener) {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            // odma’ javi prazan rezultat
            listener.onEvent(new ArrayList<>(), null);
            return () -> {};
        }

        return db.collection("categories")
                .whereEqualTo("ownerUid", uid)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) {
                        listener.onEvent(new ArrayList<>(), err);
                        return;
                    }
                    List<Category> list = snap.toObjects(Category.class);
                    listener.onEvent(list, null);
                });
    }

    /** ONE-SHOT: učitaj jednom moje kategorije. */
    public void getMyCategories(
            com.google.android.gms.tasks.OnSuccessListener<List<Category>> ok,
            com.google.android.gms.tasks.OnFailureListener err) {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { err.onFailure(new IllegalStateException("Not signed in")); return; }

        db.collection("categories")
                .whereEqualTo("ownerUid", uid)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(qs -> ok.onSuccess(qs.toObjects(Category.class)))
                .addOnFailureListener(err);
    }
}
