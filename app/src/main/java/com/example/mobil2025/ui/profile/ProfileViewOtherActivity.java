package com.example.mobil2025.ui.profile;


import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.mobil2025.R;
import com.example.mobil2025.data.repo.FriendRepository;
import com.example.mobil2025.model.UserProfile;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.List;
public class ProfileViewOtherActivity extends  AppCompatActivity{
    private ImageView imageAvatar, imageQRCode;
    private TextView textUsername, textTitle, textLevel, textXP, textBadges, textEquipment;
    private Button buttonAddFriend;

    private UserProfile otherUser;

    private FriendRepository friendRepository;
    private boolean isAlreadyFriend = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_other);

        friendRepository = new FriendRepository();

        initViews();
        loadUserData();
    }

    private void initViews() {
        imageAvatar = findViewById(R.id.imageAvatarOther);
        textUsername = findViewById(R.id.textUsernameOther);
        textTitle = findViewById(R.id.textTitleOther);
        textLevel = findViewById(R.id.textLevelOther);
        textXP = findViewById(R.id.textXPOther);
        textBadges = findViewById(R.id.textBadgesOther);
        textEquipment = findViewById(R.id.textEquipmentOther);
        imageQRCode = findViewById(R.id.imageQRCodeOther);
        buttonAddFriend = findViewById(R.id.buttonAddFriendOther);
    }

    private void loadUserData() {
        String username = getIntent().getStringExtra("username");
        String uid = getIntent().getStringExtra("uid");

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (uid != null && !uid.isEmpty()) {
            // Ako dolazi iz QR skenera
            fetchUserByUid(uid);
        } else if (username != null && !username.isEmpty()) {
            // Ako dolazi iz pretrage po username-u
            db.collection("usernames").document(username)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String userUid = doc.getString("uid");
                            fetchUserByUid(userUid);
                        } else {
                            Toast.makeText(this, "Korisnik ne postoji", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void fetchUserByUid(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        otherUser = doc.toObject(UserProfile.class);
                        populateUI();
                    }
                });
    }

    private void populateUI() {
        textUsername.setText(otherUser.username);
        textTitle.setText("Titula: " + otherUser.title);
        textLevel.setText("Level: " + otherUser.level);
        textXP.setText("XP: " + otherUser.xp);
        textBadges.setText("Bedževi: " + (otherUser.badges.isEmpty() ? "-" : String.join(", ", otherUser.badges)));
        String equipped = otherUser.equipment.isEmpty() ? "-" : otherUser.equipment.get(0);
        textEquipment.setText("Oprema: " + equipped);

        loadAvatar(otherUser.avatarKey);
        generateQRCode(otherUser.uid);
        checkFriendshipStatus();
    }

    private void checkFriendshipStatus() {
        friendRepository.isFriend(otherUser.uid, isFriend -> {
            isAlreadyFriend = isFriend;
            updateAddFriendButton();
        });
    }

    private void updateAddFriendButton() {
        if (isAlreadyFriend) {
            buttonAddFriend.setText("Ukloni prijatelja");
            buttonAddFriend.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
        } else {
            buttonAddFriend.setText("Dodaj prijatelja");
            buttonAddFriend.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        }
        setupAddFriend();
    }


    private void generateQRCode(String data) {
        if (data == null || data.isEmpty()) {
            imageQRCode.setImageResource(R.mipmap.ic_launcher); // fallback
            return;
        }

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 400, 400);
            imageQRCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Greška pri generisanju QR koda", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadAvatar(String avatarKey) {
        int resId = getResources().getIdentifier(avatarKey, "drawable", getPackageName());
        imageAvatar.setImageResource(resId);
    }


    private void setupAddFriend() {
        buttonAddFriend.setOnClickListener(v -> {
            if(isAlreadyFriend){
                // Ukloni prijatelja
                friendRepository.removeFriend(otherUser.uid, new FriendRepository.OnFriendActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(ProfileViewOtherActivity.this,
                                otherUser.username + " je uklonjen iz prijatelja!",
                                Toast.LENGTH_SHORT).show();
                        isAlreadyFriend = false;
                        updateAddFriendButton();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(ProfileViewOtherActivity.this,
                                "Greška: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }else {
                // Dodaj prijatelja
                friendRepository.addFriend(otherUser.uid, new FriendRepository.OnFriendActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(ProfileViewOtherActivity.this,
                                otherUser.username + " je dodat za prijatelja!",
                                Toast.LENGTH_SHORT).show();
                        isAlreadyFriend = true;
                        updateAddFriendButton();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(ProfileViewOtherActivity.this,
                                "Greška: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
