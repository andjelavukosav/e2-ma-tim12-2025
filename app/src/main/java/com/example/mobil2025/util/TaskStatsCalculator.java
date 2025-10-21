package com.example.mobil2025.util;

import com.example.mobil2025.model.StageStats;

import java.util.List;

/**
 * Helper klasa za izračunavanje statistike zadataka
 * Koristi se za određivanje šanse napada u Boss borbi
 */
public class TaskStatsCalculator {

    /**
     * Status zadatka
     */
    public enum TaskStatus {
        COMPLETED,      // Uspešno završen
        FAILED,         // Neuspešan
        PAUSED,         // Pauziran (ne računa se)
        CANCELLED,      // Otkazan (ne računa se)
        OVER_QUOTA      // Preko kvote (ne računa se)
    }

    /**
     * Kalkuliše StageStats na osnovu liste zadataka
     * @param tasks - lista svih zadataka u trenutnoj etapi
     * @return StageStats sa procentom uspešnosti
     */
    public static StageStats calculateStageStats(List<Task> tasks) {
        int completedCount = 0;
        int totalCount = 0;

        for (Task task : tasks) {
            // Preskoči pauzirane, otkazane i zadatke iznad kvote
            if (task.getStatus() == TaskStatus.PAUSED ||
                    task.getStatus() == TaskStatus.CANCELLED ||
                    task.getStatus() == TaskStatus.OVER_QUOTA) {
                continue;
            }

            totalCount++;

            if (task.getStatus() == TaskStatus.COMPLETED) {
                completedCount++;
            }
        }

        return new StageStats(completedCount, totalCount);
    }

    /**
     * Kalkuliše StageStats na osnovu brojeva (ako već imaš podatke)
     */
    public static StageStats calculateStageStats(int completedTasks,
                                                 int failedTasks,
                                                 int pausedTasks,
                                                 int cancelledTasks,
                                                 int overQuotaTasks) {
        // Ukupan broj zadataka koji se računaju
        int totalCount = completedTasks + failedTasks;
        // Isključi pauzirane, otkazane i preko kvote

        return new StageStats(completedTasks, totalCount);
    }

    /**
     * Primer Task klase (prilagodi svojoj strukturi)
     */
    public static class Task {
        private String id;
        private TaskStatus status;

        public Task(String id, TaskStatus status) {
            this.id = id;
            this.status = status;
        }

        public TaskStatus getStatus() {
            return status;
        }

        public String getId() {
            return id;
        }
    }

    /**
     * Primer upotrebe:
     *
     * // Iz baze ili liste
     * List<Task> tasks = taskRepository.getTasksForCurrentStage();
     * StageStats stats = TaskStatsCalculator.calculateStageStats(tasks);
     *
     * // Ili direktno sa brojkama
     * StageStats stats = TaskStatsCalculator.calculateStageStats(
     *     10,  // completed
     *     5,   // failed
     *     2,   // paused
     *     1,   // cancelled
     *     0    // over quota
     * );
     *
     * // Šansa pogotka = 10 / (10 + 5) = 66.67%
     */
}
