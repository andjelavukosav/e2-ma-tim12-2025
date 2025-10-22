package com.example.mobil2025.ui.profile;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobil2025.R;
import com.example.mobil2025.model.Alliance;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AlliancesActivity extends AppCompatActivity {

    private RecyclerView rvAlliances;
    private AllianceAdapter adapter;
    private List<Alliance> allianceList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alliances);

        rvAlliances = findViewById(R.id.rvAlliances);
        rvAlliances.setLayoutManager(new LinearLayoutManager(this));

        allianceList = new ArrayList<>();
        adapter = new AllianceAdapter(allianceList);
        rvAlliances.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadUserAlliances();
    }

    private void loadUserAlliances() {
        String currentUserId = auth.getCurrentUser().getUid();
        db.collection("alliances")
                .whereArrayContains("memberIds", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allianceList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Alliance alliance = doc.toObject(Alliance.class);
                        allianceList.add(alliance);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Greška pri učitavanju saveza", Toast.LENGTH_SHORT).show());
    }
}
