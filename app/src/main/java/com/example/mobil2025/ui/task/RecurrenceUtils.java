package com.example.mobil2025.ui.task;

import com.example.mobil2025.model.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Računanje sledeće pojave i narednih pojava za ponavljajuće zadatke. */
public class RecurrenceUtils {

    /** Normalizuje jedinicu (dan/nedelja). Sve nepoznato tretira kao DAN. */
    private static boolean isWeekly(String unitRaw) {
        if (unitRaw == null) return false;
        String u = unitRaw.trim().toLowerCase(Locale.ROOT);
        return u.equals("week") || u.equals("weeks") || u.equals("weekly")
                || u.equals("nedelja") || u.equals("nedelje") || u.equals("nedeljno");
    }

    /** TimeZone iz taska ili podrazumevana. */
    private static TimeZone zoneOf(Task t) {
        return (t.tz == null || t.tz.isEmpty()) ? TimeZone.getDefault()
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

    /**
     * Izračunaj prvu sledeću pojavu >= nowMs.
     * Ako je `t.nextDueAt > 0`, koristi njega; u suprotnom, računaj od startDate (+timeOfDay) i
     * uvećavaj za interval dok ne stigneš do nowMs. Ako postoji endDate i već si ga preskočio → null.
     */
    public static Long computeNextDueAt(Task t, long nowMs) {
        if (t == null || !t.recurring) return null;

        // Ako već postoji validan nextDueAt koristi njega
        if (t.nextDueAt > 0) return t.nextDueAt;

        TimeZone zone = zoneOf(t);
        boolean weekly = isWeekly(t.recurrenceUnit);
        int step = Math.max(1, t.recurrenceInterval);

        // start = startDate (ako je <=0, koristi now) poravnat na timeOfDay
        long startEpoch = (t.startDate > 0 ? t.startDate : nowMs);
        Calendar c = Calendar.getInstance(zone, Locale.getDefault());
        c.setTimeInMillis(startEpoch);
        setTimeOfDay(c, t.timeOfDay);

        long endLimit = endLimitMs(t, zone);
        if (c.getTimeInMillis() > endLimit) return null; // start već posle kraja

        // Ako je start u prošlosti, doteraj do prve pojave >= sada
        while (c.getTimeInMillis() < nowMs) {
            if (weekly) c.add(Calendar.WEEK_OF_YEAR, step);
            else        c.add(Calendar.DAY_OF_MONTH, step);
            if (c.getTimeInMillis() > endLimit) return null;
        }

        // Sada je c >= now i <= endLimit → to je sledeća pojava
        return c.getTimeInMillis();
    }

    /**
     * Vrati narednih `count` pojava posle SLEDEĆE (tj. iznad izračunate),
     * uključujući samo one koje su <= endDate.
     */
    public static List<Long> upcomingOccurrences(Task t, int count) {
        List<Long> out = new ArrayList<>();
        if (t == null || !t.recurring || count <= 0) return out;

        Long next = computeNextDueAt(t, System.currentTimeMillis());
        if (next == null) return out;

        TimeZone zone = zoneOf(t);
        boolean weekly = isWeekly(t.recurrenceUnit);
        int step = Math.max(1, t.recurrenceInterval);
        long endLimit = endLimitMs(t, zone);

        Calendar c = Calendar.getInstance(zone, Locale.getDefault());
        c.setTimeInMillis(next);

        for (int i = 0; i < count; i++) {
            // sledeća je prethodna + interval
            if (weekly) c.add(Calendar.WEEK_OF_YEAR, step);
            else        c.add(Calendar.DAY_OF_MONTH, step);

            long ts = c.getTimeInMillis();
            if (ts > endLimit) break;   // ne prikazuj posle kraja
            out.add(ts);
        }
        return out;
    }
    public static List<Long> upcomingOccurrencesUntilEnd(Task t) {
        List<Long> out = new ArrayList<>();
        if (t == null || !t.recurring) return out;

        Long next = computeNextDueAt(t, System.currentTimeMillis());
        if (next == null) return out;

        TimeZone zone = zoneOf(t);
        boolean weekly = isWeekly(t.recurrenceUnit);
        int step = Math.max(1, t.recurrenceInterval);
        long endLimit = endLimitMs(t, zone);

        Calendar c = Calendar.getInstance(zone, Locale.getDefault());
        c.setTimeInMillis(next);

        while (true) {
            // dodaj sledeći posle “next”
            if (weekly) c.add(Calendar.WEEK_OF_YEAR, step);
            else        c.add(Calendar.DAY_OF_MONTH, step);

            long ts = c.getTimeInMillis();
            if (ts > endLimit) break; // uključivo: ts == endLimit prolazi
            out.add(ts);
        }
        return out;
    }

}
