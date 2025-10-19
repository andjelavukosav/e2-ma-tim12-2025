package com.example.mobil2025.data.repo;

import androidx.annotation.NonNull;

import com.example.mobil2025.model.Task;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Duration;

public class TaskRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Kreiranje (single ili recurring) — bez materializacije svih pojava */
    public void createTask(@NonNull Task task,
                           @NonNull OnSuccessListener<Void> ok,
                           @NonNull OnFailureListener err) {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { err.onFailure(new IllegalStateException("Not signed in")); return; }

        long now = System.currentTimeMillis();
        task.ownerUid = uid;
        task.createdAt = now;
        task.updatedAt = now;

        if (task.recurring) {
            // sanity defaults
            if (task.tz == null || task.tz.isEmpty()) task.tz = TimeZone.getDefault().getID();
            if (task.timeOfDay == null || task.timeOfDay.isEmpty()) task.timeOfDay = "09:00";
            task.nextDueAt = computeNextOccurrenceMillis(task, now);
        } else {
            // za single možeš (opciono) izjednačiti nextDueAt radi unifikovanog upita
            task.nextDueAt = task.dueTime;
        }

        String id = db.collection("tasks").document().getId();
        task.id = id;
        db.collection("tasks").document(id)
                .set(task)
                .addOnSuccessListener(ok)
                .addOnFailureListener(err);
    }

    /** Označi jednu pojavu kao završenu — pomeri nextDueAt na sledeću (ili -1) */
    public void completeOccurrence(@NonNull Task task,
                                   @NonNull OnSuccessListener<Void> ok,
                                   @NonNull OnFailureListener err) {

        long now = System.currentTimeMillis();
        Long next = task.recurring ? computeNextOccurrenceMillis(task, now) : -1L;

        db.collection("tasks").document(task.id)
                .update("nextDueAt", next,
                        "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(ok)
                .addOnFailureListener(err);
    }

    /** Real-time slušanje svih taskova korisnika (po potrebi filtriraj na klijentu) */
    public void listenTasksForUser(String uid, EventListener<QuerySnapshot> listener) {
        db.collection("tasks")
                .whereEqualTo("ownerUid", uid)
                .addSnapshotListener(listener);
    }

    public ListenerRegistration listenTasksForUsers(String uid, EventListener<QuerySnapshot> listener) {
        return db.collection("tasks")
                .whereEqualTo("ownerUid", uid)
                .addSnapshotListener(listener);
    }
    /** Brzi upit: "šta je danas" (koristi denormalizovani nextDueAt prozor) */
    public void listenToday(String uid, long dayStartUtc, long dayEndUtc,
                            EventListener<QuerySnapshot> listener) {
        // Pošto single ima nextDueAt = dueTime, ovaj upit lovi oba tipa.
        db.collection("tasks")
                .whereEqualTo("ownerUid", uid)
                .whereGreaterThanOrEqualTo("nextDueAt", dayStartUtc)
                .whereLessThan("nextDueAt", dayEndUtc)
                .addSnapshotListener(listener);
    }

    // ============ Recurrence helperi (Calendar varijanta, bez java.time) ============

    /**
     * Računa sledeću pojavu na/posle "nowMillis".
     * Vraća epoch millis ili -1L ako više nema pojava (posle endDate).
     *
     * Oslanjamo se na:
     * - startDate: UTC millis na 00:00 startnog datuma
     * - endDate:   može biti null
     * - timeOfDay: "HH:mm" u lokalnoj zoni "tz"
     * - tz:        npr. "Europe/Belgrade"
     */
    public static long computeNextOccurrenceMillis(Task t, long nowMillis) {
        if (!t.recurring) return -1L;

        ZoneId zone = ZoneId.of(t.tz == null || t.tz.isEmpty() ? TimeZone.getDefault().getID() : t.tz);
        LocalTime tod = parseTime(t.timeOfDay);
        LocalDate start = toLocalDate(t.startDate, zone);
        LocalDate end   = (t.endDate == null) ? LocalDate.of(9999,1,1) : toLocalDate(t.endDate, zone);

        ZonedDateTime now = Instant.ofEpochMilli(nowMillis).atZone(zone);
        ZonedDateTime first = ZonedDateTime.of(start, tod, zone);
        if (now.isBefore(first)) return first.toInstant().toEpochMilli();

        int step = Math.max(1, t.recurrenceInterval);
        boolean byWeek = "week".equalsIgnoreCase(t.recurrenceUnit);

        // koliki je „skok“ do sledeće pojave POSLE now
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


    private static boolean isAfterEnd(Calendar cand, Long endDateUtcMidnight, TimeZone zone, int[] hm) {
        Calendar end = Calendar.getInstance(zone, Locale.getDefault());
        end.setTimeInMillis(endDateUtcMidnight);
        setTimeOfDay(end, hm[0], hm[1]); // upoređujemo na istom "timeOfDay"
        return cand.after(end);
    }

    private static void setTimeOfDay(Calendar cal, int h, int m) {
        cal.set(Calendar.HOUR_OF_DAY, h);
        cal.set(Calendar.MINUTE, m);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private static TimeZone safeTz(String tz) {
        if (tz == null || tz.isEmpty()) return TimeZone.getDefault();
        return TimeZone.getTimeZone(tz);
    }

    /** Parsira "HH:mm" → [hour, minute] sa fallback-om na 09:00 */
    private static int[] parseHm(String timeOfDay) {
        try {
            if (timeOfDay != null) {
                String[] p = timeOfDay.split(":");
                int h = Integer.parseInt(p[0]);
                int m = Integer.parseInt(p[1]);
                if (h >= 0 && h < 24 && m >= 0 && m < 60) return new int[]{h, m};
            }
        } catch (Exception ignored) {}
        return new int[]{9, 0};
    }
    // ===================== Helper metode za vreme i datume =====================

    /** Bezbedno parsira string u LocalTime ("HH:mm"), vraća 09:00 ako nije validno */
    private static LocalTime parseTime(String hhmm) {
        try {
            return LocalTime.parse(hhmm);
        } catch (Exception e) {
            return LocalTime.of(9, 0);
        }
    }

    /** Pretvara UTC millis u LocalDate u zadatoj zoni */
    private static LocalDate toLocalDate(long utcMidnightMillis, ZoneId zone) {
        return Instant.ofEpochMilli(utcMidnightMillis).atZone(zone).toLocalDate();
    }

    // NOVO: helper – promjena statusa
    public void updateTaskStatus(String taskId, String status,
                                 OnSuccessListener<Void> ok,
                                 OnFailureListener err) {
        boolean active = "active".equals(status);
        db.collection("tasks").document(taskId)
                .update("status", status, "active", active)
                .addOnSuccessListener(ok)
                .addOnFailureListener(err);
    }
}
