package com.example.mobil2025;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mobil2025.ui.auth.LoginActivity;
import com.example.mobil2025.ui.category.CategoryListActivity;
import com.example.mobil2025.ui.task.CreateTaskActivity;
import com.example.mobil2025.ui.task.TaskListActivity;   // ✅ import liste zadataka
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private Button btnLogout;
    private Button btnOpenCategories;
    private Button btnCreateTask;
    private Button btnShowTasks; // ✅ NOVO

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnLogout = findViewById(R.id.btnLogout);
        btnOpenCategories = findViewById(R.id.btnOpenCategories);
        btnCreateTask = findViewById(R.id.btnCreateTask);
        btnShowTasks = findViewById(R.id.btnShowTasks); // ✅

        btnCreateTask.setOnClickListener(v ->
                startActivity(new Intent(this, CreateTaskActivity.class)));

        btnOpenCategories.setOnClickListener(v ->
                startActivity(new Intent(this, CategoryListActivity.class)));

        // ✅ Otvori listu zadataka
        btnShowTasks.setOnClickListener(v ->
                startActivity(new Intent(this, TaskListActivity.class)));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onStart(){
        super.onStart();
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}
