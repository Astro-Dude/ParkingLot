package model;

public class EntryGate {
    private final String gateId;
    private final int floor;
    private final int position;

    public EntryGate(String gateId, int floor, int position) {
        this.gateId = gateId;
        this.floor = floor;
        this.position = position;
    }

    public String getGateId() {
        return gateId;
    }

    public int getFloor() {
        return floor;
    }

    public int getPosition() {
        return position;
    }
}
