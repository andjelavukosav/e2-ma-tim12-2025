package com.example.mobil2025.util;

import com.example.mobil2025.R;

/**
 * Pomocna klasa za mapiranje avatar kljuceva na drawable resurse.
 * Koristi se npr. kada želimo da prikažemo avatar korisnika po ključu.
 */
public final class AvatarUtils {
    private AvatarUtils() {}

    /**
     * Vraca ID slike u drawable folderu na osnovu ključa avatara.
     * Koristi se za prikaz slike pohranjenog korisnika.
     */
    public static int toDrawableRes(String key) {
        switch (key) {
            case "avatar_fox": return R.drawable.avatar_fox;
            case "avatar_turtle": return R.drawable.avatar_turtle;
            case "avatar_lion": return R.drawable.avatar_lion;
            case "avatar_cat": return R.drawable.avatar_cat;
            case "avatar_panda": return R.drawable.avatar_panda;
            default: return R.drawable.avatar_cat;
        }
    }
}
