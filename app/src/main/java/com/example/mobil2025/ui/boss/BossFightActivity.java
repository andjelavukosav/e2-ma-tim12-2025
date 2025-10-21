package com.example.mobil2025.ui.boss;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobil2025.model.Boss;
import com.example.mobil2025.model.StageStats;
import com.example.mobil2025.util.BossManager;
import com.example.mobil2025.R;

import java.util.ArrayList;
import java.util.List;

public class BossFightActivity extends AppCompatActivity {

    private BossManager bossManager;
    // private EquipmentAdapter equipmentAdapter; // Oprema isključena
  //  private BossFightSession currentSession;

    // UI Components
    private TextView tvBossLevel;
    private TextView tvBossHp;
    private TextView tvBossHpPercentage;
    private ImageView imgBoss;
    private ProgressBar progressBossHp;

    private TextView tvRewardXp;
    private TextView tvRewardCoins;
    private TextView tvRewardEquipment;

    private TextView tvPlayerPP;
    private TextView tvHitChance;
    private TextView tvRemainingAttacks;

    // private RecyclerView rvEquipment; // Oprema isključena
    // private TextView tvNoEquipment; // Oprema isključena

    private CardView cardAttackHistory;
    private LinearLayout layoutAttackHistory;

    private Button btnAttack;
    private Button btnEndSession;

