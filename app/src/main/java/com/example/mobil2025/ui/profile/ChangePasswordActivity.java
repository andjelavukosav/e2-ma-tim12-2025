package com.example.mobil2025.ui.profile;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobil2025.R;
import com.example.mobil2025.util.Validators;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText editOldPassword, editNewPassword, editConfirmPassword;
    private Button buttonChangePassword;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        auth = FirebaseAuth.getInstance();
        editOldPassword = findViewById(R.id.editOldPassword);
        editNewPassword = findViewById(R.id.editNewPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        buttonChangePassword = findViewById(R.id.buttonChangePassword);

        buttonChangePassword.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String oldPass = editOldPassword.getText().toString().trim();
        String newPass = editNewPassword.getText().toString().trim();
        String confirmPass = editConfirmPassword.getText().toString().trim();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Korisnik nije prijavljen", Toast.LENGTH_SHORT).show();
            return;
        }

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Popunite sva polja", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Validators.isPasswordValid(newPass)) {
            Toast.makeText(this, "Nova lozinka mora imati najmanje 6 karaktera", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Validators.doPasswordsMatch(newPass, confirmPass)) {
            Toast.makeText(this, "Nove lozinke se ne poklapaju", Toast.LENGTH_SHORT).show();
            return;
        }

        // reautentifikacija sa starom lozinkom
        auth.signInWithEmailAndPassword(user.getEmail(), oldPass)
                .addOnSuccessListener(result -> {
                    user.updatePassword(newPass)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Lozinka uspješno promijenjena", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Greška pri promjeni lozinke: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Stara lozinka nije tačna", Toast.LENGTH_SHORT).show());
    }
}

