package com.example.mobil2025.ui.boss;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobil2025.R;
import com.example.mobil2025.model.Boss;
import com.example.mobil2025.model.UserProfile;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Random;

public class BossFightActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private String ownerUid;
    private int bossLevel;
    private int bossHp, bossMaxHp;
    private int userPP;
    private double userSuccessRate;
    private int remainingAttacks = 5;

    private TextView tvBossHp, tvBossHpPercent, tvPlayerPP, tvHitChance, tvRemainingAttacks;
    private ProgressBar progressBossHp;
    private Button btnAttack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boss_fight);

        db = FirebaseFirestore.getInstance();

        // Preuzimanje UID-a i nivoa iz Intenta
        ownerUid = getIntent().getStringExtra("uid");
        bossLevel = getIntent().getIntExtra("level", 1);


        if (ownerUid == null) {
            Log.e("BOSS_ERROR", "User ID je null!");
            Toast.makeText(this, "Greška: nedostaje korisnički ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicijalizacija UI
        tvBossHp = findViewById(R.id.tv_boss_hp);
        tvBossHpPercent = findViewById(R.id.tv_boss_hp_percentage);
        tvPlayerPP = findViewById(R.id.tv_player_pp);
        tvHitChance = findViewById(R.id.tv_hit_chance);
        tvRemainingAttacks = findViewById(R.id.tv_remaining_attacks);
        progressBossHp = findViewById(R.id.progress_boss_hp);
        btnAttack = findViewById(R.id.btn_attack);

        // Učitaj podatke
        loadUserData();
        loadBossData();

        btnAttack.setOnClickListener(v -> performAttack());
    }

    private void loadUserData() {
        db.collection("users").document(ownerUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        userPP = doc.getLong("powerPoints").intValue();
                        userSuccessRate = doc.getDouble("successRate");
                        tvPlayerPP.setText(userPP + " PP");
                        tvHitChance.setText((int) userSuccessRate + "%");
                    }
                });
    }

    private void loadBossData() {
        String bossDocId = ownerUid + "_boss_" + bossLevel;
        db.collection("bosses").document(bossDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long hpVal = doc.getLong("hp");
                        Long maxHpVal = doc.getLong("maxHp");

                        bossHp = hpVal != null ? hpVal.intValue() : 200;
                        bossMaxHp = maxHpVal != null ? maxHpVal.intValue() : 200;
                    } else {
                        bossHp = 200;
                        bossMaxHp = 200;
                        db.collection("bosses").document("boss_" + bossLevel)
                                .set(new Boss(bossHp, bossMaxHp));
                    }
                    updateBossUi();
                });
    }

    private void performAttack() {
        if (remainingAttacks <= 0) {
            Toast.makeText(this, "Nemaš više pokušaja!", Toast.LENGTH_SHORT).show();
            return;
        }

        remainingAttacks--;

        Random random = new Random();
        int chance = random.nextInt(100);

        // Provera da li je napad uspešan
        if (chance < userSuccessRate) {
            bossHp -= userPP;
            if (bossHp < 0) bossHp = 0;

            Toast.makeText(this, "Uspešan napad! Boss je izgubio " + userPP + " HP!", Toast.LENGTH_SHORT).show();

            // 🔹 Pravi ID dokumenta: ownerUid + "_" + bossId
            String bossDocId = ownerUid + "_boss_" + bossLevel;

            // Kreiranje objekta Boss za update
            Boss updatedBoss = new Boss(
                    "boss_" + bossLevel, // id
                    ownerUid,
                    bossHp,
                    bossLevel,
                    bossMaxHp,
                    bossHp == 0 // defeated = true ako je HP 0
            );

            // Ažuriranje Firestore dokumenta sa merge opcijom
            db.collection("bosses").document(bossDocId)
                    .set(updatedBoss, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d("BOSS_DEBUG", "Boss updated successfully"))
                    .addOnFailureListener(e -> Log.e("BOSS_DEBUG", "Error updating boss: " + e.getMessage()));

            // Ako je boss pobijeđen, pokaži toast
            if (bossHp == 0) {
                // ID bossa
                // Ažuriranje bossa na defeated
                db.collection("bosses").document(bossDocId)
                        .update("defeated", true)
                        .addOnSuccessListener(aVoid -> Log.d("BOSS_DEBUG", "Boss defeated!"));

                // --- PRIBAVLJANJE I UPDATE NOVČIĆA ---
                db.collection("users").document(ownerUid)
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                long currentCoins = doc.getLong("coins") != null ? doc.getLong("coins") : 0;

                                // Izračunavanje nagrade
                                long reward;
                                if (bossLevel == 1) {
                                    reward = 200; // prvi boss
                                } else {
                                    // Nagrada = 200 * 1.2^(bossLevel-1)
                                    reward = Math.round(200 * Math.pow(1.2, bossLevel - 1));
                                }

                                long newCoins = currentCoins + reward;

                                // Update korisnika
                                db.collection("users").document(ownerUid)
                                        .update("coins", newCoins)
                                        .addOnSuccessListener(aVoid ->
                                                Toast.makeText(this, "Osvojila si " + reward + " novčića!", Toast.LENGTH_SHORT).show()
                                        )
                                        .addOnFailureListener(e ->
                                                Log.e("BOSS_DEBUG", "Greška pri update-u novčića: " + e.getMessage())
                                        );
                            }
                        })
                        .addOnFailureListener(e ->
                                Log.e("BOSS_DEBUG", "Greška pri dohvatu korisnika: " + e.getMessage())
                        );

                Toast.makeText(this, "🎉 Pobedila si Bossa!", Toast.LENGTH_LONG).show();
            }


        } else {
            Toast.makeText(this, "Napad nije uspeo 😢", Toast.LENGTH_SHORT).show();
        }

        updateBossUi();
    }


    private void updateBossUi() {
        tvBossHp.setText(bossHp + " / " + bossMaxHp + " HP");
        progressBossHp.setMax(bossMaxHp);
        progressBossHp.setProgress(bossHp);
        int percent = (int) ((bossHp * 100.0f) / bossMaxHp);
        tvBossHpPercent.setText(percent + "%");
        tvRemainingAttacks.setText(remainingAttacks + " / 5");
    }
}
