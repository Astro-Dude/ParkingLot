package strategy;

import enums.SlotType;
import enums.VehicleType;
import model.EntryGate;
import model.ParkingSlot;
import service.ParkingSlotManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class NearestSlotAssignmentStrategy implements SlotAssignmentStrategy {

    private static final int FLOOR_WEIGHT = 100;

    private static final Map<VehicleType, List<SlotType>> COMPATIBILITY = Map.of(
            VehicleType.TWO_WHEELER, List.of(SlotType.SMALL, SlotType.MEDIUM, SlotType.LARGE),
            VehicleType.CAR, List.of(SlotType.MEDIUM, SlotType.LARGE),
            VehicleType.BUS, List.of(SlotType.LARGE)
    );

    @Override
    public ParkingSlot assignSlot(VehicleType vehicleType, SlotType requestedSlotType,
                                  EntryGate entryGate, ParkingSlotManager slotManager) {

        List<SlotType> compatibleTypes = COMPATIBILITY.get(vehicleType);

        if (requestedSlotType != null) {
            if (!compatibleTypes.contains(requestedSlotType)) {
                throw new IllegalArgumentException(
                        vehicleType + " cannot park in " + requestedSlotType + " slot");
            }
            compatibleTypes = List.of(requestedSlotType);
        }

        List<ParkingSlot> candidates = new ArrayList<>();
        for (SlotType type : compatibleTypes) {
            candidates.addAll(slotManager.getAvailableSlots(type));
        }

        return candidates.stream()
                .min(Comparator.comparingInt(slot -> distance(entryGate, slot)))
                .orElse(null);
    }

    private int distance(EntryGate gate, ParkingSlot slot) {
        int floorDiff = Math.abs(gate.getFloor() - slot.getFloor());
        int posDiff = Math.abs(gate.getPosition() - slot.getPosition());
        return floorDiff * FLOOR_WEIGHT + posDiff;
    }
}
