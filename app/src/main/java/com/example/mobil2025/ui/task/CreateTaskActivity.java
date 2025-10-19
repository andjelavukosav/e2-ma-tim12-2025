package com.example.mobil2025.ui.task;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobil2025.R;
import com.example.mobil2025.data.repo.CategoryRepository;
import com.example.mobil2025.data.repo.TaskRepository;
import com.example.mobil2025.model.Category;
import com.example.mobil2025.model.Task;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.*;

public class CreateTaskActivity extends AppCompatActivity {

    private EditText etName, etDesc;
    private Spinner spinnerCategory, spinnerUnit, spinnerWeight, spinnerImportance;
    private RadioGroup rgTaskType;
    private LinearLayout layoutRecurring, layoutSingle;
    private Button btnStartDate, btnEndDate, btnDueTime, btnSave;
    private NumberPicker npInterval;

    private long startDate, endDate, dueTime;
    private String selectedCategoryId;

    private final TaskRepository taskRepo = new TaskRepository();
    private final CategoryRepository categoryRepo = new CategoryRepository();

    // --- novo: držimo liste i listener
    private final List<Category> categories = new ArrayList<>();
    private ArrayAdapter<CategoryRow> categoryAdapter;
    private ListenerRegistration catReg; // za odjavu u onDestroy

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        NumberPicker np = findViewById(R.id.npInterval);
        np.setMinValue(1);
        np.setMaxValue(30);
        np.setWrapSelectorWheel(false);

