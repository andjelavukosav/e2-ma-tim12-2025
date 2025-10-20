package com.example.mobil2025.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import com.example.mobil2025.R;
import com.example.mobil2025.model.UserProfile;

import java.util.ArrayList;
import java.util.List;
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {

    private List<UserProfile> users;
    private List<UserProfile> usersFiltered;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(UserProfile user);
    }

    public UserAdapter(List<UserProfile> users, OnUserClickListener listener) {
        this.users = users;
        this.usersFiltered = new ArrayList<>(users);
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        UserProfile u = usersFiltered.get(position);
        holder.textUsername.setText(u.username);
        int resId = holder.imgAvatar.getContext()
                .getResources()
                .getIdentifier(u.avatarKey, "drawable", holder.imgAvatar.getContext().getPackageName());
        holder.imgAvatar.setImageResource(resId);
        holder.itemView.setOnClickListener(v -> listener.onUserClick(u));
    }

    @Override
    public int getItemCount() {
        return usersFiltered.size();
    }

    public void updateList(List<UserProfile> newList) {
        this.users = new ArrayList<>(newList);
        this.usersFiltered = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        query = query.toLowerCase().trim();
        usersFiltered.clear();
        if (query.isEmpty()) {
            usersFiltered.addAll(users);
        } else {
            for (UserProfile u : users) {
                if (u.username != null && u.username.toLowerCase().contains(query)) {
                    usersFiltered.add(u);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView textUsername;
        ImageView imgAvatar;
        Button buttonAdd;

        VH(View itemView) {
            super(itemView);
            textUsername = itemView.findViewById(R.id.textUsername);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
        }
    }
}
