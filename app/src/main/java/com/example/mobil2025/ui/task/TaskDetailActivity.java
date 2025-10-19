package com.example.mobil2025.ui.task;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobil2025.R;
import com.example.mobil2025.data.repo.CategoryRepository;
import com.example.mobil2025.data.repo.OccurrenceRepository;
import com.example.mobil2025.data.repo.TaskRepository;
import com.example.mobil2025.model.Category;
import com.example.mobil2025.model.Occurrence;
import com.example.mobil2025.model.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class TaskDetailActivity extends AppCompatActivity {

    private TextView tvName, tvDesc, tvCategory, tvStatus, tvTiming;
    private Button btnActive, btnDone, btnPaused, btnCanceled, btnEdit, btnDelete;

    private String taskId;
    private Task current;
    private ListenerRegistration reg;

    private final TaskRepository taskRepo = new TaskRepository();
    private final CategoryRepository catRepo = new CategoryRepository();
    private final Map<String, Category> cats = new HashMap<>();

    private final SimpleDateFormat fDate = new SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault());
    private final SimpleDateFormat fDateTime = new SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault());

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        tvName = findViewById(R.id.tvName);
        tvDesc = findViewById(R.id.tvDesc);
        tvCategory = findViewById(R.id.tvCategory);
        tvStatus = findViewById(R.id.tvStatus);
        tvTiming = findViewById(R.id.tvTiming);

        btnActive = findViewById(R.id.btnActive);
        btnDone = findViewById(R.id.btnDone);
        btnPaused = findViewById(R.id.btnPaused);
        btnCanceled = findViewById(R.id.btnCanceled);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        // extras
        taskId = getIntent().getStringExtra("taskId");
        if (taskId == null || taskId.isEmpty()) {
            Toast.makeText(this, "Nedostaje ID zadatka", Toast.LENGTH_LONG).show();
            finish(); return;
        }

        // kategorije (za ime + boju)
        catRepo.getMyCategories(list -> {
            cats.clear();
            for (Category c : list) cats.put(c.id, c);
            listenTask();
        }, e -> listenTask());

        // akcije statusa
        btnActive.setOnClickListener(v -> updateStatus("active"));
        btnDone.setOnClickListener(v -> updateStatus("done"));
        btnPaused.setOnClickListener(v -> updateStatus("paused"));
        btnCanceled.setOnClickListener(v -> updateStatus("canceled"));

        // uredi
        btnEdit.setOnClickListener(v -> {
            Intent it = new Intent(this, CreateTaskActivity.class);
            it.putExtra("editTaskId", taskId);  // tvoja CreateTaskActivity treba da podrži “edit mode”
            startActivity(it);
        });

        // obriši
        btnDelete.setOnClickListener(v -> {
            if ("done".equalsIgnoreCase(current.status) && !Boolean.TRUE.equals(current.recurring)) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Nije dozvoljeno")
                        .setMessage("Završene (jednokratne) zadatke nije moguće obrisati.")
                        .setPositiveButton("OK", null).show();
                return;
            }

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Obriši zadatak?")
                    .setMessage(Boolean.TRUE.equals(current.recurring)
                            ? "Obrisati ćeš raspored (sva buduća ponavljanja). Ranije završene pojave ostaju u kalendaru."
                            : "Obrisati zadatak?")
                    .setPositiveButton("Obriši", (d,w) -> {
                        new TaskRepository().deleteTaskWithRule(current,
                                ok -> { Toast.makeText(this, "Obrisano", Toast.LENGTH_SHORT).show(); finish(); },
                                err -> Toast.makeText(this, "Greška: " + err.getMessage(), Toast.LENGTH_LONG).show());
                    })
                    .setNegativeButton("Otkaži", null)
                    .show();
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (reg != null) reg.remove();
    }

    private void listenTask() {
        reg = FirebaseFirestore.getInstance()
                .collection("tasks").document(taskId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || !snap.exists()) return;
                    current = snap.toObject(Task.class);
                    bind(current);
                });
    }

    private void bind(Task t) {
        if (t == null) return;

        tvName.setText(notNull(t.name));
        tvDesc.setText(notEmpty(t.description) ? t.description : "—");

        // status (možeš obojiti)
        tvStatus.setText("Status: " + notNull(t.status));

        // kategorija + boja
        Category c = t.categoryId != null ? cats.get(t.categoryId) : null;
        tvCategory.setText("Kategorija: " + (c != null ? c.name : "—"));
        // oboji kružić levo (viewCategoryColor) ako želiš – ili samo tekst oboj
        try {
            int color = Color.parseColor(c != null ? c.colorHex : "#607D8B");
            var colorDot = findViewById(R.id.viewCategoryColor);
            if (colorDot != null) {
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(color);
                colorDot.setBackground(bg);
            }
        } catch (Exception ignored) {}

        // vreme / raspored (sekcija 2.1)
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(t.recurring)) {
            sb.append("Tip: Ponavljajući\n");
            sb.append("Početak: ").append(t.startDate > 0 ? fDate.format(new java.util.Date(t.startDate)) : "—").append("\n");
            sb.append("Kraj: ").append(t.endDate > 0 ? fDate.format(new java.util.Date(t.endDate)) : "bez kraja").append("\n");
            sb.append("Interval: ").append(Math.max(1, t.recurrenceInterval)).append(" ");
            sb.append(notNull(t.recurrenceUnit)).append("\n");
            sb.append("Vreme u danu: ").append(notNull(t.timeOfDay)).append("\n");
            sb.append("Zona: ").append(notNull(t.tz));
            if (t.nextDueAt != null && t.nextDueAt > 0) {
                sb.append("\nSledeće: ").append(fDateTime.format(new java.util.Date(t.nextDueAt)));
            }
        } else {
            sb.append("Tip: Jednokratni\n");
            sb.append("Rok: ").append(t.dueTime > 0 ? fDateTime.format(new java.util.Date(t.dueTime)) : "—");
        }
        tvTiming.setText(sb.toString());
        updateButtonsVisibility(t);

    }

    private void updateStatus(String status) {
        if (current == null || current.id == null) return;
        if (!canChangeStatus(status)) return;

        if ("done".equals(status) && Boolean.TRUE.equals(current.recurring)) {
            long when = getIntent().getLongExtra("occurrenceAt", 0L);
            if (when <= 0) {
                Toast.makeText(this, "Nedostaje datum pojave (otvori iz kalendara).", Toast.LENGTH_LONG).show();
                return;
            }
            // 1) Upis occurrence-a
            Occurrence oc = new Occurrence();
            oc.taskId = current.id;
            oc.startAt = when;
            oc.endAt = when + 60 * 60 * 1000; // ako imaš per-task trajanje, zameni
            oc.status = "done";
            oc.name = current.name;
            oc.description = current.description;
            oc.categoryId = current.categoryId;
            oc.categoryColorHex = guessCategoryColor(current.categoryId); // vidi helper ispod
            oc.tz = current.tz;

            new OccurrenceRepository().addOccurrence(oc,
                    ref -> {
                        // 2) Status taska može ostati "active" (jer je to ponavljajući),
                        //    ili po tvojoj logici. Obično se schedule ne gasi.
                        //    Ako želiš da update-uješ status glavnog taska:
                        new TaskRepository().updateTaskStatus(current.id, "active", // ili ostavi kakav jeste
                                v -> Toast.makeText(this, "Pojava zabeležena kao urađena", Toast.LENGTH_SHORT).show(),
                                e -> Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    },
                    e -> Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
            return;
        }

        // Jednokratni ili promene statusa koje nisu "done"…
        new TaskRepository().updateTaskStatus(current.id, status,
                v -> Toast.makeText(this, "Status ažuriran", Toast.LENGTH_SHORT).show(),
                e -> Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private String guessCategoryColor(String categoryId) {
        Category c = /* ako u detalju imaš mapu id->Category */ null;
        // ili po potrebi vrati default:
        return "#607D8B";
    }

    private boolean canChangeStatus(String newStatus) {
        if (current == null) return false;

        long now = System.currentTimeMillis();

        // ⏱ Relevantno vreme za proveru:
        // - jednokratni zadatak -> dueTime
        // - ponavljajući zadatak -> datum pojedinačne pojave (occurrenceAt)
        long relevantTime;
        if (Boolean.TRUE.equals(current.recurring)) {
            relevantTime = getIntent().getLongExtra("occurrenceAt", 0L);
            if (relevantTime <= 0L) {
                Toast.makeText(this, "Nedostaje datum pojave (otvori iz kalendara).", Toast.LENGTH_LONG).show();
                return false;
            }
        } else {
            relevantTime = current.dueTime != null ? current.dueTime : 0L;
        }

        // ⏰ Zadaci stariji od 3 dana postaju neurađeni (samo aktivni)
        if ("active".equals(current.status) && relevantTime > 0) {
            long threeDaysAgo = now - (3 * 24 * 60 * 60 * 1000);
            if (relevantTime < threeDaysAgo) {
                new TaskRepository().updateTaskStatus(current.id, "not_done",
                        v -> Toast.makeText(this, "Zadatak automatski označen kao neurađen", Toast.LENGTH_SHORT).show(),
                        e -> {});
                return false;
            }
        }

        // ❌ Neurađeni i otkazani se ne mogu menjati
        if ("not_done".equals(current.status) || "canceled".equals(current.status)) {
            Toast.makeText(this, "Ovaj zadatak se ne može više menjati.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 🟢 Samo aktivan ili pauziran može biti menjan
        if (!"active".equals(current.status) && !"paused".equals(current.status)) {
            Toast.makeText(this, "Samo aktivan ili pauziran zadatak se može menjati.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // ⏳ Zadatak se može označiti kao urađen tek nakon isteka vremena izvršenja
        if ("done".equals(newStatus) && relevantTime > now) {
            Toast.makeText(this, "Zadatak se može označiti kao urađen tek nakon isteka vremena izvršenja.", Toast.LENGTH_LONG).show();
            return false;
        }

        // ⏸ Pauziranje samo za ponavljajuće zadatke
        if ("paused".equals(newStatus) && !Boolean.TRUE.equals(current.recurring)) {
            Toast.makeText(this, "Samo ponavljajući zadaci mogu biti pauzirani.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }



    private void updateButtonsVisibility(Task t) {
        btnActive.setEnabled(false);
        btnDone.setEnabled(false);
        btnPaused.setEnabled(false);
        btnCanceled.setEnabled(false);

        if ("active".equals(t.status)) {
            btnDone.setEnabled(true);
            btnCanceled.setEnabled(true);
            if (Boolean.TRUE.equals(t.recurring)) btnPaused.setEnabled(true);
        } else if ("paused".equals(t.status)) {
            btnActive.setEnabled(true); // dozvoli ponovno aktiviranje
        }
    }


    private static String notNull(String s) { return s != null ? s : ""; }
    private static boolean notEmpty(String s) { return s != null && !s.trim().isEmpty(); }
}
