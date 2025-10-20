package com.example.mobil2025.util;

import android.util.Log;

import com.example.mobil2025.data.repo.TaskRepository;
import com.example.mobil2025.model.OccurrenceInterval;
import com.example.mobil2025.model.Task;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TaskStatusUpdater {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final TaskRepository repo = new TaskRepository();

    // Poziva se da označi intervale starije od 3 dana kao "not_done"
    public void markExpiredTasksAsNotDone() {
        long now = System.currentTimeMillis();
        long threeDaysAgo = now - (3L * 24 * 60 * 60 * 1000); // 3 dana u ms

        db.collection("tasks")
                .get()
                .addOnSuccessListener(query -> {
                    for (var doc : query.getDocuments()) {
                        Task t = doc.toObject(Task.class);
                        if (t == null) continue;

                        // Jednokratni zadaci
                        if (!Boolean.TRUE.equals(t.recurring)) {
                            long dueTime = t.dueTime != null ? t.dueTime : t.endDate != null ? t.endDate : 0L;
                            if (dueTime > 0 && dueTime < threeDaysAgo && "active".equalsIgnoreCase(t.status)) {
                                repo.updateTaskStatus(t.id, "not_done",
                                        v -> Log.d("TaskStatusUpdater", "Jednokratni task " + t.name + " označen kao neurađen"),
                                        e -> Log.e("TaskStatusUpdater", e.getMessage()));
                            }
                            continue;
                        }

                        // Ponavljajući zadaci
                        if (t.intervals != null && !t.intervals.isEmpty()) {
                            boolean updated = false;

                            for (OccurrenceInterval interval : t.intervals) {
                                if ("active".equalsIgnoreCase(interval.status) && interval.date < threeDaysAgo) {
                                    interval.status = "not_done";
                                    updated = true;
                                    Log.d("TaskStatusUpdater", "Interval " + interval.date + " taska " + t.name + " označen kao neurađen");
                                }
                            }

                            if (updated) {
                                // Izračunaj sledeću aktivnu pojavu
                                Long nextDue = t.intervals.stream()
                                        .filter(i -> "active".equalsIgnoreCase(i.status))
                                        .map(i -> i.date)
                                        .min(Long::compareTo)
                                        .orElse(-1L);

                                Map<String, Object> updates = new HashMap<>();
                                updates.put("intervals", t.intervals);
                                updates.put("nextDueAt", nextDue); // status celog zadatka se ne dira

                                repo.updateTaskFields(t.id, updates,
                                        v -> Log.d("TaskStatusUpdater", "Intervali taska " + t.name + " ažurirani u bazi"),
                                        e -> Log.e("TaskStatusUpdater", "Greška pri update-u intervala: " + e.getMessage()));
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("TaskStatusUpdater", "Greška pri čitanju: " + e.getMessage()));
    }
}
