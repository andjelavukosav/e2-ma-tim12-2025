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

    private String editTaskId = null;
    private Task loadedTask = null;

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
        editTaskId = getIntent().getStringExtra("editTaskId"); // null ako je kreiranje
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
        maybeLoadTaskForEdit();

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

            boolean isEdit = (editTaskId != null && !editTaskId.isEmpty());

            // ZAJEDNIČKA polja (name/desc/category/XP)
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("name", etName.getText().toString().trim());
            updates.put("description", etDesc.getText().toString().trim());
            updates.put("categoryId", selectedCategoryId);

            switch (spinnerWeight.getSelectedItemPosition()) {
                case 0: updates.put("weightXP", 1); break;
                case 1: updates.put("weightXP", 3); break;
                case 2: updates.put("weightXP", 7); break;
                case 3: updates.put("weightXP", 20); break;
            }
            switch (spinnerImportance.getSelectedItemPosition()) {
                case 0: updates.put("importanceXP", 1); break;
                case 1: updates.put("importanceXP", 3); break;
                case 2: updates.put("importanceXP", 10); break;
                case 3: updates.put("importanceXP", 100); break;
            }
            int weightXP = (int) updates.get("weightXP");
            int importanceXP = (int) updates.get("importanceXP");
            updates.put("totalXP", weightXP + importanceXP);

            long now = System.currentTimeMillis();

            if (rgTaskType.getCheckedRadioButtonId() == R.id.rbSingle) {
                // JEDNOKRATNI – zabrana izmene ako je vremenski završen
                if (isEdit && loadedTask != null) {
                    boolean blocked = "done".equalsIgnoreCase(loadedTask.status) || loadedTask.dueTime < now;
                    if (blocked) {
                        Toast.makeText(this, "Zadatak je završen – izmena nije dozvoljena.", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                updates.put("recurring", false);
                updates.put("dueTime", dueTime);

                if (isEdit) {
                    new TaskRepository().updateTaskFields(editTaskId, updates,
                            v2 -> { Toast.makeText(this, "Sačuvano", Toast.LENGTH_SHORT).show(); finish(); },
                            e2 -> Toast.makeText(this, e2.getMessage(), Toast.LENGTH_LONG).show());
                } else {
                    // kreiranje (tvoj postojeći createTask poziv)
                    Task t = new Task();
                    t.name = (String) updates.get("name");
                    t.description = (String) updates.get("description");
                    t.categoryId = (String) updates.get("categoryId");
                    t.weightXP = (int) updates.get("weightXP");
                    t.importanceXP = (int) updates.get("importanceXP");
                    t.recurring = false;
                    t.dueTime = dueTime;
                    new TaskRepository().createTask(t,
                            aVoid -> { Toast.makeText(this, "Zadatak kreiran", Toast.LENGTH_SHORT).show(); finish(); },
                            e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
                }

            } else {
                // PONAVLJAJUĆI – menja samo buduće (računamo novi nextDueAt ≥ now)
                updates.put("recurring", true);
                updates.put("startDate", startDate);
                updates.put("endDate", endDate);
                updates.put("recurrenceInterval", npInterval.getValue());
                updates.put("recurrenceUnit",
                        spinnerUnit.getSelectedItem().toString().toLowerCase(java.util.Locale.ROOT));
                // (ako čuvaš timeOfDay/tz – dodaj)
                // updates.put("timeOfDay", ...); updates.put("tz", ...);

                if (isEdit) {
                    // rekonstruišemo Task objekt minimalno da izračunamo nextDueAt
                    Task tmp = (loadedTask != null) ? loadedTask : new Task();
                    tmp.recurring = true;
                    tmp.startDate = startDate;
                    tmp.endDate   = endDate;
                    tmp.recurrenceInterval = npInterval.getValue();
                    tmp.recurrenceUnit = spinnerUnit.getSelectedItem().toString().toLowerCase(java.util.Locale.ROOT);
                    tmp.timeOfDay = (loadedTask != null) ? loadedTask.timeOfDay : "09:00";
                    tmp.tz = (loadedTask != null && loadedTask.tz != null) ? loadedTask.tz : java.util.TimeZone.getDefault().getID();

                    Long next = com.example.mobil2025.ui.task.RecurrenceUtils.computeNextDueFromNow(tmp);
                    updates.put("nextDueAt", next != null ? next : 0L);

                    new TaskRepository().updateTaskFields(editTaskId, updates,
                            v2 -> { Toast.makeText(this, "Izmene sačuvane", Toast.LENGTH_SHORT).show(); finish(); },
                            e2 -> Toast.makeText(this, e2.getMessage(), Toast.LENGTH_LONG).show());
                } else {
                    // kreiranje novog ponavljajućeg
                    Task t = new Task();
                    t.name = (String) updates.get("name");
                    t.description = (String) updates.get("description");
                    t.categoryId = (String) updates.get("categoryId");
                    t.weightXP = (int) updates.get("weightXP");
                    t.importanceXP = (int) updates.get("importanceXP");
                    t.recurring = true;
                    t.startDate = startDate;
                    t.endDate = endDate;
                    t.recurrenceInterval = npInterval.getValue();
                    t.recurrenceUnit = spinnerUnit.getSelectedItem().toString().toLowerCase(java.util.Locale.ROOT);
                    t.timeOfDay = "09:00"; // postavi ako imaš picker
                    t.tz = java.util.TimeZone.getDefault().getID();

                    Long next = com.example.mobil2025.ui.task.RecurrenceUtils.computeNextDueFromNow(t);
                    t.nextDueAt = (next != null ? next : 0L);

                    new TaskRepository().createTask(t,
                            aVoid -> { Toast.makeText(this, "Zadatak kreiran", Toast.LENGTH_SHORT).show(); finish(); },
                            e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }
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

    private void maybeLoadTaskForEdit() {
        if (editTaskId == null || editTaskId.isEmpty()) return;

        // promeni naslov/dugme po želji
        setTitle("Izmena zadatka");
        btnSave.setText("Sačuvaj izmene");

        new TaskRepository().getTaskById(editTaskId, snap -> {
            if (snap == null || !snap.exists()) return;
            loadedTask = snap.toObject(Task.class);
            if (loadedTask == null) return;

            // 1) Zabrani izmenu ako je jednokratan i završen ili “vremenski završen”
            long now = System.currentTimeMillis();
            boolean singleDone = !Boolean.TRUE.equals(loadedTask.recurring)
                    && ("done".equalsIgnoreCase(loadedTask.status) || loadedTask.dueTime < now);
            if (singleDone) {
                btnSave.setEnabled(false);
                Toast.makeText(this, "Zadatak je završen – izmena nije dozvoljena.", Toast.LENGTH_LONG).show();
            }

            // 2) Popuni polja u UI iz loadedTask
            etName.setText(loadedTask.name != null ? loadedTask.name : "");
            etDesc.setText(loadedTask.description != null ? loadedTask.description : "");

            // kategorija – nađi indeks u spinneru (po id-u)
            if (loadedTask.categoryId != null) {
                // pretpostavimo da si u loadCategories() sačuvala mapu name->id ili listu kategorija
                // Ako nemaš, dodeli selectedCategoryId direktno i ostavi UI kakav jeste
                selectedCategoryId = loadedTask.categoryId;
            }

            // težina i bitnost – pozicioniraj spinner-e po XP vrednosti
            // (mapiranje po tvojim opcijama)
            spinnerWeight.setSelection(weightIndexForXP(loadedTask.weightXP));
            spinnerImportance.setSelection(importanceIndexForXP(loadedTask.importanceXP));

            if (!Boolean.TRUE.equals(loadedTask.recurring)) {
                // jednokratni
                rgTaskType.check(R.id.rbSingle);
                layoutSingle.setVisibility(android.view.View.VISIBLE);
                layoutRecurring.setVisibility(android.view.View.GONE);
                dueTime = loadedTask.dueTime;
                if (dueTime > 0) btnDueTime.setText(new java.util.Date(dueTime).toString());
            } else {
                // ponavljajući
                rgTaskType.check(R.id.rbRecurring);
                layoutSingle.setVisibility(android.view.View.GONE);
                layoutRecurring.setVisibility(android.view.View.VISIBLE);

                startDate = loadedTask.startDate;
                endDate   = loadedTask.endDate;
                if (startDate > 0) btnStartDate.setText(new java.util.Date(startDate).toString());
                if (endDate > 0) btnEndDate.setText(new java.util.Date(endDate).toString());

                npInterval.setValue(Math.max(1, loadedTask.recurrenceInterval));
                // jedinica
                if (loadedTask.recurrenceUnit != null) {
                    String u = loadedTask.recurrenceUnit.toLowerCase(java.util.Locale.ROOT);
                    spinnerUnit.setSelection(u.startsWith("ned") ? 1 : 0); // 0=Dan, 1=Nedelja
                }
                // vreme u danu – ako ga čuvaš posebno (t.timeOfDay), možeš prikazati na dugmetu
            }
        }, e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private int weightIndexForXP(int xp) {
        switch (xp) {
            case 1: return 0;  // Veoma lak - 1 XP
            case 3: return 1;  // Lak - 3 XP
            case 7: return 2;  // Težak - 7 XP
            case 20: return 3; // Ekstremno težak - 20 XP
            default: return 0;
        }
    }
    private int importanceIndexForXP(int xp) {
        switch (xp) {
            case 1: return 0;   // Normalan - 1 XP
            case 3: return 1;   // Važan - 3 XP
            case 10: return 2;  // Ekstremno važan - 10 XP
            case 100: return 3; // Specijalan - 100 XP
            default: return 0;
        }
    }

}
