package strategy;

import enums.SlotType;
import enums.VehicleType;
import model.EntryGate;
import model.ParkingSlot;
import service.ParkingSlotManager;

public interface SlotAssignmentStrategy {

    ParkingSlot assignSlot(VehicleType vehicleType, SlotType requestedSlotType,
                           EntryGate entryGate, ParkingSlotManager slotManager);
}
