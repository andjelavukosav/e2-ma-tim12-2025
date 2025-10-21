package com.example.mobil2025.ui.store;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobil2025.R;
import com.example.mobil2025.model.Clothing;
import com.example.mobil2025.model.ClothingType;
import com.example.mobil2025.model.Equipment;
import com.example.mobil2025.model.EquipmentType;
import com.example.mobil2025.model.Potion;
import com.example.mobil2025.model.UserProfile;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoreActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EquipmentAdapter adapter;
    private List<Equipment> equipmentList = new ArrayList<>();
    private FirebaseFirestore db;

    private UserProfile currentUserProfile;

    private Map<String, Equipment> masterEquipmentMap = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        recyclerView = findViewById(R.id.recyclerViewStore);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        db = FirebaseFirestore.getInstance();

        loadCurrentUserProfile();
    }

    private void loadCurrentUserProfile() {
        String uid = getCurrentUserUid(); // napravi metodu koja vraća uid trenutno ulogovanog korisnika
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUserProfile = doc.toObject(UserProfile.class);

                        // Nakon što je profil učitan, inicijalizujemo adapter
                        adapter = new EquipmentAdapter(this, equipmentList, currentUserProfile);
                        recyclerView.setAdapter(adapter);

                        loadEquipment();

                    } else {
                        Toast.makeText(this, "Korisnik nije pronađen", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Greška pri učitavanju korisnika", Toast.LENGTH_SHORT).show();
                });
    }


    private void loadEquipment() {
        db.collection("equipments").get()
                .addOnSuccessListener(querySnapshot -> {
                    equipmentList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {

                        String name = doc.getString("name");
                        int price = doc.getLong("price").intValue();
                        String typeStr = doc.getString("type");

                        EquipmentType type = EquipmentType.valueOf(typeStr.toUpperCase());

                        if (type == EquipmentType.WEAPON) {
                            // Weapon ne ide u prodavnicu
                            continue;
                        }

                        Equipment e;
                        if (type == EquipmentType.POTION) {
                            double powerBoost = doc.getDouble("powerBoost");
                            boolean permanent = doc.getBoolean("permanent");
                            e = new Potion(doc.getId(), name, price, powerBoost, permanent);
                        } else {
                            String clothingTypeStr = doc.getString("clothingType");
                            e = new Clothing(doc.getId(), name, price, 0.1,
                                    ClothingType.valueOf(clothingTypeStr.toUpperCase()));
                        }

                        equipmentList.add(e);
                    }
                    masterEquipmentMap.clear();
                    for (Equipment e : equipmentList) {
                        masterEquipmentMap.put(e.getUid(), e);
                    }


                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(StoreActivity.this,
                                "Ne mogu da učitam opremu", Toast.LENGTH_SHORT).show());
    }

    private String getCurrentUserUid() {
        return com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
    }


}
