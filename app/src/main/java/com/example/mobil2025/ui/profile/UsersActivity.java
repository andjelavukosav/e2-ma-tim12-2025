package com.example.mobil2025.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobil2025.R;
import com.example.mobil2025.model.UserProfile;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends AppCompatActivity {

    private RecyclerView recyclerUsers;
    private SearchView searchViewUsers;
    private UserAdapter adapter;
    private List<UserProfile> allUsers = new ArrayList<>();
    private String currentUsername;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        initViews();
        initAdapter();
        loadCurrentUser();
        setupSearch();
    }

    // Inicijalizacija UI elemenata
    private void initViews() {
        recyclerUsers = findViewById(R.id.recyclerUsers);
        searchViewUsers = findViewById(R.id.searchView);
        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initAdapter() {
        adapter = new UserAdapter(allUsers, new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(UserProfile user) {
                openUserProfile(user);
            }

        });
        recyclerUsers.setAdapter(adapter);
    }

    private void loadCurrentUser() {
        currentUsername = getSharedPreferences("my_app_prefs", MODE_PRIVATE)
                .getString("username", "");
    }

    private void setUsers(List<UserProfile> users) {
        List<UserProfile> filtered = new ArrayList<>();
        for (UserProfile u : users) {
            if (!u.username.equalsIgnoreCase(currentUsername)) {
                filtered.add(u);
            }
        }
        allUsers = filtered;
        adapter.updateList(allUsers);
    }

    private void setupSearch() {
        searchViewUsers.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });
    }

    private void openUserProfile(UserProfile user) {
        Intent intent = new Intent(this, ProfileViewOtherActivity.class);
        intent.putExtra("username", user.username);
        startActivity(intent);
    }

}

