package model;

import enums.SlotType;

public class ParkingSlot {
    private final String slotId;
    private final SlotType slotType;
    private final int floor;
    private final int position;
    private boolean occupied;

    public ParkingSlot(String slotId, SlotType slotType, int floor, int position) {
        this.slotId = slotId;
        this.slotType = slotType;
        this.floor = floor;
        this.position = position;
        this.occupied = false;
    }

    public void occupy() {
        if (occupied) {
            throw new IllegalStateException("Slot " + slotId + " is already occupied");
        }
        this.occupied = true;
    }

    public void vacate() {
        if (!occupied) {
            throw new IllegalStateException("Slot " + slotId + " is already vacant");
        }
        this.occupied = false;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public String getSlotId() {
        return slotId;
    }

    public SlotType getSlotType() {
        return slotType;
    }

    public int getFloor() {
        return floor;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return slotId + " (" + slotType + ", Floor " + floor + ")";
    }
}
