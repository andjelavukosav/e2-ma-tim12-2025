package com.example.mobil2025.ui.inventory;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;


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

public class ActiveEquipmentAdapter extends RecyclerView.Adapter<ActiveEquipmentAdapter.ViewHolder> {

    private List<Object> activeItems; // ClothingItem i PotionItem
    private Map<String, Equipment> masterMap;
    private Context context;
    private UserProfile userProfile;



    public ActiveEquipmentAdapter(Context context, UserProfile userProfile, Map<String, Equipment> masterMap, List<Object> activeItems) {
        this.context = context;
        this.userProfile = userProfile;
        this.masterMap = masterMap;
        this.activeItems = activeItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(com.example.mobil2025.R.layout.item_inventory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = activeItems.get(position);

        if (item instanceof ClothingItem) {
            ClothingItem c = (ClothingItem) item;
            Equipment master = masterMap.get(c.getClothingId());
            String name = master != null ? master.getName() : "Odjeća";

            holder.name.setText(name);
            holder.type.setText("Odjeća");
            updateClothingDetails(holder, c);
            holder.details.setText("Preostale borbe: " + c.getRemainingBattles());
            holder.activateButton.setText("Deaktiviraj");

            holder.activateButton.setOnClickListener(v -> {
                c.setActivated(false);
                Toast.makeText(context, name + " deaktivirana!", Toast.LENGTH_SHORT).show();
                updateUserInFirestore();
                notifyItemChanged(holder.getAdapterPosition());
            });

        } else if (item instanceof PotionItem) {
            PotionItem p = (PotionItem) item;
            Equipment master = masterMap.get(p.getPotionId());
            String name = master != null ? master.getName() : "Napitak";
            holder.type.setText("Napitak");
            updatePotionDetails(holder, p);
            holder.details.setText("Napitak aktiviran i spreman za borbu");
            holder.activateButton.setText("Deaktiviraj");

            holder.activateButton.setOnClickListener(v -> {
                p.setActivated(false);
                Toast.makeText(context, name + " deaktiviran!", Toast.LENGTH_SHORT).show();
                updateUserInFirestore();
                notifyItemChanged(holder.getAdapterPosition());
            });
        }
    }

    private void updateClothingDetails(ViewHolder holder, ClothingItem item) {
        if (item.isBroken()) {
            holder.details.setText("Odjeća je uništena.");
            holder.activateButton.setEnabled(false);
        } else if (item.isActivated()) {
            holder.details.setText("Aktivirana za borbu. Preostale borbe: " + item.getRemainingBattles());
            holder.activateButton.setEnabled(true);
        } else {
            holder.details.setText("Nije aktivirana. Preostale borbe: " + item.getRemainingBattles());
            holder.activateButton.setEnabled(true);
        }
    }

    private void updatePotionDetails(ViewHolder holder, PotionItem potion) {
        if (potion.isConsumed()) {
            holder.details.setText("Napitak je iskorišćen.");
            holder.activateButton.setEnabled(false);
        } else if (potion.isActivated()) {
            holder.details.setText("Napitak aktiviran i spreman za borbu.");
            holder.activateButton.setEnabled(true);
        } else {
            holder.details.setText("Napitak nije aktiviran.");
            holder.activateButton.setEnabled(true);
        }
    }



    @Override
    public int getItemCount() {
        return activeItems.size();
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

    private void updateUserInFirestore() {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userProfile.uid)
                .set(userProfile)
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Greška pri čuvanju promena", Toast.LENGTH_SHORT).show()
                );
    }
}