    // Data
    private int playerBasePP;
    private int playerTotalPP;
    // private List<Equipment> equippedItems; // Oprema isključena
    //private BossRewards  currentRewards;
    private StageStats stageStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boss_fight);

        initializeViews();
        initializeData();
        // setupRecyclerView(); // Oprema isključena
        setupListeners();
        startNewSession();
        updateUI();
    }

    private void initializeViews() {
        // Boss UI
        tvBossLevel = findViewById(R.id.tv_boss_level);
        tvBossHp = findViewById(R.id.tv_boss_hp);
        tvBossHpPercentage = findViewById(R.id.tv_boss_hp_percentage);
        imgBoss = findViewById(R.id.img_boss);
        progressBossHp = findViewById(R.id.progress_boss_hp);

        // Rewards UI
        tvRewardXp = findViewById(R.id.tv_reward_xp);
        tvRewardCoins = findViewById(R.id.tv_reward_coins);
        tvRewardEquipment = findViewById(R.id.tv_reward_equipment);

        // Player UI
        tvPlayerPP = findViewById(R.id.tv_player_pp);
        tvHitChance = findViewById(R.id.tv_hit_chance);
        tvRemainingAttacks = findViewById(R.id.tv_remaining_attacks);

        // Equipment UI - zakomentisano
        // rvEquipment = findViewById(R.id.rv_equipment);
        // tvNoEquipment = findViewById(R.id.tv_no_equipment);

        // Attack History
        cardAttackHistory = findViewById(R.id.card_attack_history);
        layoutAttackHistory = findViewById(R.id.layout_attack_history);

        // Buttons
        btnAttack = findViewById(R.id.btn_attack);
        btnEndSession = findViewById(R.id.btn_end_session);
    }

    private void initializeData() {
        bossManager = new BossManager(this);
        playerBasePP = getPlayerBasePP();
        // equippedItems = getPlayerEquippedItems(); // Oprema isključena
        playerTotalPP = calculateTotalPP();
        stageStats = getStageStatsFromUserSystem();
        Boss currentBoss = bossManager.getCurrentBoss();
        //currentRewards = new BossRewards(currentBoss.getLevel());
    }

    // private void setupRecyclerView() { ... } // Oprema isključena

    private void setupListeners() {
        btnAttack.setOnClickListener(v -> performAttack());
        btnEndSession.setOnClickListener(v -> endSessionManually());
    }

    private void startNewSession() {
      //  currentSession = bossManager.startNewFightSession(playerTotalPP, stageStats);
    }

    private void updateUI() {
      /*  Boss currentBoss = currentSession.getBoss();
        tvBossLevel.setText("BOSS NIVO " + currentBoss.getLevel());
        tvBossHp.setText(currentBoss.getCurrentHp() + " / " + currentBoss.getMaxHp() + " HP");
        float hpPercentage = currentBoss.getHpPercentage();
        tvBossHpPercentage.setText(String.format("%.0f%%", hpPercentage));
        animateProgressBar(progressBossHp, currentBoss.getCurrentHp());
        progressBossHp.setMax(currentBoss.getMaxHp());
        updateBossImage(currentBoss.getLevel());
        tvRewardXp.setText(currentRewards.getXpRewardText());
        tvRewardCoins.setText(currentRewards.getCoinsRewardText());
        // tvRewardEquipment.setText(currentRewards.getEquipmentRewardText()); // Oprema isključena
        tvPlayerPP.setText(playerTotalPP + " PP");
        tvHitChance.setText(stageStats.getSuccessRateFormatted());
        int remaining = currentSession.getRemainingAttacks();
        tvRemainingAttacks.setText(remaining + " / 5");

        if (currentSession.hasAttacksRemaining() && !currentSession.isBossDefeated()) {
            btnAttack.setEnabled(true);
            btnEndSession.setVisibility(View.VISIBLE);
        } else {
            btnAttack.setEnabled(false);
            if (currentSession.isSessionCompleted()) {
                btnEndSession.setVisibility(View.VISIBLE);
                btnEndSession.setText(currentSession.isBossDefeated() ?
                        "🏆 PREUZMI NAGRADE" : "😔 ZAVRŠI SESIJU");
            }
        }

        updateAttackHistory();*/
    }

    private void updateAttackHistory() {
       /* List<BossAttackResult> history = currentSession.getAttackHistory();
        if (history.isEmpty()) {
            cardAttackHistory.setVisibility(View.GONE);
            return;
        }
        cardAttackHistory.setVisibility(View.VISIBLE);
        layoutAttackHistory.removeAllViews();
        for (BossAttackResult result : history) {
            View attackView = createAttackHistoryItem(result);
            layoutAttackHistory.addView(attackView);
        }*/
    }

  /*  private View createAttackHistoryItem(BossAttackResult result) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.item_attack_result, layoutAttackHistory, false);
        TextView tvAttackNumber = view.findViewById(R.id.tv_attack_number);
        TextView tvAttackResult = view.findViewById(R.id.tv_attack_result);
        TextView tvDamage = view.findViewById(R.id.tv_damage);
        tvAttackNumber.setText("Napad #" + result.getAttackNumber());
        tvAttackResult.setText(result.getResultMessage());
        if (result.isHit()) {
            tvDamage.setText("-" + result.getDamageDealt() + " HP");
            tvDamage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
            tvAttackResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
        } else {
            tvDamage.setText("PROMAŠAJ");
            tvDamage.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            tvAttackResult.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }
        return view;
    }
*/
    private void animateProgressBar(ProgressBar progressBar, int targetProgress) {
        ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), targetProgress);
        animation.setDuration(500);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

   /* private void updateBossImage(int bossLevel) {
        int imageRes;
        if (bossLevel >= 20) imageRes = R.drawable.boss_dragon;
        else if (bossLevel >= 15) imageRes = R.drawable.boss_demon;
        else if (bossLevel >= 10) imageRes = R.drawable.boss_giant;
        else if (bossLevel >= 5) imageRes = R.drawable.boss_knight;
       // else imageRes = R.drawable.boss_goblin;
        try {
            imgBoss.setImageResource(imageRes);
        } catch (Exception e) {
            imgBoss.setImageResource(R.drawable.ic_boss_placeholder);
        }
    }*/

    private void performAttack() {
       /* if (!currentSession.hasAttacksRemaining()) {
            Toast.makeText(this, "Nemaš više pokušaja!", Toast.LENGTH_SHORT).show();
            return;
        }
        btnAttack.setEnabled(false);
        animateAttack();
        btnAttack.postDelayed(() -> {
            BossAttackResult result = currentSession.performAttack();
            showAttackResult(result);
            updateUI();
            if (currentSession.isSessionCompleted()) {
                btnAttack.postDelayed(() -> handleSessionCompletion(), 1500);
            } else {
                btnAttack.setEnabled(true);
            }
        }, 800);*/
    }

    private void animateAttack() {
        btnAttack.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100)
                .withEndAction(() -> btnAttack.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                .start();
        imgBoss.animate().translationX(-15f).setDuration(50)
                .withEndAction(() -> imgBoss.animate().translationX(15f).setDuration(50)
                        .withEndAction(() -> imgBoss.animate().translationX(-10f).setDuration(50)
                                .withEndAction(() -> imgBoss.animate().translationX(0f).setDuration(50).start())
                                .start())
                        .start())
                .start();
    }

  /*  private void showAttackResult(BossAttackResult result) {
        String message = result.getResultMessage();
        if (result.isHit()) {
            Toast.makeText(this, message + "\nPreostalo: " + result.getRemainingBossHp() + " HP",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }*/

    private void handleSessionCompletion() {
        /*BossManager.SessionCompletionResult completion = bossManager.completeSession();
        if (completion.bossDefeated) showVictoryDialog(completion);
        else showDefeatDialog(completion);*/
    }

    private void endSessionManually() {
      /*  if (currentSession.isBossDefeated()) handleSessionCompletion();
        else new AlertDialog.Builder(this)
                .setTitle("Potvrda")
                .setMessage("Da li želiš da završiš sesiju? Boss neće biti poražen.")
                .setPositiveButton("Da", (dialog, which) -> handleSessionCompletion())
                .setNegativeButton("Ne", null)
                .show();*/
    }

   /* private void showVictoryDialog(BossManager.SessionCompletionResult completion) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_boss_victory, null);
        builder.setView(dialogView);
        TextView tvVictoryTitle = dialogView.findViewById(R.id.tv_victory_title);
        TextView tvVictoryMessage = dialogView.findViewById(R.id.tv_victory_message);
        TextView tvVictoryXp = dialogView.findViewById(R.id.tv_victory_xp);
        TextView tvVictoryCoins = dialogView.findViewById(R.id.tv_victory_coins);
        TextView tvStats = dialogView.findViewById(R.id.tv_victory_stats);
        tvVictoryTitle.setText("🏆 POBEDA! 🏆");
        tvVictoryMessage.setText("Pobedio si Boss-a nivoa " + completion.statistics.initialBossHp + "!");
        tvVictoryXp.setText(currentRewards.getXpRewardText());
        tvVictoryCoins.setText(currentRewards.getCoinsRewardText());
        String stats = String.format("Pogoci: %d/%d (%.0f%%)\nUkupna šteta: %d",
                completion.statistics.successfulHits,
                completion.statistics.totalAttacks,
                completion.statistics.getActualHitRate(),
                completion.statistics.totalDamage);
        tvStats.setText(stats);
        builder.setPositiveButton("Nastavi", (dialog, which) -> {
            // giveRewardsToPlayer(currentRewards); // Oprema deo isključen
            resetForNextBoss();
            dialog.dismiss();
        });
        builder.setCancelable(false);
        builder.create().show();
    }*/

    /*private void showDefeatDialog(BossManager.SessionCompletionResult completion) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String message = String.format(
                "Nisi uspeo da poraziš bosa!\n\n" +
                        "Pogoci: %d/%d\n" +
                        "Šteta: %d/%d HP\n" +
                        "Boss HP: %d\n\n" +
                        "Pokušaj ponovo nakon što poboljšaš svoju uspešnost!",
                completion.statistics.successfulHits,
                completion.statistics.totalAttacks,
                completion.statistics.totalDamage,
                completion.statistics.initialBossHp,
                completion.statistics.finalBossHp
        );
        builder.setTitle("😔 Poraz")
                .setMessage(message)
                .setPositiveButton("Razumem", (dialog, which) -> {
                    finish();
                    dialog.dismiss();
                })
                .setCancelable(false)
                .create()
                .show();
    }*/

    /*private void resetForNextBoss() {
        bossManager.resetStageStats();
        currentRewards = new BossRewards(bossManager.getCurrentBoss().getLevel());
        startNewSession();
        updateUI();
    }
*/
    private int calculateTotalPP() {
        int total = playerBasePP;
        // for (Equipment equipment : equippedItems) total += equipment.getPowerBonus(); // Oprema isključena
        return total;
    }

    private int getPlayerBasePP() {
        return 150;
    }

    // private List<Equipment> getPlayerEquippedItems() { ... } // Oprema isključena

    private StageStats getStageStatsFromUserSystem() {
        int completedTasks = 10;
        int totalTasks = 15;
        return new StageStats(completedTasks, totalTasks);
    }
}