        // Bind UI
        etName = findViewById(R.id.etTaskName);
        etDesc = findViewById(R.id.etTaskDesc);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        spinnerWeight = findViewById(R.id.spinnerWeight);
        spinnerImportance = findViewById(R.id.spinnerImportance);
        rgTaskType = findViewById(R.id.rgTaskType);
        layoutRecurring = findViewById(R.id.layoutRecurring);
        layoutSingle = findViewById(R.id.layoutSingle);
        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);
        btnDueTime = findViewById(R.id.btnDueTime);
        btnSave = findViewById(R.id.btnSaveTask);
        npInterval = findViewById(R.id.npInterval);

        setupSpinners();
        setupRadioGroup();
        setupDateTimePickers();
        setupSaveButton();
        loadCategories(); // ← sada stvarne kategorije iz baze
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (catReg != null) { catReg.remove(); catReg = null; }
    }

    private void setupSpinners() {
        // Jedinica
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                Arrays.asList("Dan", "Nedelja")
        );
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnit.setAdapter(unitAdapter);

        // Težina
        ArrayAdapter<String> weightAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                Arrays.asList("Veoma lak - 1 XP","Lak - 3 XP","Težak - 7 XP","Ekstremno težak - 20 XP")
        );
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWeight.setAdapter(weightAdapter);

        // Bitnost
        ArrayAdapter<String> impAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                Arrays.asList("Normalan - 1 XP","Važan - 3 XP","Ekstremno važan - 10 XP","Specijalan - 100 XP")
        );
        impAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerImportance.setAdapter(impAdapter);

        // Kategorije — adapter koji prikazuje ime + kolor tačkicu
        categoryAdapter = new ArrayAdapter<CategoryRow>(this, android.R.layout.simple_spinner_item, new ArrayList<>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                applyCategoryRowStyle(tv, getItem(position));
                return tv;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                applyCategoryRowStyle(tv, getItem(position));
                return tv;
            }
            private void applyCategoryRowStyle(TextView tv, CategoryRow row) {
                if (row == null) return;
                tv.setText(row.name);
                // mala obojena tačka pre teksta
                int size = (int) (tv.getTextSize() * 0.8f);
                android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
                dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                try { dot.setColor(android.graphics.Color.parseColor(row.colorHex)); }
                catch (Exception e) { dot.setColor(0xFF9E9E9E); } // default gray
                dot.setSize(size, size);
                tv.setCompoundDrawablesWithIntrinsicBounds(dot, null, null, null);
                tv.setCompoundDrawablePadding((int) (tv.getTextSize() * 0.4f));
            }
        };
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CategoryRow row = categoryAdapter.getItem(position);
                selectedCategoryId = (row != null) ? row.id : null;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                selectedCategoryId = null;
            }
        });
    }

    private void setupRadioGroup() {
        rgTaskType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbSingle) {
                layoutSingle.setVisibility(LinearLayout.VISIBLE);
                layoutRecurring.setVisibility(LinearLayout.GONE);
            } else {
                layoutSingle.setVisibility(LinearLayout.GONE);
                layoutRecurring.setVisibility(LinearLayout.VISIBLE);
            }
        });
    }

    private void setupDateTimePickers() {
        btnStartDate.setOnClickListener(v -> showDatePicker(date -> {
            startDate = date;
            btnStartDate.setText(new Date(date).toString());
        }));

        btnEndDate.setOnClickListener(v -> showDatePicker(date -> {
            endDate = date;
            btnEndDate.setText(new Date(date).toString());
        }));

        btnDueTime.setOnClickListener(v -> showTimePicker(time -> {
            dueTime = time;
            btnDueTime.setText(new Date(time).toString());
        }));
    }

    private void showDatePicker(final OnDateSelectedListener listener) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            listener.onSelected(cal.getTimeInMillis());
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(final OnDateSelectedListener listener) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            listener.onSelected(cal.getTimeInMillis());
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private interface OnDateSelectedListener { void onSelected(long timeMillis); }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> {
            if (etName.getText().toString().trim().isEmpty()) {
                etName.setError("Obavezno");
                return;
            }
            if (selectedCategoryId == null) {
                Toast.makeText(this, "Izaberi kategoriju", Toast.LENGTH_SHORT).show();
                return;
            }

            Task t = new Task();
            t.name = etName.getText().toString().trim();
            t.description = etDesc.getText().toString().trim();
            t.categoryId = selectedCategoryId;

            // XP težina
            switch (spinnerWeight.getSelectedItemPosition()) {
                case 0: t.weightXP=1; break;
                case 1: t.weightXP=3; break;
                case 2: t.weightXP=7; break;
                case 3: t.weightXP=20; break;
            }
            // XP bitnost
            switch (spinnerImportance.getSelectedItemPosition()) {
                case 0: t.importanceXP=1; break;
                case 1: t.importanceXP=3; break;
                case 2: t.importanceXP=10; break;
                case 3: t.importanceXP=100; break;
            }

            if (rgTaskType.getCheckedRadioButtonId() == R.id.rbSingle) {
                t.recurring = false;
                t.dueTime = dueTime;
            } else {
                t.recurring = true;
                t.startDate = startDate;
                t.endDate = endDate;
                t.recurrenceInterval = npInterval.getValue();
                // Spremi kao "day" / "week" (lakše za servis)
                String unitUi = String.valueOf(spinnerUnit.getSelectedItem()).toLowerCase(Locale.ROOT);
                t.recurrenceUnit = unitUi.startsWith("ned") ? "week" : "day";
            }

            taskRepo.createTask(t,
                    aVoid -> { Toast.makeText(this, "Zadatak kreiran", Toast.LENGTH_SHORT).show(); finish(); },
                    e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }

    /** Poveži spinner sa realnim kategorijama iz baze (live listen). */
    private void loadCategories() {
        // prikaži “Učitavam…” dok ne stignu podaci
        categoryAdapter.clear();
        categoryAdapter.add(new CategoryRow(null, "Učitavam…", "#9E9E9E"));
        selectedCategoryId = null;

        catReg = categoryRepo.listenMyCategories((list, err) -> {
            if (isFinishing() || isDestroyed()) return;

            categoryAdapter.clear();
            categories.clear();

            if (err != null) {
                // fallback: pokaži grešku
                categoryAdapter.add(new CategoryRow(null, "Greška pri učitavanju", "#E53935"));
                selectedCategoryId = null;
                return;
            }
            if (list == null || list.isEmpty()) {
                categoryAdapter.add(new CategoryRow(null, "Nema kategorija", "#9E9E9E"));
                selectedCategoryId = null;
                return;
            }

            categories.addAll(list);
            for (Category c : categories) {
                categoryAdapter.add(new CategoryRow(c.id, c.name, c.colorHex));
            }

            // po želji: setuj podrazumevanu selekciju (npr. prvu)
            spinnerCategory.setSelection(0);
            CategoryRow row = categoryAdapter.getItem(0);
            selectedCategoryId = (row != null) ? row.id : null;
        });
    }

    // mali DTO za prikaz u spinneru
    private static class CategoryRow {
        final String id;
        final String name;
        final String colorHex;
        CategoryRow(String id, String name, String colorHex) {
            this.id = id; this.name = name; this.colorHex = (colorHex != null ? colorHex : "#9E9E9E");
        }
        @Override public String toString() { return name; } // koristi se od ArrayAdapter-a
    }
}
