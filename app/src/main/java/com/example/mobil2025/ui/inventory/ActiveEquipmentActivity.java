package com.example.mobil2025.ui.inventory;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobil2025.R;
import com.example.mobil2025.data.repo.EquipmentCallback;
import com.example.mobil2025.data.repo.EquipmentRepository;
import com.example.mobil2025.model.ClothingItem;
import com.example.mobil2025.model.Equipment;
import com.example.mobil2025.model.PotionItem;
import com.example.mobil2025.model.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActiveEquipmentActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ActiveEquipmentAdapter adapter;
    private List<Object> activeItems; // ClothingItem i PotionItem zajedno

    private UserProfile currentUserProfile;
    private Map<String, Equipment> masterMap = new HashMap<>();

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_equipment);

        recyclerView = findViewById(R.id.recyclerViewActiveEquipment);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();

        activeItems = new ArrayList<>();

        // Učitavamo korisnika i tek tada master map
        loadCurrentUserProfile();
    }

    private void loadCurrentUserProfile() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUserProfile = doc.toObject(UserProfile.class);

                        // Popuni activeItems iz korisničkog profila
                        activeItems.clear();
                        if (currentUserProfile != null) {
                            for (ClothingItem c : currentUserProfile.getClothingInventory()) {
                                if (c.isActivated()) activeItems.add(c);
                            }
                            for (PotionItem p : currentUserProfile.getPotionInventory()) {
                                if (p.isActivated()) activeItems.add(p);
                            }
                        }

                        // Sada učitavamo master map
                        loadMasterEquipment();

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
                masterMap.clear();
                for (Equipment e : equipmentList) {
                    masterMap.put(e.getUid(), e);
                }
                setupAdapter();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ActiveEquipmentActivity.this, "Greška pri učitavanju opreme", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAdapter() {
        adapter = new ActiveEquipmentAdapter(
                this,
                currentUserProfile,  // obavezno koristi currentUserProfile
                masterMap,
                activeItems
        );
        recyclerView.setAdapter(adapter);
    }

}