package model;

import java.time.LocalDateTime;

public class Bill {
    private final ParkingTicket ticket;
    private final LocalDateTime exitTime;
    private final long hoursParked;
    private final double amount;

    public Bill(ParkingTicket ticket, LocalDateTime exitTime, long hoursParked, double amount) {
        this.ticket = ticket;
        this.exitTime = exitTime;
        this.hoursParked = hoursParked;
        this.amount = amount;
    }

    public ParkingTicket getTicket() {
        return ticket;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public long getHoursParked() {
        return hoursParked;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "Bill[" + ticket.getVehicle().getLicensePlate()
                + " | Slot: " + ticket.getSlot().getSlotType()
                + " | " + hoursParked + " hrs"
                + " | Rs. " + amount + "]";
    }
}
