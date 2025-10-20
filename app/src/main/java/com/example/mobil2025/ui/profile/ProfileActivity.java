package com.example.mobil2025.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobil2025.R;
import com.example.mobil2025.model.UserProfile;
import com.example.mobil2025.ui.auth.LoginActivity;
import com.example.mobil2025.ui.category.CategoryListActivity;
import com.example.mobil2025.ui.task.CalendarActivity;
import com.example.mobil2025.ui.task.CreateTaskActivity;
import com.example.mobil2025.ui.task.TaskListActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

//import com.bumptech.glide.Glide;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ImageView imageAvatar;
    private TextView textUsername, textLevel, textTitle, textXP, textPP, textCoins,
            textBadges, textEquipment, textQRCode;
    private Button buttonLogout, buttonChangePassword, btnViewUsers, btnCreateTask, btnOpenCategories, btnShowTasks, btnOpenCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1️ Inicijalizacija Firebase servisa
        initFirebase();

        // 2 Inicijalizacija UI elemenata
        initViews();

        // 3️ Provjera korisnika i učitavanje profila
        loadUserProfile();

        // 4️ Postavljanje akcija na dugmad
        setupListeners();
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    //povezivanje promjenljivih sa stvarnim elementima u XML-u
    private void initViews() {
        imageAvatar = findViewById(R.id.imageAvatar);
        textUsername = findViewById(R.id.textUsername);
        textLevel = findViewById(R.id.textLevel);
        textTitle = findViewById(R.id.textTitle);
        textXP = findViewById(R.id.textXP);
        textPP = findViewById(R.id.textPP);
        textCoins = findViewById(R.id.textCoins);
        textBadges = findViewById(R.id.textBadges);
        textEquipment = findViewById(R.id.textEquipment);
        textQRCode = findViewById(R.id.textQRCode);
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonChangePassword = findViewById(R.id.buttonChangePassword);
        btnViewUsers = findViewById(R.id.buttonViewUsers);
        btnOpenCategories = findViewById(R.id.btnOpenCategories);
        btnCreateTask = findViewById(R.id.btnCreateTask);
        btnShowTasks = findViewById(R.id.btnShowTasks);
        btnOpenCalendar = findViewById(R.id.btnOpenCalendar);
    }

    private void loadUserProfile() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserProfile profile = documentSnapshot.toObject(UserProfile.class);
                        if (profile != null) displayProfile(profile);
                    } else {
                        Toast.makeText(this, "Profil nije pronađen.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Greška pri učitavanju profila.", Toast.LENGTH_SHORT).show()
                );
    }

    private void setupListeners() {
        buttonChangePassword.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, ChangePasswordActivity.class))
        );

        buttonLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        btnViewUsers.setOnClickListener(v -> {
            Intent i = new Intent(ProfileActivity.this, UsersActivity.class);
            startActivity(i);
        });

        btnCreateTask.setOnClickListener(v ->
                startActivity(new Intent(this, CreateTaskActivity.class)));

        btnOpenCategories.setOnClickListener(v ->
                startActivity(new Intent(this, CategoryListActivity.class)));

        btnShowTasks.setOnClickListener(v ->
                startActivity(new Intent(this, TaskListActivity.class)));

        btnOpenCalendar.setOnClickListener(v ->
                startActivity(new Intent(this, CalendarActivity.class)));



    }

    private void displayProfile(UserProfile profile) {
        textUsername.setText(profile.username);
        textLevel.setText("Nivo: " + profile.level);
        textTitle.setText("Titula: " + profile.title);
        textXP.setText("XP: " + profile.xp);
        textPP.setText("Snaga (PP): " + profile.powerPoints);
        textCoins.setText("Novčići: " + profile.coins);
        textBadges.setText("Bedževi: " + String.join(", ", profile.badges));
        textEquipment.setText("Oprema: " + String.join(", ", profile.equipment));
        textQRCode.setText("QR kod: " + profile.qrCodeUrl);

        loadAvatar(profile.avatarKey);
    }

    private void loadAvatar(String avatarKey) {
        if (avatarKey != null && !avatarKey.isEmpty()) {
            int resId = getResources().getIdentifier(avatarKey, "drawable", getPackageName());
            if (resId != 0) {
                imageAvatar.setImageResource(resId);
            } else {
                imageAvatar.setImageResource(R.mipmap.ic_launcher); // fallback
            }
        } else {
            imageAvatar.setImageResource(R.mipmap.ic_launcher);
        }
    }
}
