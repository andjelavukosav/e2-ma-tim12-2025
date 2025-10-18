package com.example.mobil2025.util;

public final class Validators {
    private Validators() {}

    public static boolean isEmailValid(String email) {
        return email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isPasswordValid(String pass) {
        return pass != null && pass.length() >= 6;
    }

    public static boolean doPasswordsMatch(String p1, String p2) {
        return p1 != null && p1.equals(p2);
    }

    public static boolean isUsernameValid(String username) {
        return username != null && username.matches("^[a-zA-Z0-9._-]{3,20}$");
    }

    public static String normalizeUsernameKey(String username) {
        return username.toLowerCase(java.util.Locale.ROOT);
    }

    public static boolean isLoginInputValid(String email, String pass) {
        return isEmailValid(email) && isPasswordValid(pass);
    }

}
