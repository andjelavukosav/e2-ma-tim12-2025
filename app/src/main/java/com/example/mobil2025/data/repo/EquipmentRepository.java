package com.example.mobil2025.data.repo;

import com.example.mobil2025.model.Clothing;
import com.example.mobil2025.model.ClothingType;
import com.example.mobil2025.model.Equipment;
import com.example.mobil2025.model.EquipmentType;
import com.example.mobil2025.model.Potion;
import com.example.mobil2025.model.Weapon;
import com.example.mobil2025.model.WeaponType;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.mobil2025.data.repo.EquipmentCallback;
public class EquipmentRepository {

    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static void loadEquipmentFromFirestore(EquipmentCallback callback) {
        db.collection("equipments").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Equipment> equipmentList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        Long priceLong = doc.getLong("price");
                        int price = priceLong != null ? priceLong.intValue() : 0;

                        String typeStr = doc.getString("type");
                        if (typeStr == null) {
                            System.err.println("Document " + doc.getId() + " nema polje 'type'");
                            continue; // preskoči dokument bez tipa
                        }

                        EquipmentType type;
                        try {
                            type = EquipmentType.valueOf(typeStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            System.err.println("Nepoznat tip opreme: " + typeStr);
                            continue;
                        }

                        Equipment e = null;

                        switch (type) {
                            case POTION:
                                Double powerBoost = doc.getDouble("powerBoost");
                                Boolean permanent = doc.getBoolean("permanent");
                                e = new Potion(
                                        doc.getId(),
                                        name,
                                        price,
                                        powerBoost != null ? powerBoost : 0,
                                        permanent != null && permanent
                                );
                                break;

                            case CLOTHING:
                                String clothingTypeStr = doc.getString("clothingType");
                                if (clothingTypeStr == null) {
                                    System.err.println("Document " + doc.getId() + " nema polje 'clothingType'");
                                    continue;
                                }
                                e = new Clothing(
                                        doc.getId(),
                                        name,
                                        price,
                                        0.1,
                                        ClothingType.valueOf(clothingTypeStr.toUpperCase())
                                );
                                break;

                            case WEAPON:
                                String weaponTypeStr = doc.getString("weaponType");
                                WeaponType weaponType = WeaponType.SWORD; // default
                                if (weaponTypeStr != null) {
                                    try {
                                        weaponType = WeaponType.valueOf(weaponTypeStr.toUpperCase());
                                    } catch (IllegalArgumentException ex) {
                                        System.err.println("Nepoznat WeaponType: " + weaponTypeStr);
                                    }
                                }

                                Double bonusValue = doc.getDouble("bonusValue");
                                String effectType = doc.getString("effectType") != null ? doc.getString("effectType") : "";

                                e = new Weapon(
                                        doc.getId(),
                                        name,
                                        weaponType,
                                        bonusValue != null ? bonusValue : 0,
                                        effectType
                                );
                                break;

                            default:
                                System.err.println("Nepoznat tip opreme: " + typeStr);
                                continue;
                        }

                        if (e != null) {
                            equipmentList.add(e);
                        }
                    }

                    callback.onLoaded(equipmentList);
                })
                .addOnFailureListener(callback::onError);
    }


    public static List<Equipment> getInitialStoreItems() {
        List<Equipment> items = new ArrayList<>();

        // Napici
        items.add(new Potion(null, "Napitak 20% PP", 50, 0.2, false));
        items.add(new Potion(null, "Napitak 40% PP", 70, 0.4, false));
        items.add(new Potion(null, "Trajni napitak +5% PP", 200, 0.05, true));
        items.add(new Potion(null, "Trajni napitak +10% PP", 1000, 0.1, true));

        // Odjeća
        items.add(new Clothing(null, "Rukavice", 60, 0.1, ClothingType.GLOVES));
        items.add(new Clothing(null, "Štit", 60, 0.1, ClothingType.SHIELD));
        items.add(new Clothing(null, "Čizme", 80, 0.4, ClothingType.BOOTS));

        // --- Oružje ---
        items.add(new Weapon(null, "Mač", WeaponType.SWORD, 0.05, "PP"));  // +5% snage
        items.add(new Weapon(null, "Luk i strijela", WeaponType.BOW, 0.05, "COINS")); // +5% novca


        return items;
    }

    public static void addStoreItemsIfNotExists() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Equipment> items = getInitialStoreItems();

        for (Equipment e : items) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", e.getName());
            data.put("price", e.getPrice());
            data.put("type", e.getType().toString());

            if (e instanceof Potion) {
                Potion p = (Potion) e;
                data.put("powerBoost", p.getPowerBoost());
                data.put("permanent", p.isPermanent());
            } else if (e instanceof Clothing) {
                Clothing c = (Clothing) e;
                data.put("bonusValue", c.getBonusValue());
                data.put("clothingType", c.getClothingType().toString());
            }else if (e instanceof Weapon) {
                Weapon w = (Weapon) e;
                data.put("weaponType", w.getWeaponType().toString());
                data.put("bonusValue", w.getBonusValue());
                data.put("effectType", w.getEffectType());
            }

            // Firestore automatski generiše UID
            DocumentReference newDocRef = db.collection("equipments").document();

            // Postavi UID u objekt (ako želiš da čuvaš u svom modelu)
            e.setUid(newDocRef.getId());

            // Dodaj dokument
            newDocRef.set(data)
                    .addOnSuccessListener(aVoid -> System.out.println(e.getName() + " added with UID " + e.getUid()))
                    .addOnFailureListener(err -> System.err.println("Failed to add " + e.getName()));
        }
    }


}
