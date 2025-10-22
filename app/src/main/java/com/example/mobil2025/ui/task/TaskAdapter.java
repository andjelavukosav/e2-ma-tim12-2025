package com.example.mobil2025.ui.task;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobil2025.R;
import com.example.mobil2025.model.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {

    public interface OnTaskClick {
        void onTaskClicked(Task t);
    }

    private final List<Task> tasks;
    private final Map<String, String> categoryColors; // id -> hex
    private final OnTaskClick listener;

    public TaskAdapter(List<Task> tasks, Map<String,String> categoryColors, OnTaskClick listener){
        this.tasks = tasks;
        this.categoryColors = categoryColors;
        this.listener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {
        View color;
        TextView name, date;

        VH(View v) {
            super(v);
            color = v.findViewById(R.id.viewCategoryColor);
            name = v.findViewById(R.id.tvTaskName);
            date = v.findViewById(R.id.tvTaskDate);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Task t = tasks.get(pos);
        h.name.setText(t.name != null ? t.name : "");

        // Prikaz prvog datuma (ili jednokratno vreme)
        List<Long> dates = getTaskDates(t);
        if (!dates.isEmpty()) {
            h.date.setText(new java.text.SimpleDateFormat("dd.MM.yyyy").format(new Date(dates.get(0))));
        } else {
            h.date.setText("");
        }

        // Boja kategorije
        String colorHex = categoryColors.getOrDefault(t.categoryId, "#9E9E9E");
        h.color.setBackgroundColor(android.graphics.Color.parseColor(colorHex));

        h.itemView.setOnClickListener(v -> listener.onTaskClicked(t));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    /** Helper metoda za dobijanje svih datuma zadatka (jednokratni ili ponavljajući) */
    private List<Long> getTaskDates(Task t){
        List<Long> dates = new ArrayList<>();
        if(!t.recurring){
            // Jednokratni zadatak
            dates.add(t.dueTime);
        } else {
            // Ponavljajući zadatak
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(t.startDate);
            while(c.getTimeInMillis() <= t.endDate){
                dates.add(c.getTimeInMillis());
                if("day".equals(t.recurrenceUnit)){
                    c.add(Calendar.DAY_OF_MONTH, t.recurrenceInterval);
                } else if("week".equals(t.recurrenceUnit)){
                    c.add(Calendar.WEEK_OF_YEAR, t.recurrenceInterval);
                }
            }
        }
        return dates;
    }
}
