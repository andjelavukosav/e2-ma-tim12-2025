package com.example.mobil2025.ui.store;

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
import com.example.mobil2025.model.Clothing;
import com.example.mobil2025.model.Equipment;
import com.example.mobil2025.model.EquipmentType;
import com.example.mobil2025.model.Potion;
import com.example.mobil2025.model.UserProfile;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.ViewHolder> {

    private List<Equipment> equipmentList;
    private UserProfile userProfile;
    private Context context;

    public EquipmentAdapter(Context context, List<Equipment> equipmentList, UserProfile userProfile) {
        this.context = context;
        this.equipmentList = equipmentList;
        this.userProfile = userProfile;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Equipment e = equipmentList.get(position);
        holder.name.setText(e.getName());
        holder.type.setText("Tip: " + e.getType().toString());
        holder.price.setText("Cena: " + e.getPrice());

        holder.buyButton.setOnClickListener(v -> handlePurchase(e));
    }

    private void handlePurchase(Equipment selected) {
        if (selected.getType() == EquipmentType.WEAPON) {
            Toast.makeText(context, "Oružje se ne može kupiti!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!canPurchase(selected)) {
            return;
        }

        performPurchase(selected);
        updateUserInFirestore();


        Toast.makeText(context,
                selected.getName() + " kupljeno!",
                Toast.LENGTH_SHORT).show();
    }

    private boolean canPurchase(Equipment selected) {

        if (userProfile.getCoins() < selected.getPrice()) {
            Toast.makeText(context, "Nedovoljno novčića!", Toast.LENGTH_SHORT).show();
            return false;
        }

        // za odjeću – provjeri da li već ima tu vrstu
        if (selected.getType() == EquipmentType.CLOTHING) {
            Clothing clothing = (Clothing) selected;

            boolean alreadyHasSameType = userProfile.getClothingInventory().stream()
                    .anyMatch(item -> item.getClothingId().equals(clothing.getUid()));

            if (alreadyHasSameType) {
                Toast.makeText(context,
                        "Već posjeduješ odeću tipa " + clothing.getClothingType(),
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }

    private void performPurchase(Equipment selected) {
        if (selected.getType() == EquipmentType.CLOTHING) {
            Clothing c = (Clothing) selected;
            userProfile.addClothing(c);

        } else if (selected.getType() == EquipmentType.POTION) {
            Potion p = (Potion) selected;
            userProfile.addPotion(p);
        }

        userProfile.setCoins(userProfile.getCoins() - selected.getPrice());
    }
    private void updateUserInFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userProfile.uid)
                .set(userProfile)
                .addOnSuccessListener(aVoid -> {
                    // opcionalno logovanje
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Greška pri čuvanju korisnika", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, type, price;

        Button buyButton;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textEquipmentName);
            type = itemView.findViewById(R.id.textEquipmentType);
            price = itemView.findViewById(R.id.textEquipmentPrice);
            buyButton = itemView.findViewById(R.id.buttonBuy);
        }
    }
}