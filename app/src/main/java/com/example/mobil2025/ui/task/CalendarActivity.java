// LOKACIJA: app/src/main/java/com/example/mobil2025/ui/task/CalendarActivity.java
package com.example.mobil2025.ui.task;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.example.mobil2025.R;
import com.example.mobil2025.data.repo.CategoryRepository;
import com.example.mobil2025.data.repo.OccurrenceRepository;
import com.example.mobil2025.data.repo.TaskRepository;
import com.example.mobil2025.model.Category;
import com.example.mobil2025.model.Occurrence;
import com.example.mobil2025.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Applandeo kalendar:
 * - crta tačke po boji kategorije
 * - klik na dan -> lista zadataka za taj dan (ime + opis)
 * - klik na stavku -> TaskDetailActivity (menjanje statusa tamo)
 */
public class CalendarActivity extends AppCompatActivity {

    // Repo-i
    private final TaskRepository taskRepo = new TaskRepository();
    private final CategoryRepository categoryRepo = new CategoryRepository();
    private final OccurrenceRepository occRepo = new OccurrenceRepository();

    // UI
    private CalendarView calendarView;
    private RecyclerView rvDayTasks;
    private TaskBriefAdapter briefAdapter;

    // State
    private ListenerRegistration tasksReg;
    private final Map<String, Category> categoriesMap = new HashMap<>();
    private final List<Task> allTasks = new ArrayList<>();
    private final Map<String, List<Task>> tasksByDayKey = new HashMap<>(); // "yyyy-MM-dd" -> tasks
    private Calendar lastSelectedDay; // fallback ako korisnik još nije kliknuo dan

    // Konstante
    private static final long SIX_MONTHS_MS = 183L * 24 * 60 * 60 * 1000; // ~6m
    private final TimeZone appZone = TimeZone.getDefault();
    private final Locale appLocale = Locale.getDefault();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // Inicijalni fallback: danas
        lastSelectedDay = Calendar.getInstance();

        calendarView = findViewById(R.id.calendarView);

        // RecyclerView za listu zadataka iz izabranog dana
        rvDayTasks = findViewById(R.id.rvDayTasks);
        if (rvDayTasks == null) {
            throw new IllegalStateException("Dodaj RecyclerView s id=rvDayTasks u activity_calendar.xml");
        }
        rvDayTasks.setLayoutManager(new LinearLayoutManager(this));

        // KREIRAJ JEDAN adapter i odmah ga poveži sa RV
        briefAdapter = new TaskBriefAdapter(t -> {
            if (t == null || t.id == null || t.id.isEmpty()) return;

            long occurrenceAt = (lastSelectedDay != null)
                    ? lastSelectedDay.getTimeInMillis()
                    : System.currentTimeMillis();

            Intent it = new Intent(this, TaskDetailActivity.class);
            // Prosledi sve podatke koji su već u listi
            it.putExtra("taskId", t.id);
            it.putExtra("name", t.name);
            it.putExtra("description", t.description);
            it.putExtra("categoryId", t.categoryId);
            it.putExtra("occurrenceAt", occurrenceAt);

            startActivity(it);
        });


        rvDayTasks.setAdapter(briefAdapter);

        // Klik na dan: osvježi listu za taj dan
        calendarView.setOnDayClickListener(eventDay -> {
            lastSelectedDay = eventDay.getCalendar(); // zapamti izabrani dan
            String key = dayKey(lastSelectedDay.getTimeInMillis(), appZone, appLocale);
            List<Task> dayTasks = tasksByDayKey.getOrDefault(key, Collections.emptyList());
            briefAdapter.submit(dayTasks);
        });

