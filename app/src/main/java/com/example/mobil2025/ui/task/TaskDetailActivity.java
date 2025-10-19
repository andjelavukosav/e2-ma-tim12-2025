package com.example.mobil2025.ui.task;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobil2025.R;
import com.example.mobil2025.data.repo.TaskRepository;
import com.example.mobil2025.model.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TaskDetailActivity extends AppCompatActivity {

    private TextView tvName, tvDesc, tvCategory, tvType, tvDue, tvRange, tvRule, tvNexts;
    private Spinner spStatus;
    private Button btnSaveStatus, btnMarkDone;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final TaskRepository taskRepo = new TaskRepository();

    private String taskId;
    private Task task;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        taskId = getIntent().getStringExtra("taskId");
        if (taskId == null) {
            Toast.makeText(this, "Nema ID zadatka", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvName = findViewById(R.id.tvName);
        tvDesc = findViewById(R.id.tvDesc);
        tvCategory = findViewById(R.id.tvCategory);
        tvType = findViewById(R.id.tvType);
        tvDue = findViewById(R.id.tvDue);
        tvRange = findViewById(R.id.tvRange);
        tvRule = findViewById(R.id.tvRule);
        tvNexts = findViewById(R.id.tvNexts);
        spStatus = findViewById(R.id.spStatus);
        btnSaveStatus = findViewById(R.id.btnSaveStatus);
        btnMarkDone = findViewById(R.id.btnMarkDone);

        // status opcije
        List<String> statuses = Arrays.asList("active","done","paused","canceled");
        ArrayAdapter<String> stAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statuses);
        stAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatus.setAdapter(stAdapter);

        loadTask();
        setupActions();
    }

    private void loadTask() {
        db.collection("tasks").document(taskId).get()
                .addOnSuccessListener(this::bindTask)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Greška učitavanja: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void bindTask(DocumentSnapshot snap) {
        task = snap.toObject(Task.class);
        if (task == null) { finish(); return; }

        tvName.setText(nullToEmpty(task.name));
        tvDesc.setText(nullToEmpty(task.description));
        tvCategory.setText(nullToEmpty(task.categoryId)); // po želji: lookup naziva kategorije
        tvType.setText(task.recurring ? "Ponavljajući" : "Jednokratni");

        if (!task.recurring) {
            findViewById(R.id.boxSingle).setVisibility(android.view.View.VISIBLE);
            findViewById(R.id.boxRecurring).setVisibility(android.view.View.GONE);
            tvDue.setText(formatDateTime(task.dueTime, task.tz));
        } else {
            findViewById(R.id.boxSingle).setVisibility(android.view.View.GONE);
            findViewById(R.id.boxRecurring).setVisibility(android.view.View.VISIBLE);

            String start = formatDate(task.startDate, task.tz);
            String end = (task.endDate > 0) ? formatDate(task.endDate, task.tz) : "bez kraja";
            tvRange.setText(start + " — " + end);

            boolean isWeek = isWeekly(task.recurrenceUnit);
            String rule = (task.recurrenceInterval <= 1)
                    ? (isWeek ? "svake nedelje" : "svakog dana")
                    : String.format(Locale.getDefault(),"svakih %d %s",
                    task.recurrenceInterval, isWeek ? "nedelje" : "dana");
            tvRule.setText(rule + " u " + (task.timeOfDay != null ? task.timeOfDay : "09:00"));

            // prikaži sledećih par termina (on-the-fly)
            java.util.List<Long> ups = RecurrenceUtils.upcomingOccurrences(task, 5);
            if (ups.isEmpty()) tvNexts.setText("Nema budućih pojava");
            else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < ups.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(formatDateTime(ups.get(i), task.tz));
                }
                tvNexts.setText(sb.toString());
            }
        }

        // status
        String st = (task.status == null || task.status.isEmpty()) ? "active" : task.status;
        int pos = ((ArrayAdapter<String>) spStatus.getAdapter()).getPosition(st);
        if (pos >= 0) spStatus.setSelection(pos);
    }

    private void setupActions() {
        btnSaveStatus.setOnClickListener(v -> {
            String newStatus = (String) spStatus.getSelectedItem();
            if (task == null) return;
            taskRepo.updateTaskStatus(task.id, newStatus,
                    a -> Toast.makeText(this, "Status sačuvan", Toast.LENGTH_SHORT).show(),
                    e -> Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        btnMarkDone.setOnClickListener(v -> {
            if (task == null) return;
            taskRepo.updateTaskStatus(task.id, "done",
                    a -> {
                        spStatus.setSelection(((ArrayAdapter<String>) spStatus.getAdapter()).getPosition("done"));
                        Toast.makeText(this, "Označeno kao urađeno", Toast.LENGTH_SHORT).show();
                    },
                    e -> Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }

    // ——— helpers ———
    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static String formatDateTime(long millis, String tz) {
        SimpleDateFormat f = new SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault());
        if (tz != null && !tz.isEmpty()) f.setTimeZone(TimeZone.getTimeZone(tz));
        return f.format(new java.util.Date(millis));
    }
    private static String formatDate(long millis, String tz) {
        SimpleDateFormat f = new SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault());
        if (tz != null && !tz.isEmpty()) f.setTimeZone(TimeZone.getTimeZone(tz));
        return f.format(new java.util.Date(millis));
    }
    private static boolean isWeekly(String unitRaw) {
        if (unitRaw == null) return false;
        String u = unitRaw.toLowerCase(Locale.ROOT).trim();
        return u.equals("week") || u.equals("weeks") || u.equals("weekly")
                || u.equals("nedelja") || u.equals("nedelje") || u.equals("nedeljno");
    }
}
