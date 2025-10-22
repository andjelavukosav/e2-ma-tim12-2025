package com.example.mobil2025.ui.task;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobil2025.R;
import com.example.mobil2025.model.OccurrenceInterval;
import com.example.mobil2025.model.Task;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class TaskListAdapter extends ListAdapter<Task, RecyclerView.ViewHolder> {

    private static final int TYPE_SINGLE = 1;
    private static final int TYPE_RECURRING = 2;

    public interface OnTaskClick { void onTaskClick(Task t); }
    private final OnTaskClick click;

    public TaskListAdapter(OnTaskClick click) {
        super(DIFF);
        this.click = click;
    }

    private static final DiffUtil.ItemCallback<Task> DIFF = new DiffUtil.ItemCallback<Task>() {
        @Override public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return Objects.equals(oldItem.id, newItem.id);
        }
        @Override
        public boolean areContentsTheSame(@NonNull Task o, @NonNull Task n) {
            return o.recurring == n.recurring
                    && Objects.equals(o.name, n.name)
                    && Objects.equals(o.dueTime, n.dueTime)
                    && Objects.equals(o.nextDueAt, n.nextDueAt);
        }

    };

    @Override public int getItemViewType(int position) {
        return getItem(position).recurring ? TYPE_RECURRING : TYPE_SINGLE;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_RECURRING)
            return new RecVH(inf.inflate(R.layout.item_task_recurring, parent, false));
        else
            return new SingleVH(inf.inflate(R.layout.item_task_single, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        Task t = getItem(position);
        if (h instanceof SingleVH) ((SingleVH) h).bind(t, click);
        else ((RecVH) h).bind(t, click);
    }

    // ---- Jednokratni
    static class SingleVH extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory, tvDue;
        SingleVH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvCategory = v.findViewById(R.id.tvCategory);
            tvDue = v.findViewById(R.id.tvDue);
        }
        void bind(Task t, OnTaskClick click) {
            tvName.setText(t.name);
            tvCategory.setText(t.categoryId != null ? t.categoryId : "");
            tvDue.setText("Rok: " + formatDateTime(t.dueTime, t.tz));
            itemView.setOnClickListener(v -> click.onTaskClick(t));
        }
    }

    // ---- Ponavljajući
    static class RecVH extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory, tvRule, tvNext, tvUpcoming;
        RecVH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvCategory = v.findViewById(R.id.tvCategory);
            tvRule = v.findViewById(R.id.tvRule);
            tvNext = v.findViewById(R.id.tvNext);
            tvUpcoming = v.findViewById(R.id.tvUpcoming);
        }
        void bind(Task t, OnTaskClick click) {
            tvName.setText(t.name);
            tvCategory.setText(t.categoryId != null ? t.categoryId : "");

            // Formatiranje pravila ponavljanja
            boolean isWeek = isWeekly(t.recurrenceUnit);
            String intervalTxt = (t.recurrenceInterval <= 1)
                    ? (isWeek ? "svake nedelje" : "svakog dana")
                    : String.format(Locale.getDefault(), "svakih %d %s",
                    t.recurrenceInterval, isWeek ? "nedelje" : "dana");
            String opseg = String.format("(%s — %s)",
                    formatDate(t.startDate, t.tz),
                    t.endDate != null && t.endDate > 0 ? formatDate(t.endDate, t.tz) : "bez kraja");
            tvRule.setText(intervalTxt + " " + opseg);

            long now = System.currentTimeMillis();

            // Filtriraj samo buduće intervale
            List<OccurrenceInterval> futureIntervals = new java.util.ArrayList<>();
            if (t.intervals != null) {
                for (OccurrenceInterval oi : t.intervals) {
                    if (oi.date != null && oi.date >= now && !"canceled".equalsIgnoreCase(oi.status)) {
                        futureIntervals.add(oi);
                    }
                }
            }

            if (!futureIntervals.isEmpty()) {
                // Prvi interval u budućnosti
                tvNext.setText("Sledeće: " + formatDateTime(futureIntervals.get(0).date, t.tz));

                // Svi budući intervali
                StringBuilder sb = new StringBuilder("Naredno: ");
                for (int i = 0; i < futureIntervals.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(formatDateTime(futureIntervals.get(i).date, t.tz));
                }
                tvUpcoming.setText(sb.toString());
            } else {
                tvNext.setText("Nema više pojava");
                tvUpcoming.setText("");
            }

            itemView.setOnClickListener(v -> click.onTaskClick(t));
        }


        private boolean isWeekly(String unitRaw) {
            if (unitRaw == null) return false;
            String u = unitRaw.trim().toLowerCase(Locale.ROOT);
            return u.equals("week") || u.equals("weeks") || u.equals("weekly")
                    || u.equals("nedelja") || u.equals("nedelje") || u.equals("nedeljno");
        }
    }

    // ---- Formatiranje datuma
    static String formatDateTime(long millis, String tz) {
        SimpleDateFormat f = new SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault());
        if (tz != null && !tz.isEmpty()) f.setTimeZone(TimeZone.getTimeZone(tz));
        return f.format(new java.util.Date(millis));
    }

    static String formatDate(long millis, String tz) {
        SimpleDateFormat f = new SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault());
        if (tz != null && !tz.isEmpty()) f.setTimeZone(TimeZone.getTimeZone(tz));
        return f.format(new java.util.Date(millis));
    }
}