        // Učitaj kategorije pa slušaj zadatke
        loadCategoriesThenTasks();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tasksReg != null) tasksReg.remove();
    }

    // -------------------- Data loading --------------------

    private void loadCategoriesThenTasks() {
        categoryRepo.getMyCategories(list -> {
            categoriesMap.clear();
            for (Category c : list) categoriesMap.put(c.id, c);
            listenTasks();
        }, e -> {
            categoriesMap.clear();
            listenTasks();
        });
    }

    private void listenTasks() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // ✅ ispravno ime metode (slušaj zadatke za korisnika)
        tasksReg = taskRepo.listenTasksForUsers(uid, (QuerySnapshot snap, com.google.firebase.firestore.FirebaseFirestoreException e) -> {
            if (e != null || snap == null) return;
            allTasks.clear();
            allTasks.addAll(snap.toObjects(Task.class));
            rebuildCalendar();
        });
    }

    // -------------------- Calendar build --------------------

    /** Izračunaj pojave (single + recurring) u prozoru -6m .. +6m i nacrtaj tačke. */
    private void rebuildCalendar() {
        tasksByDayKey.clear();
        List<EventDay> events = new ArrayList<>();

        long now = System.currentTimeMillis();
        long from = now - SIX_MONTHS_MS;
        long to = now + SIX_MONTHS_MS;

        // 1) Dodaj sve taskove
        Set<String> addedTaskIds = new HashSet<>();
        for (Task t : allTasks) {
            Set<String> days = dayKeysForTaskInRange(t, from, to);
            int color = colorForCategory(t.categoryId);

            for (String k : days) {
                tasksByDayKey.computeIfAbsent(k, z -> new ArrayList<>()).add(t);
                addedTaskIds.add(t.id);
                Calendar cal = keyToCalendar(k, appZone, appLocale);
                events.add(new EventDay(cal, new ColorDrawable(color)));
            }
        }

        // 2) Dodaj occurrence-e koji nisu već u task listi
        occRepo.loadOccurrencesInRange(from, to, occs -> {
            for (Occurrence oc : occs) {
                if (addedTaskIds.contains(oc.taskId)) continue;

                String key = dayKey(oc.startAt, appZone, appLocale);
                int color = Color.parseColor(oc.categoryColorHex != null ? oc.categoryColorHex : "#607D8B");
                Calendar cal = keyToCalendar(key, appZone, appLocale);
                events.add(new EventDay(cal, new ColorDrawable(color)));

                Task t = new Task();
                t.id = oc.taskId;
                t.name = oc.name;
                t.description = oc.description;
                t.categoryId = oc.categoryId;
                t.tz = oc.tz;
                t.status = oc.status;

                tasksByDayKey.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
            }


            calendarView.setEvents(events);

            // Prikaži za trenutno izabrani dan
            String todayKey = dayKey(lastSelectedDay != null ? lastSelectedDay.getTimeInMillis() : System.currentTimeMillis(), appZone, appLocale);
            briefAdapter.submit(tasksByDayKey.getOrDefault(todayKey, Collections.emptyList()));
        }, err -> {
            calendarView.setEvents(events);
        });
    }


    // -------------------- Helpers --------------------

    @ColorInt
    private int colorForCategory(String categoryId) {
        try {
            if (categoryId != null && categoriesMap.containsKey(categoryId)) {
                String hex = categoriesMap.get(categoryId).colorHex;
                if (hex != null) return Color.parseColor(hex);
            }
        } catch (Exception ignored) {}
        return Color.parseColor("#607D8B"); // default siva
    }

    /** Kreira skup ključeva dana (yyyy-MM-dd) za single/recurring task u datom opsegu. */
    private Set<String> dayKeysForTaskInRange(Task t, long rangeStart, long rangeEnd) {
        Set<String> out = new HashSet<>();
        if (t == null) return out;

        TimeZone zone = (t.tz == null || t.tz.isEmpty()) ? appZone : TimeZone.getTimeZone(t.tz);

        if (!Boolean.TRUE.equals(t.recurring)) {
            long when = t.dueTime != 0 ? t.dueTime : (t.nextDueAt != null ? t.nextDueAt : 0);
            if (when >= rangeStart && when <= rangeEnd) {
                out.add(dayKey(when, zone, appLocale));
            }
            return out;
        }

        // Ponavljajući: dnevno ili nedeljno u koraku intervala
        int step = Math.max(1, t.recurrenceInterval);
        boolean weekly = isWeekly(t.recurrenceUnit);

        Calendar cur = Calendar.getInstance(zone, appLocale);
        long start = (t.startDate > 0 ? t.startDate : rangeStart);
        cur.setTimeInMillis(start);
        setTimeOfDay(cur, t.timeOfDay);

        long endLimit = (t.endDate > 0 ? alignEndToTod(t.endDate, t.timeOfDay, zone, appLocale) : Long.MAX_VALUE);

        // preskoči do opsega
        while (cur.getTimeInMillis() < rangeStart) {
            if (weekly) cur.add(Calendar.WEEK_OF_YEAR, step);
            else        cur.add(Calendar.DAY_OF_MONTH, step);
            if (cur.getTimeInMillis() > endLimit) return out;
        }
        // skupljaj dok smo u opsegu
        while (true) {
            long s = cur.getTimeInMillis();
            if (s > endLimit || s > rangeEnd) break;
            out.add(dayKey(s, zone, appLocale));
            if (weekly) cur.add(Calendar.WEEK_OF_YEAR, step);
            else        cur.add(Calendar.DAY_OF_MONTH, step);
        }
        return out;
    }

    private static boolean isWeekly(String u) {
        if (u == null) return false;
        String x = u.toLowerCase(Locale.ROOT).trim();
        return x.equals("week") || x.equals("weeks") || x.equals("weekly")
                || x.equals("nedelja") || x.equals("nedelje") || x.equals("nedeljno");
    }

    private static void setTimeOfDay(Calendar cal, String tod) {
        int h = 9, m = 0;
        try {
            if (tod != null) {
                String[] p = tod.split(":");
                h = Integer.parseInt(p[0]); m = Integer.parseInt(p[1]);
            }
        } catch (Exception ignored) {}
        cal.set(Calendar.HOUR_OF_DAY, h);
        cal.set(Calendar.MINUTE, m);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private static long alignEndToTod(long endDate, String tod, TimeZone zone, Locale loc) {
        Calendar c = Calendar.getInstance(zone, loc);
        c.setTimeInMillis(endDate);
        setTimeOfDay(c, tod);
        return c.getTimeInMillis();
    }

    private static String dayKey(long millis, TimeZone zone, Locale loc) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", loc);
        f.setTimeZone(zone);
        return f.format(new Date(millis));
    }

    private static Calendar keyToCalendar(String key, TimeZone zone, Locale loc) {
        // key: yyyy-MM-dd
        String[] p = key.split("-");
        int y = Integer.parseInt(p[0]);
        int M = Integer.parseInt(p[1]) - 1;
        int d = Integer.parseInt(p[2]);
        Calendar c = Calendar.getInstance(zone, loc);
        c.clear();
        c.set(y, M, d, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    // ==================== Adapter (ime + opis) ====================

    private static class TaskBriefAdapter extends RecyclerView.Adapter<TaskBriefAdapter.VH> {
        interface OnTaskClick { void onClick(Task t); }

        private final List<Task> data = new ArrayList<>();
        private final OnTaskClick click;

        TaskBriefAdapter(OnTaskClick click) { this.click = click; }

        void submit(List<Task> items) {
            data.clear();
            if (items != null) data.addAll(items);
            notifyDataSetChanged();
        }

        @Override public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task_brief, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(VH h, int pos) {
            Task t = data.get(pos);
            h.tvName.setText(t.name != null ? t.name : "");
            h.tvDesc.setText(t.description != null && !t.description.isEmpty() ? t.description : "—");
            h.itemView.setOnClickListener(v -> click.onClick(t));
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            android.widget.TextView tvName, tvDesc;
            VH(android.view.View v) {
                super(v);
                tvName = v.findViewById(R.id.tvName);
                tvDesc = v.findViewById(R.id.tvDesc);
            }
        }
    }
}
