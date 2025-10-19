package com.example.mobil2025.model;

public class Task {
    public String id;
    public String ownerUid;
    public String categoryId;
    public String name;
    public String description;

    // Jednokratni ili ponavljajući
    public boolean recurring;          // true => koristi recurrence polja ispod
    public int recurrenceInterval;     // 1, 2, 3...
    public String recurrenceUnit;      // "day" ili "week"
    public Long startDate;             // UTC millis na 00:00 startnog datuma (može biti 0 ako nije recurring)
    public Long endDate;               // može biti null
    public String timeOfDay;           // "HH:mm" lokalno vreme (npr. "06:30"), koristi se kad je recurring
    public String tz;                  // npr. "Europe/Belgrade"

    public int weightXP;
    public int importanceXP;

    // Jednokratni due (ili možeš ga koristiti i za single kao nextDueAt)
    public Long dueTime;               // za single: tačan timestamp izvršenja

    // Denormalizovano za recurring
    public Long nextDueAt;             // sledeća pojava (null ili -1L ako je gotovo zauvek)

    public Long createdAt;
    public Long updatedAt;             // dodato, dobro je imati
    public String status;   // "active", "done", "paused", "canceled"

    public Task() {
    }
}
