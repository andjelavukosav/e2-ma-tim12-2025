package com.example.mobil2025.data.repo;

import com.example.mobil2025.model.Equipment;

import java.util.List;

public interface EquipmentCallback {
    void onLoaded(List<Equipment> equipmentList);
    void onError(Exception e);
}