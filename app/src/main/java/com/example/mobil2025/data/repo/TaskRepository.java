package com.example.mobil2025.data.repo;

import androidx.annotation.NonNull;

import com.example.mobil2025.model.OccurrenceInterval;
import com.example.mobil2025.model.Task;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Kreiranje (single ili recurring) — bez materializacije svih pojava */
    public void createTask(@NonNull Task task,
                           @NonNull OnSuccessListener<Void> ok,
                           @NonNull OnFailureListener err) {

        // Izračunaj ukupnu XP vrednost
        task.totalXP = (task.weightXP > 0 ? task.weightXP : 0) +
                (task.importanceXP > 0 ? task.importanceXP : 0);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { err.onFailure(new IllegalStateException("Not signed in")); return; }

        long now = System.currentTimeMillis();
        task.ownerUid = uid;
        task.createdAt = now;
        task.updatedAt = now;
        task.status = (task.status == null || task.status.isEmpty()) ? "active" : task.status;

        // Ako je recurring, popuni intervale
        if (Boolean.TRUE.equals(task.recurring)) {
            if (task.tz == null || task.tz.isEmpty()) task.tz = TimeZone.getDefault().getID();
            if (task.timeOfDay == null || task.timeOfDay.isEmpty()) task.timeOfDay = "09:00";

            task.intervals = new ArrayList<>();
            long next = computeNextOccurrenceMillis(task, task.startDate != null ? task.startDate : now);

            while (next != -1 && (task.endDate == null || next <= task.endDate)) {
                task.intervals.add(new OccurrenceInterval(next, "active"));
                next = computeNextOccurrenceMillis(task, next + 1000); // sledeća pojava
            }

            task.nextDueAt = task.intervals.isEmpty() ? -1L : task.intervals.get(0).date;

        } else {
            // Jednokratni zadatak
            task.nextDueAt = task.dueTime;
        }

        String id = db.collection("tasks").document().getId();
        task.id = id;

        db.collection("tasks").document(id)
                .set(task)
                .addOnSuccessListener(ok)
                .addOnFailureListener(err);
    }


    /** Isto kao gore, samo vraća ListenerRegistration */
    public ListenerRegistration listenTasksForUsers(@NonNull String uid,
                                                    @NonNull EventListener<QuerySnapshot> listener) {
        return db.collection("tasks")
                .whereEqualTo("ownerUid", uid) // ⬅️ obavezno
                .addSnapshotListener(listener);
    }


    // ============ Recurrence helperi (java.time) ============

    public static long computeNextOccurrenceMillis(@NonNull Task t, long nowMillis) {
        if (!Boolean.TRUE.equals(t.recurring)) return -1L;

        ZoneId zone = ZoneId.of((t.tz == null || t.tz.isEmpty()) ? TimeZone.getDefault().getID() : t.tz);
        LocalTime tod = parseTime(t.timeOfDay);
        LocalDate start = toLocalDate(t.startDate, zone);
        LocalDate end   = (t.endDate == null) ? LocalDate.of(9999,1,1) : toLocalDate(t.endDate, zone);

        ZonedDateTime now = Instant.ofEpochMilli(nowMillis).atZone(zone);
        ZonedDateTime first = ZonedDateTime.of(start, tod, zone);
        if (now.isBefore(first)) return first.toInstant().toEpochMilli();

        int step = Math.max(1, t.recurrenceInterval == null ? 1 : t.recurrenceInterval);
        boolean byWeek = "week".equalsIgnoreCase(t.recurrenceUnit);

        if (byWeek) {
            long weeks = Duration.between(first, now).toDays() / 7;
            long jumps = (weeks / step) + 1;
            ZonedDateTime cand = first.plusWeeks(jumps * step);
            if (cand.toLocalDate().isAfter(end)) return -1L;
            return cand.toInstant().toEpochMilli();
        } else {
            long days = Duration.between(first, now).toDays();
            long jumps = (days / step) + 1;
            ZonedDateTime cand = first.plusDays(jumps * step);
            if (cand.toLocalDate().isAfter(end)) return -1L;
            return cand.toInstant().toEpochMilli();
        }
    }

    private static LocalTime parseTime(String hhmm) {
        try { return LocalTime.parse(hhmm); }
        catch (Exception e) { return LocalTime.of(9, 0); }
    }

    private static LocalDate toLocalDate(long utcMidnightMillis, ZoneId zone) {
        return Instant.ofEpochMilli(utcMidnightMillis).atZone(zone).toLocalDate();
    }

    // ===================== Ostale operacije =====================

    /** Promjena statusa + updatedAt (korisno za UI) */
    public void updateTaskStatus(@NonNull String taskId, @NonNull String status,
                                 @NonNull OnSuccessListener<Void> ok,
                                 @NonNull OnFailureListener err) {
        boolean active = "active".equals(status);
        db.collection("tasks").document(taskId)
                .update("status", status, "active", active, "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(ok)
                .addOnFailureListener(err);
    }

    public void deleteTask(@NonNull String taskId,
                           @NonNull OnSuccessListener<Void> ok,
                           @NonNull OnFailureListener err) {
        db.collection("tasks").document(taskId)
                .delete()
                .addOnSuccessListener(ok)
                .addOnFailureListener(err);
    }

    /** Brisanje taska uz čišćenje budućih occurrences — dodaj ownerUid filter na query! */
    /*public void deleteTaskWithRule(@NonNull Task t,
                                   @NonNull OnSuccessListener<Void> ok,
                                   @NonNull OnFailureListener err) {
        if (t.id == null) { err.onFailure(new IllegalArgumentException("No task")); return; }

        if (!Boolean.TRUE.equals(t.recurring) && "done".equalsIgnoreCase(t.status)) {
            err.onFailure(new IllegalStateException("Cannot delete finished one-time task"));
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { err.onFailure(new IllegalStateException("Not signed in")); return; }

        if (Boolean.TRUE.equals(t.recurring)) {
            db.collection("occurrences")
                    .whereEqualTo("ownerUid", uid)
                    .whereEqualTo("taskId", t.id)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        long now = System.currentTimeMillis();
                        snapshot.getDocuments().forEach(doc -> {
                            Long startAt = doc.getLong("startAt");
                            if (startAt != null && startAt > now) {
                                doc.getReference().delete();
                            }
                        });
                        db.collection("tasks").document(t.id)
                                .delete()
                                .addOnSuccessListener(ok)
                                .addOnFailureListener(err);
                    })
                    .addOnFailureListener(err);

        } else {
            db.collection("tasks").document(t.id)
                    .delete()
                    .addOnSuccessListener(ok)
                    .addOnFailureListener(err);
        }
    }
*/
    /** Brisanje taska uz čišćenje intervala koji nisu 'done' */
    public void deleteTaskWithRule(@NonNull Task t,
                                   @NonNull OnSuccessListener<Void> ok,
                                   @NonNull OnFailureListener err) {
        if (t.id == null) {
            err.onFailure(new IllegalArgumentException("No task"));
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            err.onFailure(new IllegalStateException("Not signed in"));
            return;
        }

        if (!Boolean.TRUE.equals(t.recurring) && "done".equalsIgnoreCase(t.status)) {
            err.onFailure(new IllegalStateException("Cannot delete finished one-time task"));
            return;
        }

        if (Boolean.TRUE.equals(t.recurring)) {
            AtomicBoolean anyActiveLeft = new AtomicBoolean(false);
            long now = System.currentTimeMillis();

            if (t.intervals != null) {
                t.intervals.removeIf(interval -> {
                    if (!"done".equalsIgnoreCase(interval.status)) {
                        return true; // ukloni interval
                    }
                    if ("active".equalsIgnoreCase(interval.status)) anyActiveLeft.set(true);
                    return false;
                });
            }

            if (!anyActiveLeft.get() && (t.intervals == null || t.intervals.isEmpty())) {
                // Ako više nema aktivnih intervala, briši ceo task
                db.collection("tasks").document(t.id)
                        .delete()
                        .addOnSuccessListener(ok)
                        .addOnFailureListener(err);
            } else {
                // Samo ažuriraj listu intervala i nextDueAt
                Long nextDue = t.intervals.stream()
                        .filter(i -> "active".equalsIgnoreCase(i.status))
                        .map(i -> i.date)
                        .min(Long::compareTo)
                        .orElse(-1L);

                t.nextDueAt = nextDue;
                db.collection("tasks").document(t.id)
                        .update("intervals", t.intervals, "nextDueAt", t.nextDueAt, "updatedAt", System.currentTimeMillis())
                        .addOnSuccessListener(ok)
                        .addOnFailureListener(err);
            }

        } else {
            // Jednokratni ili recurring koji nemaju intervale
            db.collection("tasks").document(t.id)
                    .delete()
                    .addOnSuccessListener(ok)
                    .addOnFailureListener(err);
        }
    }

    public void getTaskById(@NonNull String taskId,
                            @NonNull OnSuccessListener<DocumentSnapshot> ok,
                            @NonNull OnFailureListener err) {
        db.collection("tasks").document(taskId)
                .get()
                .addOnSuccessListener(ok)
                .addOnFailureListener(err);
    }

    /** Parcijalni update (samo prosleđena polja) — održava updatedAt */
    public void updateTaskFields(@NonNull String taskId, @NonNull Map<String, Object> updates,
                                 @NonNull OnSuccessListener<Void> ok,
                                 @NonNull OnFailureListener err) {
        updates.put("updatedAt", System.currentTimeMillis());
        db.collection("tasks").document(taskId)
                .update(updates)
                .addOnSuccessListener(ok)
                .addOnFailureListener(err);
    }
}
