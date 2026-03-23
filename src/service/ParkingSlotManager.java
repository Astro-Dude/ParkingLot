package service;

import enums.SlotType;
import model.ParkingSlot;

import java.util.*;
import java.util.stream.Collectors;

public class ParkingSlotManager {

    private final Map<SlotType, List<ParkingSlot>> slotsByType;

    public ParkingSlotManager(List<ParkingSlot> allSlots) {
        this.slotsByType = allSlots.stream()
                .collect(Collectors.groupingBy(ParkingSlot::getSlotType));
    }

    public List<ParkingSlot> getAvailableSlots(SlotType slotType) {
        return slotsByType.getOrDefault(slotType, Collections.emptyList())
                .stream()
                .filter(slot -> !slot.isOccupied())
                .collect(Collectors.toList());
    }

    public Map<SlotType, int[]> getStatusMap() {
        Map<SlotType, int[]> status = new LinkedHashMap<>();
        for (SlotType type : SlotType.values()) {
            List<ParkingSlot> slots = slotsByType.getOrDefault(type, Collections.emptyList());
            int total = slots.size();
            int available = (int) slots.stream().filter(s -> !s.isOccupied()).count();
            status.put(type, new int[]{total, available});
        }
        return status;
    }
}
