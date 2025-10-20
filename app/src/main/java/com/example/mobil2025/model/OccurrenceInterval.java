package com.example.mobil2025.model;

public class OccurrenceInterval {
    public Long date;      // timestamp za tu pojavu
    public String status;  // "active", "done", "paused"

    public OccurrenceInterval() {}
    public OccurrenceInterval(Long date, String status) {
        this.date = date;
        this.status = status;
    }
}

