package com.example.mobil2025.ui.inventory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobil2025.R;
import com.example.mobil2025.model.ClothingItem;
import com.example.mobil2025.model.Equipment;
import com.example.mobil2025.model.PotionItem;
import com.example.mobil2025.model.UserProfile;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    private UserProfile userProfile;
    private Context context;

    private Map<String, Equipment> masterMap;

    public InventoryAdapter(Context context, UserProfile userProfile, Map<String, Equipment> masterMap) {
        this.context = context;
        this.userProfile = userProfile;
        this.masterMap = masterMap;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Kombinujemo odeću i napitke u jednu listu za prikaz
        int clothingSize = userProfile.getClothingInventory().size();
        if (position < clothingSize) {
            ClothingItem item = userProfile.getClothingInventory().get(position);
            Equipment master = masterMap.get(item.getClothingId());
            holder.name.setText(master.getName());
            holder.type.setText("Odeća");
            holder.details.setText("Preostale borbe: " + item.getRemainingBattles());

            holder.activateButton.setVisibility(View.VISIBLE);
            updateClothingDetails(holder, item);
            updateClothingButton(holder.activateButton, item);

            holder.activateButton.setOnClickListener(v -> {
                if (!item.isActivated()) {
                    activateClothing(item, master != null ? master.getName() : "Odeća");
                } else {
                    deactivateClothing(item, master != null ? master.getName() : "Odeća");
                }

                updateUserInFirestore();
                notifyItemChanged(position);
            });

        } else {
            int potionIndex = position - clothingSize; // ➜ korigujemo indeks
            PotionItem potion = userProfile.getPotionInventory().get(potionIndex);

            Equipment master = masterMap.get(potion.getPotionId());
            holder.name.setText(master.getName());
            holder.type.setText("Napitak");

            updatePotionDetails(holder, potion);
            updatePotionButton(holder.activateButton, potion);


            holder.activateButton.setOnClickListener(v -> {
                if (potion.isConsumed()) {
                    Toast.makeText(context, "Napitak je već iskorišćen.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!potion.isActivated()) {
                    activatePotion(potion, master != null ? master.getName() : "Napitak");
                } else {
                    deactivatePotion(potion, master != null ? master.getName() : "Napitak");
                }

                updateUserInFirestore();
                notifyItemChanged(position);
            });
        }
    }

    private void updateClothingDetails(ViewHolder holder, ClothingItem item) {
        if (item.isBroken()) {
            holder.details.setText("Odeća je uništena.");
        } else if (item.isActivated()) {
            holder.details.setText("Aktivirana za borbu. Preostale borbe: " + item.getRemainingBattles());
        } else {
            holder.details.setText("Nije aktivirana. Preostale borbe: " + item.getRemainingBattles());
        }
    }

    private void updateClothingButton(Button button, ClothingItem item) {
        if (item.isBroken()) {
            button.setText("Uništena");
            button.setEnabled(false);
        } else if (item.isActivated()) {
            button.setText("Deaktiviraj");
            button.setEnabled(true);
        } else {
            button.setText("Aktiviraj");
            button.setEnabled(true);
        }
    }

    private void activateClothing(ClothingItem item, String name) {
        item.setActivated(true);
        Toast.makeText(context, name + " aktivirana!", Toast.LENGTH_SHORT).show();
    }

    private void deactivateClothing(ClothingItem item, String name) {
        item.setActivated(false);
        Toast.makeText(context, name + " deaktivirana!", Toast.LENGTH_SHORT).show();
    }

    private void updatePotionDetails(ViewHolder holder, PotionItem potion) {
        if (potion.isConsumed()) {
            holder.details.setText("Napitak je iskorišćen.");
        } else if (potion.isActivated()) {
            holder.details.setText("Napitak je aktiviran i spreman za borbu.");
        } else {
            holder.details.setText("Napitak nije aktiviran.");
        }
    }

    private void updatePotionButton(Button button, PotionItem potion) {
        if (potion.isConsumed()) {
            button.setText("Iskorišćen");
            button.setEnabled(false);
        } else if (potion.isActivated()) {
            button.setText("Deaktiviraj");
            button.setEnabled(true);
        } else {
            button.setText("Aktiviraj");
            button.setEnabled(true);
        }
    }

    private void activatePotion(PotionItem potion, String name) {
        if (potion.isConsumed()) {
            Toast.makeText(context, "Napitak je već iskorišćen.", Toast.LENGTH_SHORT).show();
            return;
        }

        potion.setActivated(true);
        Toast.makeText(context, name + " aktiviran!", Toast.LENGTH_SHORT).show();
    }

    private void deactivatePotion(PotionItem potion, String name) {
        potion.setActivated(false);
        Toast.makeText(context, name + " deaktiviran!", Toast.LENGTH_SHORT).show();
    }

    private void updateUserInFirestore() {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userProfile.uid)
                .set(userProfile)
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Greška pri čuvanju promena", Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public int getItemCount() {
        return userProfile.getClothingInventory().size() + userProfile.getPotionInventory().size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, type, details;
        Button activateButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textItemName);
            type = itemView.findViewById(R.id.textItemType);
            details = itemView.findViewById(R.id.textItemDetails);
            activateButton = itemView.findViewById(R.id.buttonActivate);
        }
    }
}
