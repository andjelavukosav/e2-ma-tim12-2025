package com.example.mobil2025.ui.task;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.core.graphics.drawable.DrawableCompat;

import com.applandeo.materialcalendarview.EventDay;
import com.example.mobil2025.model.Category;
import com.example.mobil2025.model.Task;
import com.example.mobil2025.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class CalendarHelper {

    /**
     * Mapira listu Task-ova u EventDay objekte za MaterialCalendarView
     * @param context Context je potreban za učitavanje drawable resursa
     * @param tasks Lista svih taskova
     * @param categories Mapa categoryId -> Category, koristi se za boju
     * @return Lista EventDay koja se može proslediti calendarView.setEvents()
     */
    public static List<EventDay> mapTasksToEventDays(Context context,
                                                     List<Task> tasks,
                                                     Map<String, Category> categories) {
        List<EventDay> events = new ArrayList<>();

        for (Task t : tasks) {
            if (t.nextDueAt == null) continue;

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(t.nextDueAt);

            // podrazumevana boja zelena
            int color = 0xFF43A047;

            if (t.categoryId != null && categories.containsKey(t.categoryId)) {
                String hex = categories.get(t.categoryId).colorHex;
                try {
                    color = android.graphics.Color.parseColor(hex);
                } catch (Exception ignored) {}
            }

            // Napravi drawable i tintuj ga
            Drawable drawable = context.getResources().getDrawable(R.drawable.event_circle, null);
            DrawableCompat.setTint(drawable, color);

            events.add(new EventDay(cal, drawable));
        }

        return events;
    }
}
