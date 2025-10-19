package com.example.mobil2025.ui.category;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobil2025.R;
import com.example.mobil2025.model.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

    public interface OnCategoryAction {
        void onEditColor(@NonNull Category c);
        void onDelete(@NonNull Category c);
    }

    private final List<Category> items;
    private final OnCategoryAction actions;

    public CategoryAdapter(List<Category> items,
                           java.util.function.Consumer<Category> onEditColor,
                           java.util.function.Consumer<Category> onDelete) {
        this.items = items;
        this.actions = new OnCategoryAction() {
            @Override public void onEditColor(@NonNull Category c) { onEditColor.accept(c); }
            @Override public void onDelete(@NonNull Category c) { onDelete.accept(c); }
        };
    }

    static class VH extends RecyclerView.ViewHolder {
        View viewColor;
        TextView tvName;
        ImageButton btnEditColor, btnDelete;

        VH(@NonNull View v) {
            super(v);
            viewColor = v.findViewById(R.id.viewColor);
            tvName = v.findViewById(R.id.tvCategoryName);
            btnEditColor = v.findViewById(R.id.btnEditColor);
            btnDelete = v.findViewById(R.id.btnDeleteCategory);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Category c = items.get(pos);
        h.tvName.setText(c.name != null ? c.name : "(bez naziva)");

        // oboji krug (bg_color_circle je shape; tint ćemo primeniti programatski)
        Drawable bg = h.viewColor.getBackground();
        if (bg != null) {
            Drawable wrap = DrawableCompat.wrap(bg.mutate());
            int color = Color.parseColor(c.colorHex != null ? c.colorHex : "#9E9E9E");
            DrawableCompat.setTint(wrap, color);
            h.viewColor.setBackground(wrap);
        }

        h.btnEditColor.setOnClickListener(v -> actions.onEditColor(c));
        h.btnDelete.setOnClickListener(v -> actions.onDelete(c));
    }

    @Override
    public int getItemCount() { return items.size(); }
}
