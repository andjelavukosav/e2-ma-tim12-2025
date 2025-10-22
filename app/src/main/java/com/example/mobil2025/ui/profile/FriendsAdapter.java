package com.example.mobil2025.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mobil2025.R;
import com.example.mobil2025.model.UserProfile;

import java.util.ArrayList;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    private final List<UserProfile> friends;
    private ImageView imageAvatar;
    private final List<String> selectedFriendIds = new ArrayList<>();

    public FriendsAdapter(List<UserProfile> friends) {
        this.friends = friends;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        UserProfile friend = friends.get(position);
        holder.tvUsername.setText(friend.username);

        if (friend.avatarKey != null && !friend.avatarKey.isEmpty()) {
            int resId = holder.ivAvatar.getContext()
                    .getResources()
                    .getIdentifier(friend.avatarKey, "drawable", holder.ivAvatar.getContext().getPackageName());

            if (resId != 0) {
                holder.ivAvatar.setImageResource(resId);
            } else {
                // Ako ne postoji drawable, koristi default
                holder.ivAvatar.setImageResource(R.mipmap.ic_launcher);
            }
        } else {
            holder.ivAvatar.setImageResource(R.mipmap.ic_launcher); // default avatar
        }

        holder.cbSelect.setOnCheckedChangeListener(null); // uklanja prethodni listener
        holder.cbSelect.setChecked(selectedFriendIds.contains(friend.uid)); // inicijalno stanje

        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedFriendIds.contains(friend.uid)) {
                    selectedFriendIds.add(friend.uid);
                }
            } else {
                selectedFriendIds.remove(friend.uid);
            }
        });
    }

    public List<String> getSelectedFriendIds() {
        return new ArrayList<>(selectedFriendIds);
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername;
        ImageView ivAvatar;
        CheckBox cbSelect;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            cbSelect = itemView.findViewById(R.id.cbSelect);
        }
    }
}
