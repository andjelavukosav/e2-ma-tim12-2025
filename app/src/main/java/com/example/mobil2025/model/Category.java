// com.example.mobil2025.model.Category
package com.example.mobil2025.model;

public class Category {
    public String id;        // Firestore doc id
    public String ownerUid;  // vlasnik
    public String name;      // npr. "zdravlje"
    public String colorHex;  // npr. "#43A047"
    public long createdAt;

    public Category() {}
}
