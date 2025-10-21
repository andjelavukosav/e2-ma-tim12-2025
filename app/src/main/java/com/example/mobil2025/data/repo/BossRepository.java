package com.example.mobil2025.data.repo;

import androidx.annotation.NonNull;

import com.example.mobil2025.model.Boss;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class BossRepository {

    private final FirebaseFirestore db;
    private final CollectionReference bossRef;

    public BossRepository() {
        db = FirebaseFirestore.getInstance();
        bossRef = db.collection("bosses"); // Kolekcija u Firestore-u
    }



    public void createNextBossForUser(String ownerUid, int level, OnSuccessListener<Void> success, OnFailureListener failure) {
        // Prvo proveri da li postoji prethodni boss
        bossRef.whereEqualTo("ownerUid", ownerUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Boss> bosses = querySnapshot.toObjects(Boss.class);

                    // Pronadji boss sa najvećim level-om
                    Boss lastBoss = null;
                    int maxLevel = -1;
                    for (Boss b : bosses) {
                        if (b.getLevel() > maxLevel) {
                            maxLevel = b.getLevel();
                            lastBoss = b;
                        }
                    }

                    int newHp;
                    if (lastBoss != null) {
                        newHp = lastBoss.getMaxHp() * 2 + lastBoss.getMaxHp() / 2;
                    } else {
                        newHp = 200;
                    }

                    Boss newBoss = new Boss("boss_" + (maxLevel + 1), ownerUid, newHp, maxLevel + 1);
                    bossRef.document(ownerUid + "_" + newBoss.getId())
                            .set(newBoss)
                            .addOnSuccessListener(success)
                            .addOnFailureListener(failure);
                })
                .addOnFailureListener(failure);
    }

}
