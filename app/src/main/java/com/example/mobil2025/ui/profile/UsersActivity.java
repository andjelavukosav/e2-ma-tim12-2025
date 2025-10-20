package com.example.mobil2025.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.SearchView;

import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobil2025.R;
import com.example.mobil2025.model.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends AppCompatActivity {

    private RecyclerView recyclerUsers;
    private SearchView searchViewUsers;
    private UserAdapter adapter;
    private List<UserProfile> allUsers = new ArrayList<>();
    private String currentUsername;
    private String currentUid;

    private FirebaseAuth auth;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        initViews();
        initAdapter();
        loadCurrentUser();
        loadUsersFromFirebase();
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
        auth = FirebaseAuth.getInstance();        // inicijalizacija auth objekta
        currentUid = auth.getCurrentUser().getUid();  // dohvat UID-a trenutno ulogovanog korisnika
    }


    private void loadUsersFromFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<UserProfile> users = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        UserProfile u = doc.toObject(UserProfile.class);
                        if (u != null) users.add(u);
                    }
                    setUsers(users); // filtrira trenutno ulogovanog korisnika
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Greška pri učitavanju korisnika.", Toast.LENGTH_SHORT).show()
                );
    }


    private void setUsers(List<UserProfile> users) {
        List<UserProfile> filtered = new ArrayList<>();
        for (UserProfile u : users) {
            if (u.uid != null && !u.uid.equals(currentUid)) {  // filtriranje po UID
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

