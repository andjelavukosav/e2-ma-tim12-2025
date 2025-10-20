package com.example.mobil2025.util;

import com.example.mobil2025.model.Level;

import java.util.ArrayList;
import java.util.List;

public class LevelManager {

    public static List<Level> generateLevels(int maxLevel) {
        List<Level> levels = new ArrayList<>();

        int xp = 200;  // XP za prvi nivo
        int pp = 40;   // PP za prvi nivo
        String[] titles = {"Početnik", "Naučenik", "Iskusni igrač", "Majstor", "Legendarni"};

        for (int i = 1; i <= maxLevel; i++) {
            String title = i <= titles.length ? titles[i - 1] : "Level " + i;
            levels.add(new Level(i, xp, pp, title));

            // Racunanje XP za sljedeci nivo
            xp = (int)Math.ceil((xp * 2 + xp / 2.0) / 100.0) * 100;

            // Racunanje PP za sledeci nivo
            pp = (int)Math.round(pp + pp * 0.75);
        }

        return levels;
    }
}

