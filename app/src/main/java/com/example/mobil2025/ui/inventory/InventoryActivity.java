package com.example.mobil2025.ui.inventory;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobil2025.R;
import com.example.mobil2025.data.repo.EquipmentCallback;
import com.example.mobil2025.data.repo.EquipmentRepository;
import com.example.mobil2025.model.Equipment;
import com.example.mobil2025.model.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private UserProfile currentUserProfile;
    private FirebaseFirestore db;
    private Map<String, Equipment> masterMap = new HashMap<>();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        recyclerView = findViewById(R.id.recyclerViewInventory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();

        loadCurrentUserProfile();
    }


    private void loadCurrentUserProfile() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUserProfile = doc.toObject(UserProfile.class);
                        loadMasterEquipment();  // nastavljamo tek kada korisnik bude učitan
                    } else {
                        Toast.makeText(this, "Korisnik nije pronađen", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Greška pri učitavanju korisnika", Toast.LENGTH_SHORT).show());
    }

    private void loadMasterEquipment() {
        EquipmentRepository.loadEquipmentFromFirestore(new EquipmentCallback() {
            @Override
            public void onLoaded(List<Equipment> equipmentList) {
                for (Equipment e : equipmentList) {
                    masterMap.put(e.getUid(), e);
                }
                setupAdapter();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(InventoryActivity.this, "Greška pri učitavanju opreme", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void setupAdapter() {
        adapter = new InventoryAdapter(this, currentUserProfile, masterMap);
        recyclerView.setAdapter(adapter);
    }
}
