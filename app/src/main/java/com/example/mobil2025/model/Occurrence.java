// com.example.mobil2025.model.Occurrence
package com.example.mobil2025.model;

public class Occurrence {
    public String id;
    public String taskId;
    public String ownerUid;

    public long startAt; // millis (početak pojave)
    public long endAt;   // millis (kraj pojave, opcionalno)
    public String status; // "done"

    public String name;       // snapshot podataka radi prikaza posle brisanja taska
    public String description;
    public String categoryId;
    public String categoryColorHex; // za boju u kalendaru
    public String tz;
}
