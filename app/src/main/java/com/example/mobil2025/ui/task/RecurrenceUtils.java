// LOKACIJA: app/src/main/java/com/example/mobil2025/ui/task/RecurrenceUtils.java
package com.example.mobil2025.ui.task;

import com.example.mobil2025.model.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Računanje sledećih pojava za ponavljajuće zadatke (dnevno ili nedeljno). */
public class RecurrenceUtils {

    // ===================== HELPERS (JEDNOM DEFINISANI) =====================

    /** Da li je jedinica nedelja (prihvata en/sr varijante). */
    private static boolean isWeeklyUnit(String unitRaw) {
        if (unitRaw == null) return false;
        String u = unitRaw.trim().toLowerCase(Locale.ROOT);
        return u.equals("week") || u.equals("weeks") || u.equals("weekly")
                || u.equals("nedelja") || u.equals("nedelje") || u.equals("nedeljno")
                || u.equals("nedelja/i"); // tolerantan unos
    }

    /** TimeZone iz taska ili podrazumevana. */
    private static TimeZone zoneOf(Task t) {
        return (t.tz == null || t.tz.isEmpty())
                ? TimeZone.getDefault()
                : TimeZone.getTimeZone(t.tz);
    }

    /** Postavi HH:mm (ili 09:00 ako nema) na dati kalendar. */
    private static void setTimeOfDay(Calendar cal, String timeOfDay) {
        int h = 9, m = 0;
        try {
            if (timeOfDay != null && !timeOfDay.isEmpty()) {
                String[] p = timeOfDay.split(":");
                h = Integer.parseInt(p[0]);
                m = Integer.parseInt(p[1]);
            }
        } catch (Exception ignored) {}
        cal.set(Calendar.HOUR_OF_DAY, h);
        cal.set(Calendar.MINUTE, m);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    /** Krajnji limit (endDate) poravnat na timeOfDay; ako nema kraja → Long.MAX_VALUE. */
    private static long endLimitMs(Task t, TimeZone zone) {
        if (t.endDate <= 0) return Long.MAX_VALUE;
        Calendar end = Calendar.getInstance(zone, Locale.getDefault());
        end.setTimeInMillis(t.endDate);
        setTimeOfDay(end, t.timeOfDay);
        return end.getTimeInMillis();
    }

    // ===================== JAVNE METODE =====================

    /**
     * Izračunaj sledeću pojavu (>= nowMs) za ponavljajući task.
     * Ako je `t.nextDueAt` validan (>0), vrati njega; inače računaj od startDate/timeOfDay.
     * Ako pređe endDate → vrati null.
     */
    public static Long computeNextDueAt(Task t, long nowMs) {
        if (t == null || !Boolean.TRUE.equals(t.recurring)) return null;

        if (t.nextDueAt != null && t.nextDueAt > 0) return t.nextDueAt;

        TimeZone zone = zoneOf(t);
        boolean weekly = isWeeklyUnit(t.recurrenceUnit);
        int step = Math.max(1, t.recurrenceInterval);

        long startEpoch = (t.startDate > 0 ? t.startDate : nowMs);
        Calendar c = Calendar.getInstance(zone, Locale.getDefault());
        c.setTimeInMillis(startEpoch);
        setTimeOfDay(c, t.timeOfDay);

        long endLimit = endLimitMs(t, zone);
        if (c.getTimeInMillis() > endLimit) return null;

        // “Skrolaj” do prve pojave >= sada
        while (c.getTimeInMillis() < nowMs) {
            if (weekly) c.add(Calendar.WEEK_OF_YEAR, step);
            else        c.add(Calendar.DAY_OF_MONTH, step);
            if (c.getTimeInMillis() > endLimit) return null;
        }
        return c.getTimeInMillis();
    }

    /**
     * Vrati narednih `count` pojava posle sledeće (računate u odnosu na sada).
     * Uključuje samo one koje su <= endDate (ako postoji).
     */
    public static List<Long> upcomingOccurrences(Task t, int count) {
        List<Long> out = new ArrayList<>();
        if (t == null || !Boolean.TRUE.equals(t.recurring) || count <= 0) return out;

        Long next = computeNextDueAt(t, System.currentTimeMillis());
        if (next == null) return out;

        TimeZone zone = zoneOf(t);
        boolean weekly = isWeeklyUnit(t.recurrenceUnit);
        int step = Math.max(1, t.recurrenceInterval);
        long endLimit = endLimitMs(t, zone);

        Calendar c = Calendar.getInstance(zone, Locale.getDefault());
        c.setTimeInMillis(next);

        for (int i = 0; i < count; i++) {
            if (weekly) c.add(Calendar.WEEK_OF_YEAR, step);
            else        c.add(Calendar.DAY_OF_MONTH, step);

            long ts = c.getTimeInMillis();
            if (ts > endLimit) break;
            out.add(ts);
        }
        return out;
    }

    /** Vrati sve buduće pojave (od sada) do kraja rasporeda. */
    public static List<Long> upcomingOccurrencesUntilEnd(Task t) {
        List<Long> out = new ArrayList<>();
        if (t == null || !Boolean.TRUE.equals(t.recurring)) return out;

        Long next = computeNextDueAt(t, System.currentTimeMillis());
        if (next == null) return out;

        TimeZone zone = zoneOf(t);
        boolean weekly = isWeeklyUnit(t.recurrenceUnit);
        int step = Math.max(1, t.recurrenceInterval);
        long endLimit = endLimitMs(t, zone);

        Calendar c = Calendar.getInstance(zone, Locale.getDefault());
        c.setTimeInMillis(next);

        while (true) {
            if (weekly) c.add(Calendar.WEEK_OF_YEAR, step);
            else        c.add(Calendar.DAY_OF_MONTH, step);

            long ts = c.getTimeInMillis();
            if (ts > endLimit) break;
            out.add(ts);
        }
        return out;
    }

    /**
     * Pomoćna varijanta: sledeća pojava od trenutka poziva (“sada”).
     * Za jednokratne vraća dueTime (ako postoji), za ponavljajuće računa od rasporeda.
     */
    public static Long computeNextDueFromNow(Task t) {
        if (t == null) return null;
        if (!Boolean.TRUE.equals(t.recurring)) {
            return (t.dueTime > 0) ? t.dueTime : null;
        }
        TimeZone zone = zoneOf(t);
        Calendar cur = Calendar.getInstance(zone, Locale.getDefault());
        long now = System.currentTimeMillis();

        long start = (t.startDate > 0 ? Math.max(t.startDate, now) : now);
        cur.setTimeInMillis(start);
        setTimeOfDay(cur, t.timeOfDay);

        int step = Math.max(1, t.recurrenceInterval);
        boolean weekly = isWeeklyUnit(t.recurrenceUnit);
        long endLimit = endLimitMs(t, zone);

        while (cur.getTimeInMillis() < now) {
            if (weekly) cur.add(Calendar.WEEK_OF_YEAR, step);
            else        cur.add(Calendar.DAY_OF_MONTH, step);
            if (cur.getTimeInMillis() > endLimit) return null;
        }
        long candidate = cur.getTimeInMillis();
        return (candidate > endLimit) ? null : candidate;
    }
}
