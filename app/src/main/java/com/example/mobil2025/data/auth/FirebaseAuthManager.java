package com.example.mobil2025.data.auth;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Klasa koja upravlja Firebase Authentication funkcionalnošću.
 * Odgovorna za kreiranje korisnika i pristup trenutno prijavljenom korisniku.
 */
public class FirebaseAuthManager {

    private final FirebaseAuth auth;

    public FirebaseAuthManager() {
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Kreira novog korisnika u Firebase Authentication servisu.
     *
     * @param email    korisnikov email
     * @param password lozinka
     * @param ok       callback ako je uspešno
     * @param err      callback ako dođe do greške
     */
    public void createUser(String email,
                           String password,
                           OnSuccessListener<AuthResult> ok,
                           OnFailureListener err) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(ok)
                .addOnFailureListener(err);
    }

    /**
     * Vraća trenutno prijavljenog korisnika, ako postoji.
     *
     * @return FirebaseUser ili null ako niko nije prijavljen
     */
    public FirebaseUser currentUser() {
        return auth.getCurrentUser();
    }

    public void signIn(String email, String password, OnSuccessListener<AuthResult> ok, OnFailureListener err) {
        auth.signInWithEmailAndPassword(email, password) // Firebase SDK kreira autentifikacioni token i cuva ga lokalno unutar apl
                .addOnSuccessListener(ok)
                .addOnFailureListener(err);
    }
}
