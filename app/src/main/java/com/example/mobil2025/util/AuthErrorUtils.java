package com.example.mobil2025.util;

import androidx.annotation.NonNull;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
public class AuthErrorUtils {

    private AuthErrorUtils() {}

    public enum TargetField { EMAIL, PASSWORD, NONE }

    public static final class UiHint {
        public final String message;
        public final TargetField target;

        public UiHint(@NonNull String message, @NonNull TargetField target) {
            this.message = message;
            this.target  = target;
        }
    }

    public static UiHint fromException(Exception e, boolean privacyMode) {
        // Nema mreze
        if (e instanceof FirebaseNetworkException) {
            return new UiHint("Nema internet veze. Pokušaj ponovo.", TargetField.NONE);
        }

        if (e instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) e).getErrorCode();
            switch (code) {
                case "ERROR_INVALID_EMAIL":
                    return new UiHint("Neispravan format email-a.", TargetField.EMAIL);

                case "ERROR_WRONG_PASSWORD":
                    if (privacyMode) {
                        return new UiHint("Pogrešan email ili lozinka.", TargetField.PASSWORD);
                    }
                    return new UiHint("Pogrešna lozinka.", TargetField.PASSWORD);

                case "ERROR_USER_NOT_FOUND":
                    if (privacyMode) {
                        return new UiHint("Pogrešan email ili lozinka.", TargetField.PASSWORD);
                    }
                    return new UiHint("Nalog sa ovim email-om ne postoji.", TargetField.EMAIL);

                case "ERROR_USER_DISABLED":
                    return new UiHint("Nalog je deaktiviran. Obratite se podršci.", TargetField.NONE);

                case "ERROR_TOO_MANY_REQUESTS":
                    return new UiHint("Previše pokušaja. Pokušaj kasnije.", TargetField.NONE);

                case "ERROR_OPERATION_NOT_ALLOWED":
                    return new UiHint("Prijava email/lozinka trenutno nije dozvoljena.", TargetField.NONE);

                case "ERROR_INVALID_CREDENTIAL":
                case "ERROR_INVALID_LOGIN_CREDENTIALS":
                case "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL":
                    return new UiHint("Pogrešan email ili lozinka.", TargetField.PASSWORD);

                default:
                    return new UiHint("Greška pri prijavi. Pokušaj ponovo.", TargetField.NONE);
            }
        }

        // Fallback za neocekivane greske
        return new UiHint("Neočekivana greška. Pokušaj ponovo.", TargetField.NONE);
    }
}
