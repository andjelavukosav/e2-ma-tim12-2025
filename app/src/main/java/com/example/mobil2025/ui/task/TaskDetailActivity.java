package com.example.mobil2025.ui.task;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobil2025.R;
import com.example.mobil2025.data.repo.CategoryRepository;
import com.example.mobil2025.data.repo.TaskRepository;
import com.example.mobil2025.model.Category;
import com.example.mobil2025.model.OccurrenceInterval;
import com.example.mobil2025.model.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class TaskDetailActivity extends AppCompatActivity {

    private TextView tvName, tvDesc, tvCategory, tvStatus, tvTiming;
    private Button btnActive, btnDone, btnPaused, btnCanceled, btnEdit, btnDelete;
    private TextView tvXP;

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
        tvXP = findViewById(R.id.tvXP);

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
        FirebaseFirestore.getInstance()
                .collection("tasks").document(taskId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) return;

                    if (snap != null && snap.exists()) {
                        // Task je pronađen (živ)
                        current = snap.toObject(Task.class);
                        // intervali već postoje u dokumentu, ne generišemo ništa
                        // samo ih koristimo u updateStatus()
                        bind(current);

                    } else {
                        // Task ne postoji
                        Toast.makeText(this, "Task nije pronađen", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }


    private void bind(Task t) {
        if (t == null) return;

        tvName.setText(notNull(t.name));
        tvDesc.setText(notEmpty(t.description) ? t.description : "—");

        // status (možeš obojiti)
        String statusText = Boolean.TRUE.equals(t.recurring)
                ? getIntervalStatusForCurrentOccurrence()
                : notNull(t.status);

        tvStatus.setText("Status: " + statusText);

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
        String xpText = "⭐ Težina: " + t.weightXP + " XP\n" +
                "🔥 Bitnost: " + t.importanceXP + " XP\n" +
                "💎 Ukupno: " + t.totalXP + " XP";
        tvXP.setText(xpText);
        tvXP.setTextColor(Color.parseColor("#4CAF50")); // svetlo zelena

    }

    private void updateStatus(String newStatus) {
        if (current == null || current.id == null) return;

        long now = System.currentTimeMillis();
        long threeDaysAgo = now - 3L * 24 * 60 * 60 * 1000;
        long occurrenceAt = getIntent().getLongExtra("occurrenceAt", 0L);

        // 🔹 Ako pokušavaš da označiš kao "done", a vreme još nije prošlo → zabrani
        long relevantTime = 0L;
        if (Boolean.TRUE.equals(current.recurring)) {
            relevantTime = occurrenceAt;
        } else if (current.dueTime != null) {
            relevantTime = current.dueTime;
        }

        if ("done".equals(newStatus) && relevantTime > now) {
            Toast.makeText(this, "Zadatak se može označiti kao urađen tek nakon isteka vremena izvršenja.", Toast.LENGTH_LONG).show();
            return;
        }

        // 🔹 Ponavljajući zadaci (sa intervalima)
        if (Boolean.TRUE.equals(current.recurring) && current.intervals != null && occurrenceAt > 0) {

            boolean intervalFound = false;

            for (OccurrenceInterval interval : current.intervals) {

                // ⏳ automatski označi istekle intervale kao not_done
                if ("active".equalsIgnoreCase(interval.status) && interval.date < threeDaysAgo) {
                    interval.status = "not_done";
                }

                // 🟢 ako je trenutni interval, ažuriraj njegov status
                if (isSameDay(interval.date, occurrenceAt)) {
                    intervalFound = true;

                    // ako pokušavaš da označiš kao urađen pre vremena, spreči
                    if ("done".equals(newStatus) && interval.date > now) {
                        Toast.makeText(this, "Ova pojava još nije završena — ne može biti označena kao urađena.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (!"not_done".equalsIgnoreCase(interval.status)) {
                        interval.status = newStatus;
                    }
                }
            }

            if (!intervalFound) {
                Toast.makeText(this, "Interval za ovu pojavu nije pronađen.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔸 Update Firestore-a
            Map<String, Object> updates = new HashMap<>();
            updates.put("intervals", current.intervals);

            Long nextDue = current.intervals.stream()
                    .filter(i -> "active".equalsIgnoreCase(i.status))
                    .map(i -> i.date)
                    .min(Long::compareTo)
                    .orElse(-1L);
            updates.put("nextDueAt", nextDue);
            updates.put("updatedAt", now);

            taskRepo.updateTaskFields(current.id, updates,
                    v -> {
                        Toast.makeText(this, "Status intervala ažuriran.", Toast.LENGTH_SHORT).show();
                        bind(current);
                    },
                    e -> Toast.makeText(this, "Greška pri update-u intervala: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
            return;
        }

        // 🔹 Jednokratni zadaci
        if ("active".equals(current.status) && relevantTime > 0 && relevantTime < threeDaysAgo) {
            new TaskRepository().updateTaskStatus(current.id, "not_done",
                    v -> {
                        Toast.makeText(this, "Zadatak je istekao i označen kao neurađen.", Toast.LENGTH_SHORT).show();
                        bind(current);
                    },
                    e -> Toast.makeText(this, "Greška pri update-u zadatka: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
            return;
        }

        // 🔹 Normalno menjanje statusa
        taskRepo.updateTaskStatus(current.id, newStatus,
                v -> {
                    Toast.makeText(this, "Status zadatka ažuriran", Toast.LENGTH_SHORT).show();
                    bind(current);
                },
                e -> Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    private boolean isSameDay(long t1, long t2) {
        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(t1);
        Calendar c2 = Calendar.getInstance();
        c2.setTimeInMillis(t2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
                && c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH);
    }



    private boolean canChangeStatus(String newStatus) {
        if (current == null) return false;

        long now = System.currentTimeMillis();
        long threeDaysAgo = now - (3L * 24 * 60 * 60 * 1000);

        long relevantTime;

        if (Boolean.TRUE.equals(current.recurring)) {
            // Za ponavljajuće zadatke uzimamo datum trenutnog intervala
            relevantTime = getIntent().getLongExtra("occurrenceAt", 0L);
            if (relevantTime <= 0L) {
                Toast.makeText(this, "Nedostaje datum pojave (otvori iz kalendara).", Toast.LENGTH_LONG).show();
                return false;
            }

            // Provera da li je interval istekao
            if (relevantTime < threeDaysAgo) {
                // Update samo intervala ili celog taska sa starim intervalima
                if (Boolean.TRUE.equals(current.recurring)) {
                    // Recurring zadaci: update intervala
                    if (current.intervals != null) {
                        for (OccurrenceInterval interval : current.intervals) {
                            if (interval.date == relevantTime && "active".equalsIgnoreCase(interval.status)) {
                                interval.status = "not_done";
                            }
                        }

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("intervals", current.intervals);

                        new TaskRepository().updateTaskFields(current.id, updates,
                                v -> Toast.makeText(this, "Ovaj interval je istekao i označen kao neurađen.", Toast.LENGTH_SHORT).show(),
                                e -> Toast.makeText(this, "Greška pri update-u intervala.", Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    // Jednokratni zadaci
                    new TaskRepository().updateTaskStatus(current.id, "not_done",
                            v -> Toast.makeText(this, "Zadatak je istekao i označen kao neurađen.", Toast.LENGTH_SHORT).show(),
                            e -> Toast.makeText(this, "Greška pri update-u zadatka.", Toast.LENGTH_SHORT).show()
                    );
                }

                return false; // onemogući dalju promenu jer je već automatski označen
            }


        } else {
            // Jednokratni zadatak
            relevantTime = current.dueTime != null ? current.dueTime : 0L;

            if ("active".equals(current.status) && relevantTime > 0 && relevantTime < threeDaysAgo) {
                Toast.makeText(this, "Zadatak je istekao i automatski se smatra neurađenim.", Toast.LENGTH_SHORT).show();
                return false; // blokira promenu, ne menja status u bazi
            }
        }

        // Neurađeni i otkazani zadaci se ne mogu menjati
        if ("not_done".equals(current.status) || "canceled".equals(current.status)) {
            Toast.makeText(this, "Ovaj zadatak se ne može više menjati.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Samo aktivan ili pauziran može biti menjan
        if (!"active".equals(current.status) && !"paused".equals(current.status)) {
            Toast.makeText(this, "Samo aktivan ili pauziran zadatak se može menjati.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Zadatak se može označiti kao urađen tek nakon isteka vremena izvršenja
        if ("done".equals(newStatus) && relevantTime > now) {
            Toast.makeText(this, "Zadatak se može označiti kao urađen tek nakon isteka vremena izvršenja.", Toast.LENGTH_LONG).show();
            return false;
        }

        // Pauziranje samo za ponavljajuće zadatke
        if ("paused".equals(newStatus) && !Boolean.TRUE.equals(current.recurring)) {
            Toast.makeText(this, "Samo ponavljajući zadaci mogu biti pauzirani.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void updateButtonsVisibility(Task t) {
        String status = Boolean.TRUE.equals(t.recurring) ? getIntervalStatusForCurrentOccurrence() : notNull(t.status);

        btnActive.setEnabled(false);
        btnDone.setEnabled(false);
        btnPaused.setEnabled(false);
        btnCanceled.setEnabled(false);

        if ("active".equals(status)) {
            btnDone.setEnabled(true);
            btnCanceled.setEnabled(true);
            if (Boolean.TRUE.equals(t.recurring)) btnPaused.setEnabled(true);
        } else if ("paused".equals(status)) {
            btnActive.setEnabled(true); // dozvoli ponovno aktiviranje
        }

    }

    private String getIntervalStatusForCurrentOccurrence() {
        if (current == null || !Boolean.TRUE.equals(current.recurring) || current.intervals == null)
            return current != null ? notNull(current.status) : "—";

        long occurrenceAt = getIntent().getLongExtra("occurrenceAt", 0L);
        if (occurrenceAt <= 0) return notNull(current.status);

        for (OccurrenceInterval interval : current.intervals) {
            if (isSameDay(interval.date, occurrenceAt)) {
                return notNull(interval.status);
            }
        }

        return notNull(current.status); // fallback
    }


    private static String notNull(String s) { return s != null ? s : ""; }
    private static boolean notEmpty(String s) { return s != null && !s.trim().isEmpty(); }
}
