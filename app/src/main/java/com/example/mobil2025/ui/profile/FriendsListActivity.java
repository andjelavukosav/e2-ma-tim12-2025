package com.example.mobil2025.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobil2025.R;
import com.example.mobil2025.data.repo.AllianceRepository;
import com.example.mobil2025.data.repo.UserRepository;
import com.example.mobil2025.model.Alliance;
import com.example.mobil2025.model.AllianceInvitation;
import com.example.mobil2025.model.UserProfile;

import java.util.ArrayList;
import java.util.List;

public class FriendsListActivity extends AppCompatActivity {

    private RecyclerView rvFriends;
    private FriendsAdapter adapter;
    private List<UserProfile> friendsList = new ArrayList<>();

    private EditText etAllianceName;
    private Button btnCreateAlliance;
    private TextView tvInviteFriendsTitle;

    private UserRepository userRepository;
    private Alliance createdAlliance;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

        etAllianceName = findViewById(R.id.etAllianceName);
        btnCreateAlliance = findViewById(R.id.btnCreateAlliance);
        tvInviteFriendsTitle = findViewById(R.id.tvInviteFriendsTitle);

        rvFriends = findViewById(R.id.rvFriends);
        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendsAdapter(friendsList);
        rvFriends.setAdapter(adapter);
        userRepository = new UserRepository();

        loadFriends();

        btnCreateAlliance.setOnClickListener(v -> createAlliance());
    }

    private void loadFriends() {
        UserRepository userRepository = new UserRepository();

        userRepository.getCurrentUserProfile(profile -> {
            if(profile != null && profile.friends != null){
                // Dobavljanje podataka za sve prijatelje
                userRepository.getUsersByIds(profile.friends,
                        users -> {  // success callback
                            friendsList.clear();
                            friendsList.addAll(users);
                            adapter.notifyDataSetChanged();
                        },
                        e -> {  // failure callback
                            Toast.makeText(this, "Greška pri učitavanju prijatelja", Toast.LENGTH_SHORT).show();
                        }
                );
            } else {
                Toast.makeText(this, "Nemate prijatelja", Toast.LENGTH_SHORT).show();
            }
        }, e -> {  // failure callback za getCurrentUserProfile
            Toast.makeText(this, "Greška pri učitavanju profila", Toast.LENGTH_SHORT).show();
        });
    }

    private void createAlliance() {
        String allianceName = etAllianceName.getText().toString().trim();
        if (allianceName.isEmpty()) {
            etAllianceName.setError("Unesite naziv saveza");
            return;
        }

        List<String> selectedFriendIds = adapter.getSelectedFriendIds();
        if (selectedFriendIds.isEmpty()) {
            Toast.makeText(this, "Morate izabrati barem jednog prijatelja za savez", Toast.LENGTH_SHORT).show();
            return;
        }

        AllianceRepository allianceRepo = new AllianceRepository();
        allianceRepo.createAlliance(allianceName, selectedFriendIds, new AllianceRepository.AllianceCallback() {
            @Override
            public void onSuccess(Alliance alliance) {
                createdAlliance = alliance;
                Toast.makeText(FriendsListActivity.this, "Savez kreiran i pozivi poslati prijateljima!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRejected(AllianceInvitation invitation) {}

            @Override
            public void onFailure(String error) {
                Toast.makeText(FriendsListActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void sendFriendInvites() {
        if (createdAlliance == null) return;

        List<String> selectedFriendIds = adapter.getSelectedFriendIds();
        if (selectedFriendIds.isEmpty()) {
            Toast.makeText(this, "Izaberite prijatelje za poziv", Toast.LENGTH_SHORT).show();
            return;
        }

        AllianceRepository allianceRepo = new AllianceRepository();
        allianceRepo.sendInvitesToFriends(createdAlliance, selectedFriendIds, new AllianceRepository.AllianceCallback() {
            @Override
            public void onSuccess(Alliance alliance) {
                Toast.makeText(FriendsListActivity.this, "Pozivi poslati prijateljima!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRejected(AllianceInvitation invitation) {}

            @Override
            public void onFailure(String error) {
                Toast.makeText(FriendsListActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
