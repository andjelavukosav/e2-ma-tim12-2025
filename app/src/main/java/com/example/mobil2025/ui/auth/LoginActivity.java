package com.example.mobil2025.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobil2025.MainActivity;
import com.example.mobil2025.R;
import com.example.mobil2025.data.auth.FirebaseAuthManager;
import com.example.mobil2025.util.AuthErrorUtils;
import com.example.mobil2025.util.Validators;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

public class LoginActivity extends AppCompatActivity {

    private static final long ACTIVATION_WINDOW_MS = 2L * 60L * 1000L;

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegisterLink;
    private ProgressBar progress;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = new FirebaseAuthManager();
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        progress = findViewById(R.id.progress);

        btnLogin.setOnClickListener(v -> loginUser());
        tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (!Validators.isEmailValid(email)) { etEmail.setError("Neispravan email"); return; }
        if (password.isEmpty()) { etPassword.setError("Unesi lozinku"); return; }

        setLoading(true);

        authManager.signIn(email, password, (AuthResult result) -> {
            FirebaseUser user = result.getUser();
            if (user == null) { setLoading(false); toast("Greška pri prijavi."); return; }
            handlePostSignIn(user);
        }, e -> {
            setLoading(false);
            AuthErrorUtils.UiHint hint = AuthErrorUtils.fromException(e, true);
            toast(hint.message);
        });
    }

    private void handlePostSignIn(FirebaseUser user) {
        user.reload().addOnCompleteListener(t -> {
            if (!t.isSuccessful()) { setLoading(false); toast("Ne mogu da osvežim status."); return; }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        Long deadline = doc.getLong("activationDeadline");
                        boolean enabled = Boolean.TRUE.equals(doc.getBoolean("enabled"));
                        long now = System.currentTimeMillis();

                        if (!user.isEmailVerified()) {
                            setLoading(false);
                            toast("Proveri email i klikni na link za aktivaciju (važi 2 min).");
                            return;
                        }

                        if (deadline != null && now > deadline) {
                            toast("Aktivacioni link je istekao. Registruj se ponovo.");
                            deleteAccountAndProfileFully(user, () -> {
                                FirebaseAuth.getInstance().signOut();
                                setLoading(false);
                                startActivity(new Intent(this, RegisterActivity.class));
                                finish();
                            });
                            return;
                        }

                        if (!enabled) {
                            db.collection("users").document(user.getUid())
                                    .update("enabled", true)
                                    .addOnSuccessListener(v -> { setLoading(false); goToMain(); })
                                    .addOnFailureListener(e -> { setLoading(false); toast("Ne mogu da aktiviram profil."); });
                        } else {
                            setLoading(false);
                            goToMain();
                        }
                    })
                    .addOnFailureListener(e -> { setLoading(false); toast("Greška: " + e.getMessage()); });
        });
    }

    private void deleteAccountAndProfileFully(FirebaseUser user, Runnable onDone) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String uname = doc.exists() ? doc.getString("username") : null;

                    WriteBatch batch = db.batch();
                    batch.delete(db.collection("users").document(uid));
                    if (uname != null && !uname.trim().isEmpty()) {
                        batch.delete(db.collection("usernames").document(uname.toLowerCase()));
                    }

                    batch.commit()
                            .addOnSuccessListener(v -> {
                                user.delete()
                                        .addOnSuccessListener(x -> onDone.run())
                                        .addOnFailureListener(e -> {
                                            // npr. requires-recent-login: svejedno smo oslobodili podatke u bazi
                                            Toast.makeText(this, "Auth nalog nije obrisan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            onDone.run();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Brisanje iz baze nije uspelo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                // Ipak pokušaj obrisati auth nalog, pa nastavi
                                user.delete().addOnCompleteListener(t -> onDone.run());
                            });
                })
                .addOnFailureListener(e -> {
                    // Ako ne možemo da pročitamo profil, makar pokušaj obrisati auth nalog
                    user.delete().addOnCompleteListener(t -> onDone.run());
                });
    }


    private void goToMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void setLoading(boolean l) {
        progress.setVisibility(l ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!l);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
