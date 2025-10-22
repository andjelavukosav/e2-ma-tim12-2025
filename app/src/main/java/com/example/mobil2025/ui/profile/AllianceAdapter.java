package com.example.mobil2025.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobil2025.R;
import com.example.mobil2025.model.Alliance;

import java.util.List;

public class AllianceAdapter extends RecyclerView.Adapter<AllianceAdapter.AllianceViewHolder> {

    private final List<Alliance> alliances;

    public AllianceAdapter(List<Alliance> alliances) {
        this.alliances = alliances;
    }

    @NonNull
    @Override
    public AllianceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alliance, parent, false);
        return new AllianceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AllianceViewHolder holder, int position) {
        Alliance alliance = alliances.get(position);
        holder.tvName.setText(alliance.getName());
    }

    @Override
    public int getItemCount() {
        return alliances.size();
    }

    static class AllianceViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        public AllianceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvAllianceName);
        }
    }
}
