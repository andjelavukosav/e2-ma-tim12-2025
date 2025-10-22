package com.example.mobil2025.ui.category;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobil2025.R;
import com.example.mobil2025.data.repo.CategoryRepository;
import com.example.mobil2025.model.Category;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

public class CategoryListActivity extends AppCompatActivity {

    private RecyclerView rv;
    private FloatingActionButton fab;
    private MaterialToolbar topBar;
    private CategoryAdapter adapter;
    private final List<Category> data = new ArrayList<>();

    private final CategoryRepository repo = new CategoryRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_categories);

        rv = findViewById(R.id.rvCategories);
        fab = findViewById(R.id.fabAddCategory);

        // Toolbar back
        if (topBar != null) {
            topBar.setNavigationOnClickListener(v -> finish());
            topBar.setOnMenuItemClickListener(this::onMenuClick);
        }

        // Recycler
        adapter = new CategoryAdapter(data,
                this::onEditColorClick,
                this::onDeleteClick
        );
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        fab.setOnClickListener(v -> showAddDialog());

        listenCategories();
    }

    private boolean onMenuClick(@NonNull MenuItem item) {
        // ostavljeno za buduće akcije (sort/filter)
        return false;
    }

    /** Realtime slušalac za moje kategorije. */
    private void listenCategories() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { Toast.makeText(this, "Niste prijavljeni", Toast.LENGTH_SHORT).show(); return; }

        Query q = FirebaseFirestore.getInstance()
                .collection("categories")
                .whereEqualTo("ownerUid", uid);


        q.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Toast.makeText(CategoryListActivity.this, "Greška: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }
                data.clear();
                if (value != null) {
                    for (var doc : value.getDocuments()) {
                        Category c = doc.toObject(Category.class);
                        if (c != null) data.add(c);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    // -------------------- DODAVANJE --------------------

    private void showAddDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null, false);
        EditText etName = view.findViewById(R.id.etCategoryName);
        GridLayout grid = view.findViewById(R.id.gridColors);

        // izbor boje
        final View[] selected = new View[1]; // trenutno izabrani "krug"
        for (int i = 0; i < grid.getChildCount(); i++) {
            View chip = grid.getChildAt(i);
            chip.setOnClickListener(v -> {
                if (selected[0] != null) selected[0].setSelected(false);
                v.setSelected(true);
                selected[0] = v;
            });
        }

        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle("Nova kategorija")
                .setView(view)
                .setPositiveButton("Sačuvaj", null)
                .setNegativeButton("Otkaži", (dialog, which) -> dialog.dismiss())
                .create();

        d.setOnShowListener(dialog -> {
            d.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    etName.setError("Unesi naziv");
                    return;
                }
                if (selected[0] == null || selected[0].getTag() == null) {
                    Toast.makeText(this, "Izaberi boju", Toast.LENGTH_SHORT).show();
                    return;
                }
                String colorHex = String.valueOf(selected[0].getTag()); // npr. "#43A047"

                repo.createCategoryUniqueColor(name, colorHex,
                        ref -> {
                            Toast.makeText(this, "Dodato", Toast.LENGTH_SHORT).show();
                            d.dismiss();
                        },
                        e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
            });
        });

        d.show();
    }

    // -------------------- IZMENA BOJE --------------------

    private void onEditColorClick(@NonNull Category c) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null, false);
        EditText etName = view.findViewById(R.id.etCategoryName);
        GridLayout grid = view.findViewById(R.id.gridColors);

        // pre-popuni naziv (read-only u ovom dijalogu; boju menjamo)
        etName.setText(c.name);
        etName.setEnabled(false);

        final View[] selected = new View[1];

        for (int i = 0; i < grid.getChildCount(); i++) {
            View chip = grid.getChildAt(i);

            // osiguraj da se čip može kliknuti (ako nisi već stavila u XML)
            chip.setClickable(true);
            chip.setFocusable(true);

            // preselektuj staru boju (null-safe + trim + ignoreCase)
            Object tagObj = chip.getTag();
            String hex = (tagObj instanceof String) ? ((String) tagObj).trim() : null;
            if (hex != null && c.colorHex != null && hex.equalsIgnoreCase(c.colorHex.trim())) {
                chip.setSelected(true);
                selected[0] = chip;
            }

            chip.setOnClickListener(v -> {
                if (selected[0] != null) selected[0].setSelected(false);
                v.setSelected(true);
                selected[0] = v;
                // (opciono) v.refreshDrawableState();  // ako želiš da odmah “preslika” state
            });
        }

        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle("Promeni boju")
                .setView(view)
                .setPositiveButton("Sačuvaj", null)
                .setNegativeButton("Otkaži", (dialog, which) -> dialog.dismiss())
                .create();

        d.setOnShowListener(dialog -> {
            d.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (selected[0] == null || selected[0].getTag() == null) {
                    Toast.makeText(this, "Izaberi boju", Toast.LENGTH_SHORT).show();
                    return;
                }
                String newColor = String.valueOf(selected[0].getTag());

                // Ako je ista boja, samo zatvori
                if (c.colorHex != null && c.colorHex.equalsIgnoreCase(newColor)) {
                    d.dismiss();
                    return;
                }

                repo.updateCategoryColor(c.id, newColor,
                        aVoid -> {
                            Toast.makeText(this, "Boja izmenjena", Toast.LENGTH_SHORT).show();
                            d.dismiss();
                        },
                        e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
            });
        });

        d.show();
    }

    // -------------------- BRISANJE --------------------

    private void onDeleteClick(@NonNull Category c) {
        new AlertDialog.Builder(this)
                .setTitle("Obriši kategoriju")
                .setMessage("Da li sigurno želiš da obrišeš \"" + c.name + "\"?\n" +
                        "Kategoriju nije moguće obrisati ako postoje aktivni zadaci u njoj.")
                .setPositiveButton("Obriši", (dialog, which) -> {
                    repo.deleteCategoryIfUnused(c.id,
                            aVoid -> Toast.makeText(this, "Obrisano", Toast.LENGTH_SHORT).show(),
                            e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }
}
