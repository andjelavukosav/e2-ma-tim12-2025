package com.example.mobil2025.util;

import android.util.Log;

import com.example.mobil2025.data.repo.TaskRepository;
import com.example.mobil2025.model.Task;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class TaskStatusUpdater {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final TaskRepository repo = new TaskRepository();

    // Poziva se iz aktivnosti da ažurira zadatke starije od 3 dana
    public void markExpiredTasksAsNotDone() {
        long now = System.currentTimeMillis();
        long threeDaysAgo = now - (3 * 24 * 60 * 60 * 1000);

        db.collection("tasks")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(query -> {
                    for (var doc : query.getDocuments()) {
                        Task t = doc.toObject(Task.class);
                        if (t == null) continue;

                        long dueTime = t.dueTime > 0 ? t.dueTime : t.endDate;
                        if (dueTime > 0 && dueTime < threeDaysAgo) {
                            repo.updateTaskStatus(t.id, "not_done",
                                    v -> Log.d("TaskStatusUpdater", "Zadatak " + t.name + " označen kao neurađen"),
                                    e -> Log.e("TaskStatusUpdater", "Greška: " + e.getMessage()));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("TaskStatusUpdater", "Greška pri čitanju: " + e.getMessage()));
    }
}
