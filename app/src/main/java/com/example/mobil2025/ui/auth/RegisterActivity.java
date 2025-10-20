package com.example.mobil2025.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobil2025.R;
import com.example.mobil2025.data.auth.FirebaseAuthManager;
import com.example.mobil2025.data.repo.UserRepository;
import com.example.mobil2025.util.Validators;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etPasswordConfirm, etUsername;
    private Button btnRegister;
    private ProgressBar progress;

    private ImageView ivFox, ivTurtle, ivLion, ivCat, ivPanda;
    private String selectedAvatarKey = "avatar_fox";

    private FirebaseAuthManager authManager;
    private UserRepository userRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authManager = new FirebaseAuthManager();
        userRepo = new UserRepository();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        etUsername = findViewById(R.id.etUsername);
        btnRegister = findViewById(R.id.btnRegister);
        progress = findViewById(R.id.progress);

        ivFox = findViewById(R.id.avatar_fox);
        ivTurtle = findViewById(R.id.avatar_turtle);
        ivLion = findViewById(R.id.avatar_lion);
        ivCat = findViewById(R.id.avatar_cat);
        ivPanda = findViewById(R.id.avatar_panda);

        View.OnClickListener avatarClick = v -> {
            clearSelections();
            v.setSelected(true);
            int id = v.getId();
            if (id == R.id.avatar_fox) selectedAvatarKey = "avatar_fox";
            else if (id == R.id.avatar_turtle) selectedAvatarKey = "avatar_turtle";
            else if (id == R.id.avatar_lion) selectedAvatarKey = "avatar_lion";
            else if (id == R.id.avatar_cat) selectedAvatarKey = "avatar_cat";
            else if (id == R.id.avatar_panda) selectedAvatarKey = "avatar_panda";
        };

        ivFox.setOnClickListener(avatarClick);
        ivTurtle.setOnClickListener(avatarClick);
        ivLion.setOnClickListener(avatarClick);
        ivCat.setOnClickListener(avatarClick);
        ivPanda.setOnClickListener(avatarClick);
        ivFox.setSelected(true);

        btnRegister.setOnClickListener(v -> onRegister());
    }

    private void clearSelections() {
        ivFox.setSelected(false);
        ivTurtle.setSelected(false);
        ivLion.setSelected(false);
        ivCat.setSelected(false);
        ivPanda.setSelected(false);
    }

    private void onRegister() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString();
        String pass2 = etPasswordConfirm.getText().toString();
        String username = etUsername.getText().toString().trim();

        if (!Validators.isEmailValid(email)) { etEmail.setError("Neispravan email"); return; }
        if (!Validators.isPasswordValid(pass)) { etPassword.setError("Min 6 karaktera"); return; }
        if (!Validators.doPasswordsMatch(pass, pass2)) { etPasswordConfirm.setError("Lozinke se ne poklapaju"); return; }
        if (!Validators.isUsernameValid(username)) { etUsername.setError("3-20, slova/brojevi ._-"); return; }

        setLoading(true);

        authManager.createUser(email, pass, result -> {
            FirebaseUser fu = result.getUser();
            if (fu == null) { setLoading(false); toast("Neočekovana greška"); return; }

            fu.sendEmailVerification()
                    .addOnSuccessListener(a -> toast("Proveri email — link važi 2 minuta."))
                    .addOnFailureListener(e -> toast("Nije poslata verifikacija: " + e.getMessage()));

            userRepo.createUserProfileWithUniqueUsername(
                    fu.getUid(), email, username, selectedAvatarKey,
                    aVoid -> {
                        setLoading(false);
                        toast("Registracija uspešna! Proveri email i aktiviraj nalog.");
                        finish();
                    },
                    e -> {
                        setLoading(false);
                        toast(e.getMessage());
                        FirebaseUser cur = authManager.currentUser();
                        if (cur != null) cur.delete();
                    }
            );
        }, e -> {
            setLoading(false);
            toast(e.getMessage());
        });
    }

    private void setLoading(boolean l) {
        progress.setVisibility(l ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!l);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
