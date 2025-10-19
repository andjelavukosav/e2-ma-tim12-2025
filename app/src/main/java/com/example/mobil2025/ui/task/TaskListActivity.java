// com.example.mobil2025.ui.task.TaskListActivity
package com.example.mobil2025.ui.task;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobil2025.R;
import com.example.mobil2025.data.repo.TaskRepository;
import com.example.mobil2025.model.Task;
import com.example.mobil2025.util.TaskStatusUpdater;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TaskListActivity extends AppCompatActivity {

    private RecyclerView rvTasks;
    private TaskListAdapter adapter;
    private TabLayout tabLayout;

    private final TaskRepository repo = new TaskRepository();
    private ListenerRegistration reg;

    private final List<Task> all = new ArrayList<>();
    private final List<Task> singles = new ArrayList<>();
    private final List<Task> recurs = new ArrayList<>();

    @Override
    public void onResume() {
        super.onResume();
        new TaskStatusUpdater().markExpiredTasksAsNotDone();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Jednokratni"));
        tabLayout.addTab(tabLayout.newTab().setText("Ponavljajući"));

        rvTasks = findViewById(R.id.rvTasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TaskListAdapter(t -> {
            android.content.Intent it = new android.content.Intent(this, TaskDetailActivity.class);
            it.putExtra("taskId", t.id);
            startActivity(it);
        });
        rvTasks.setAdapter(adapter);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            // ✅ ispravljeno: listenTasksForUser (ne plural)
            reg = repo.listenTasksForUsers(uid, (QuerySnapshot snapshot, FirebaseFirestoreException e) -> {
                if (e != null || snapshot == null) return;
                all.clear();
                all.addAll(snapshot.toObjects(Task.class));
                rebuildFiltersAndShow();
            });
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { showCurrentTab(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) { showCurrentTab(); }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reg != null) reg.remove();
    }

    /** Filtriraj tako da lista prikazuje SAMO sadašnje i buduće zadatke. */
    private void rebuildFiltersAndShow() {
        singles.clear();
        recurs.clear();

        long now = System.currentTimeMillis();

        for (Task t : all) {
            // po želji: preskoči otkazane iz liste
            boolean canceled = "canceled".equalsIgnoreCase(t.status);

            if (Boolean.TRUE.equals(t.recurring)) {
                // PONAVLJAJUĆI: prikazuj u listi ako raspored i dalje važi (nema kraja ili kraj ≥ sada)
                // i ako postoji smislen nextDueAt u budućnosti/sada
                boolean notEnded = (t.endDate == 0L) || (t.endDate >= now);
                boolean hasNext = (t.nextDueAt != null && t.nextDueAt > 0);
                if (!canceled && notEnded && hasNext && t.nextDueAt >= now) {
                    recurs.add(t);
                }
            } else {
                // JEDNOKRATNI: prikazuj samo ako je rok sada ili u budućnosti
                boolean futureOrNow = (t.dueTime >= now);
                // po želji iz liste izbaci "done" (uradjene) — jer su prošli (ili odmah nestaju kad prodje vreme)
                boolean notDone = !"done".equalsIgnoreCase(t.status);
                if (!canceled && futureOrNow && notDone) {
                    singles.add(t);
                }
            }
        }

        // Sortiraj po narednom roku (manje -> pre)
        singles.sort((a, b) -> Long.compare(a.dueTime, b.dueTime));
        recurs.sort((a, b) -> {
            long na = (a.nextDueAt != null && a.nextDueAt > 0) ? a.nextDueAt : Long.MAX_VALUE;
            long nb = (b.nextDueAt != null && b.nextDueAt > 0) ? b.nextDueAt : Long.MAX_VALUE;
            return Long.compare(na, nb);
        });

        showCurrentTab();
    }

    private void showCurrentTab() {
        int pos = tabLayout.getSelectedTabPosition();
        if (pos == 1) {
            adapter.submitList(new ArrayList<>(recurs));
        } else {
            adapter.submitList(new ArrayList<>(singles));
        }
    }

}
