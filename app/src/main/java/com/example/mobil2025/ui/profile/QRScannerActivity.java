package com.example.mobil2025.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobil2025.data.repo.FriendRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class QRScannerActivity extends AppCompatActivity {

    private FriendRepository friendRepository;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        friendRepository = new FriendRepository();

        // Pokreni skener odmah
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Skeniraj QR kod prijatelja");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Skeniranje otkazano", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                String scannedUid = result.getContents();
                handleScannedUser(scannedUid);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleScannedUser(String friendUid) {
        friendRepository.isFriend(friendUid, isFriend -> {
            if (isFriend) {
                Toast.makeText(this, "Već ste prijatelji!", Toast.LENGTH_SHORT).show();
                openUserProfile(friendUid);
            } else {
                // Ako nisu prijatelji, dodaj ih
                friendRepository.addFriend(friendUid, new FriendRepository.OnFriendActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(QRScannerActivity.this, "Korisnik dodat kao prijatelj!", Toast.LENGTH_SHORT).show();
                        openUserProfile(friendUid);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(QRScannerActivity.this, "Greška pri dodavanju prijatelja: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        openUserProfile(friendUid);
                    }
                });
            }
        });
    }

    private void openUserProfile(String uid) {
        Intent intent = new Intent(this, ProfileViewOtherActivity.class);
        intent.putExtra("uid", uid);
        startActivity(intent);
        finish();
    }
}