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
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

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
            reg = repo.listenTasksForUsers(uid, (QuerySnapshot snapshot, FirebaseFirestoreException e) -> {
                if (e != null || snapshot == null) return;
                all.clear();
                all.addAll(snapshot.toObjects(Task.class));

                // Sortiraj (kao što si već imala): po sledećem roku
                all.sort((a, b) -> {
                    long na = a.recurring ? (a.nextDueAt != null && a.nextDueAt > 0 ? a.nextDueAt : Long.MAX_VALUE) : a.dueTime;
                    long nb = b.recurring ? (b.nextDueAt != null && b.nextDueAt > 0 ? b.nextDueAt : Long.MAX_VALUE) : b.dueTime;
                    return Long.compare(na, nb);
                });

                // filtriraj listu za oba taba
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

    private void rebuildFiltersAndShow() {
        singles.clear();
        recurs.clear();
        for (Task t : all) {
            if (t.recurring) recurs.add(t);
            else singles.add(t);
        }
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

    private void showStatusSheet(Task t) {
        // vrlo jednostavno: AlertDialog sa listom statusa
        String[] opts = new String[] { "Aktivno", "Urađeno", "Pauzirano", "Otkazano" };
        String[] values = new String[] { "active", "done", "paused", "canceled" };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(t.name)
                .setItems(opts, (d, which) -> {
                    repo.updateTaskStatus(t.id, values[which],
                            v -> Toast.makeText(this, "Status ažuriran", Toast.LENGTH_SHORT).show(),
                            e -> Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Zatvori", null)
                .show();
    }
}
