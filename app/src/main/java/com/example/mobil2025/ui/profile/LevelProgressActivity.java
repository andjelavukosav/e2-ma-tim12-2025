package com.example.mobil2025.ui.profile;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobil2025.R;
import com.example.mobil2025.data.repo.LevelRepository;
import com.example.mobil2025.model.UserProfile;
import com.example.mobil2025.ui.boss.BossFightActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LevelProgressActivity extends AppCompatActivity {
    private TextView textLevelTitle, textLevel, textPP, textXP, textXPToNext;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_progress);
        Button btnGoToBoss = findViewById(R.id.btnGoToBoss);
        btnGoToBoss.setOnClickListener(v -> {
            Intent intent = new Intent(LevelProgressActivity.this, BossFightActivity.class);
            startActivity(intent);
        });
        textLevelTitle = findViewById(R.id.textLevelTitle);
        textLevel = findViewById(R.id.textLevel);
        textPP = findViewById(R.id.textPP);
        textXP = findViewById(R.id.textXP);
        textXPToNext = findViewById(R.id.textXPToNext);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserProfile();
    }


    private void loadUserProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if(user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()){
                        UserProfile profile = documentSnapshot.toObject(UserProfile.class);
                        if(profile != null) displayProfile(profile);
                    }
                });
    }

    private void displayProfile(UserProfile profile){
        int xpToNext = profile.addXPAndGetRemainingToNextLevel(0); // 0 jer ne dodajemo ništa, samo da dobijemo preostali XP

        textLevelTitle.setText("Titula: " + profile.getTitle());
        textLevel.setText("Nivo: " + profile.getLevel());
        textPP.setText("Snaga (PP): " + profile.getPowerPoints());
        textXP.setText("Trenutni XP: " + profile.getXp());

        textXPToNext.setText("XP do sledećeg nivoa: " + xpToNext);
    }
}
