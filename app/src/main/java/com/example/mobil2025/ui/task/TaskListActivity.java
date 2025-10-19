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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class TaskListActivity extends AppCompatActivity {

    private RecyclerView rvTasks;
    private TaskListAdapter adapter;
    private final TaskRepository repo = new TaskRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        rvTasks = findViewById(R.id.rvTasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskListAdapter(t ->
                Toast.makeText(this, "Klik: " + t.name, Toast.LENGTH_SHORT).show());
        rvTasks.setAdapter(adapter);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            repo.listenTasksForUser(uid, (QuerySnapshot snapshot, FirebaseFirestoreException e) -> {
                if (e != null || snapshot == null) return;
                List<Task> list = snapshot.toObjects(Task.class);
                list.sort((a, b) -> {
                    long na = a.recurring ? (a.nextDueAt != null ? a.nextDueAt : Long.MAX_VALUE) : a.dueTime;
                    long nb = b.recurring ? (b.nextDueAt != null ? b.nextDueAt : Long.MAX_VALUE) : b.dueTime;
                    return Long.compare(na, nb);
                });
                adapter.submitList(list);
            });
        }
    }
}
