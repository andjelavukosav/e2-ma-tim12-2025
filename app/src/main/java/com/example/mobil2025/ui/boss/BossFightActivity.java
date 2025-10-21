package com.example.mobil2025.ui.boss;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobil2025.R;
import com.example.mobil2025.model.Boss;
import com.example.mobil2025.model.EquipmentType;
import com.example.mobil2025.model.UserProfile;
import com.example.mobil2025.ui.inventory.ActiveEquipmentActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
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

    private ImageView imgTreasureChest;
    private TextView tvObtainedRewards;
    private int totalAttacks = 0;               // ukupno izvedeni napadi

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
        Button btnViewEquipment = findViewById(R.id.btn_view_equipment);

        btnViewEquipment.setOnClickListener(v -> {
            Intent intent = new Intent(BossFightActivity.this, ActiveEquipmentActivity.class);
            startActivity(intent);
        });
        imgTreasureChest = findViewById(R.id.img_treasure_chest);
        tvObtainedRewards = findViewById(R.id.tv_obtained_rewards);
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

  /*  private void performAttack() {
        if (remainingAttacks <= 0) {
            Toast.makeText(this, "Nemaš više pokušaja!", Toast.LENGTH_SHORT).show();
            return;
        }

        remainingAttacks--;
        totalAttacks++;

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
                // 🔹 20% šansa da padne oprema
                Random dropChance = new Random();
                int dropRoll = dropChance.nextInt(100); // 0–99

                if (dropRoll < 20) { // 20% šansa
                    int typeRoll = dropChance.nextInt(100); // 0–99
                    EquipmentType dropType = (typeRoll < 95) ? EquipmentType.CLOTHING : EquipmentType.WEAPON;

                    Toast.makeText(this, "🎁 Dobićeš komad opreme!", Toast.LENGTH_SHORT).show();

                    // 🔹 Učitaj opremu tog tipa iz Firestore-a
                    db.collection("equipments")
                            .whereEqualTo("type", dropType.toString())
                            .get()
                            .addOnSuccessListener(query -> {
                                if (!query.isEmpty()) {
                                    // Izaberi nasumičan dokument iz liste
                                    Random pick = new Random();
                                    int randomIndex = pick.nextInt(query.size());
                                    var doc = query.getDocuments().get(randomIndex);

                                    String name = doc.getString("name");
                                    String id = doc.getId();

                                    // 🔹 Dodaj u korisnikov inventar
                                    Map<String, Object> newItem = new HashMap<>();
                                    newItem.put("equipmentId", id);
                                    newItem.put("name", name);
                                    newItem.put("type", dropType.toString());
                                    newItem.put("obtainedAt", System.currentTimeMillis());

                                    db.collection("users").document(ownerUid)
                                            .collection("inventory")
                                            .add(newItem)
                                            .addOnSuccessListener(ref ->
                                                    Toast.makeText(this, "✨ DobiIa si: " + name, Toast.LENGTH_LONG).show()
                                            )
                                            .addOnFailureListener(e ->
                                                    Log.e("DROP_DEBUG", "Greška pri dodavanju opreme: " + e.getMessage())
                                            );
                                }
                            })
                            .addOnFailureListener(e ->
                                    Log.e("DROP_DEBUG", "Greška pri dohvatu opreme: " + e.getMessage())
                            );
                } else {
                    Toast.makeText(this, "💨 Ovaj put nisi dobila opremu.", Toast.LENGTH_SHORT).show();
                }


                Toast.makeText(this, "🎉 Pobedila si Bossa!", Toast.LENGTH_LONG).show();
            }


        } else {
            Toast.makeText(this, "Napad nije uspeo 😢", Toast.LENGTH_SHORT).show();
        }

        updateBossUi();
    }*/
    // class fields you should have somewhere in your Activity/ViewModel
    private int attacksOnCurrentBoss = 0;   // RESET na 0 kada započne novi boss
    private static final double BASE_DROP_CHANCE = 0.20; // 20%

    private long calcBaseRewardForLevel(int bossLevel) {
        if (bossLevel <= 1) return 200;
        return Math.round(200 * Math.pow(1.2, bossLevel - 1));
    }

    private void tryDropEquipment(double dropChance) {
        Random rng = new Random();
        int roll = rng.nextInt(100); // 0-99
        if (roll >= (int)(dropChance * 100)) {
            Toast.makeText(this, "💨 Ovaj put nema opreme.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 95% odeća, 5% oružje
        int typeRoll = rng.nextInt(100);
        EquipmentType dropType = (typeRoll < 95) ? EquipmentType.CLOTHING : EquipmentType.WEAPON;

        Toast.makeText(this, "🎁 Dobićeš komad opreme!", Toast.LENGTH_SHORT).show();

        db.collection("equipments")
                .whereEqualTo("type", dropType.toString())
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        Random pick = new Random();
                        int randomIndex = pick.nextInt(query.size());
                        var doc = query.getDocuments().get(randomIndex);

                        String name = doc.getString("name");
                        String id = doc.getId();

                        Map<String, Object> newItem = new HashMap<>();
                        newItem.put("equipmentId", id);
                        newItem.put("name", name);
                        newItem.put("type", dropType.toString());
                        newItem.put("obtainedAt", System.currentTimeMillis());

                        db.collection("users").document(ownerUid)
                                .collection("inventory")
                                .add(newItem)
                                .addOnSuccessListener(ref ->
                                        Toast.makeText(this, "✨ Dobila si: " + name, Toast.LENGTH_LONG).show()
                                )
                                .addOnFailureListener(e ->
                                        Log.e("DROP_DEBUG", "Greška pri dodavanju opreme: " + e.getMessage())
                                );
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("DROP_DEBUG", "Greška pri dohvatu opreme: " + e.getMessage())
                );
    }

    private void grantCoins(long amount) {
        if (amount <= 0) return;
        db.collection("users").document(ownerUid)
                .get()
                .addOnSuccessListener(doc -> {
                    long currentCoins = (doc.exists() && doc.getLong("coins") != null) ? doc.getLong("coins") : 0L;
                    long newCoins = currentCoins + amount;
                    db.collection("users").document(ownerUid)
                            .update("coins", newCoins)
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this, "Osvojila si " + amount + " novčića!", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Log.e("BOSS_DEBUG", "Greška pri update-u novčića: " + e.getMessage())
                            );
                })
                .addOnFailureListener(e ->
                        Log.e("BOSS_DEBUG", "Greška pri dohvatu korisnika: " + e.getMessage())
                );
    }

    private void updateBossInFirestore() {
        String bossDocId = ownerUid + "_boss_" + bossLevel;
        Boss updatedBoss = new Boss(
                "boss_" + bossLevel,
                ownerUid,
                bossHp,
                bossLevel,
                bossMaxHp,
                bossHp == 0
        );

        db.collection("bosses").document(bossDocId)
                .set(updatedBoss, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("BOSS_DEBUG", "Boss updated successfully"))
                .addOnFailureListener(e -> Log.e("BOSS_DEBUG", "Error updating boss: " + e.getMessage()));

        if (bossHp == 0) {
            db.collection("bosses").document(bossDocId)
                    .update("defeated", true)
                    .addOnSuccessListener(aVoid -> Log.d("BOSS_DEBUG", "Boss defeated!"));
        }
    }

    private void handleFiveAttackPayoutIfNeeded() {
        if (attacksOnCurrentBoss % 5 != 0) return; // radi samo na svakom 5. pokušaju

        // Bazna nagrada po nivou
        long reward = calcBaseRewardForLevel(bossLevel);
        double dropChance = BASE_DROP_CHANCE;

        if (bossHp == 0) {
            // Puna nagrada + puna šansa
            grantCoins(reward);
            tryDropEquipment(dropChance);
            Toast.makeText(this, "🎉 Pobedila si Bossa nakon 5 napada!", Toast.LENGTH_LONG).show();
            // Po želji: attacksOnCurrentBoss = 0; // ako startuješ novog bossa odmah
            return;
        }

        // Boss nije poražen
        if (bossHp <= bossMaxHp / 2) {
            // Prepolovljeno jer je posle 5 napada skinuto ≥ 50% HP
            long halved = Math.max(1, reward / 2);
            grantCoins(halved);
            tryDropEquipment(dropChance / 2.0); // 10%
            Toast.makeText(this, "⚔️ Posle 5 napada: boss još živi, ali je ispod 50% HP. Nagrada i šansa prepolovljene.", Toast.LENGTH_LONG).show();
        } else {
            // Nema nagrade jer posle 5 napada nije ni poražen ni na ≤50% HP
            Toast.makeText(this, "⏳ Posle 5 napada nema nagrade (boss je iznad 50% HP).", Toast.LENGTH_SHORT).show();
        }
    }

    private void onSuccessfulHit() {
        bossHp -= userPP;
        if (bossHp < 0) bossHp = 0;
        Toast.makeText(this, "Uspešan napad! Boss je izgubio " + userPP + " HP!", Toast.LENGTH_SHORT).show();
        updateBossInFirestore();
    }

    private void onMiss() {
        Toast.makeText(this, "Napad nije uspeo 😢", Toast.LENGTH_SHORT).show();
    }

    private boolean isHit(Random rng) {
        int chance = rng.nextInt(100); // 0–99
        return chance < userSuccessRate;
    }

    private void blockIfBossAlreadyDefeated() {
        if (bossHp <= 0) {
            Toast.makeText(this, "Boss je već poražen! 🎉", Toast.LENGTH_SHORT).show();
            updateBossUi();
            throw new IllegalStateException("Boss already defeated"); // ili jednostavno return iz performAttack
        }
    }

    private void decrementAttemptOrWarn() {
        if (remainingAttacks <= 0) {
            Toast.makeText(this, "Nemaš više pokušaja!", Toast.LENGTH_SHORT).show();
            throw new IllegalStateException("No more attempts");
        }
        remainingAttacks--;
        totalAttacks++;
        attacksOnCurrentBoss++; // SVAKI pokušaj se računa
    }

    private void safeRun(Runnable r) {
        try { r.run(); } catch (IllegalStateException ignore) {}
    }

    private void performAttack() {
        safeRun(() -> {
            blockIfBossAlreadyDefeated();        // spreči dalje “napade” na poraženog bossa
            decrementAttemptOrWarn();            // smanji pokušaje i uvećaj brojače

            Random random = new Random();
            if (isHit(random)) {
                onSuccessfulHit();
            } else {
                onMiss();
            }

            // Payout logika striktno “nakon 5 napada”
            handleFiveAttackPayoutIfNeeded();

            updateBossUi();
        });
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
