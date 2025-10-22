package com.example.mobil2025.data.repo;

import com.example.mobil2025.config.AppConfig;
import com.example.mobil2025.model.Level;
import com.example.mobil2025.util.LevelManager;

import java.util.List;

public class LevelRepository {

    // Lista nivoa se generiše jednom i čuva u memoriji
    private static final List<Level> levels = LevelManager.generateLevels(AppConfig.MAX_LEVEL);

    // Vraća sve nivoe
    public static List<Level> getLevels() {
        return levels;
    }

    public static void printAllLevels() {
        System.out.println("=== Svi nivoi ===");
        for (Level l : levels) {
            System.out.println(
                    "Nivo: " + l.getLevel() +
                            ", XP: " + l.getRequiredXP() +
                            ", PP: " + l.getPowerPoints() +
                            ", Titula: " + l.getTitle()
            );
        }
        System.out.println("=== Kraj liste ===");
    }
    // Pronalazi trenutni nivo korisnika na osnovu XP
    public static Level getLevelForXP(int xp) {
        printAllLevels();
        if(xp < levels.get(0).getRequiredXP()) {
            // Korisnik nije dostigao prvi nivo
            return new Level(0, 0, 0, "Nema nivoa");
        }

        Level current = levels.get(0);
        for(Level l : levels) {
            if(xp >= l.getRequiredXP()) {
                current = l;
            } else break;
        }
        return current;
    }

    public static Level getNextLevel(Level current) {
        List<Level> levels = getLevels();
        int index = -1;
        for (int i = 0; i < levels.size(); i++) {
            if(levels.get(i).getLevel() == current.getLevel()) {
                index = i;
                break;
            }
        }
        if(index >= 0 && index < levels.size() - 1){
            return levels.get(index + 1);
        }
        return null;
    }


}
