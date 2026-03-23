package service;

import enums.SlotType;
import model.*;
import strategy.PricingStrategy;
import strategy.SlotAssignmentStrategy;

import java.time.LocalDateTime;
import java.util.Map;

public class ParkingLot {

    private final ParkingSlotManager slotManager;
    private final TicketService ticketService;
    private final SlotAssignmentStrategy assignmentStrategy;
    private final PricingStrategy pricingStrategy;
    private final Map<String, EntryGate> gates;

    public ParkingLot(ParkingSlotManager slotManager,
                      TicketService ticketService,
                      SlotAssignmentStrategy assignmentStrategy,
                      PricingStrategy pricingStrategy,
                      Map<String, EntryGate> gates) {
        this.slotManager = slotManager;
        this.ticketService = ticketService;
        this.assignmentStrategy = assignmentStrategy;
        this.pricingStrategy = pricingStrategy;
        this.gates = gates;
    }

    public ParkingTicket park(Vehicle vehicle, LocalDateTime entryTime,
                              SlotType requestedSlotType, String entryGateId) {

        EntryGate gate = gates.get(entryGateId);
        if (gate == null) {
            throw new IllegalArgumentException("Unknown gate: " + entryGateId);
        }

        ParkingSlot slot = assignmentStrategy.assignSlot(
                vehicle.getVehicleType(), requestedSlotType, gate, slotManager);

        if (slot == null) {
            throw new IllegalStateException("No available slot for " + vehicle.getVehicleType());
        }

        slot.occupy();
        return ticketService.createTicket(vehicle, slot, entryTime);
    }

    public Map<SlotType, int[]> status() {
        return slotManager.getStatusMap();
    }

    public Bill exit(String ticketId, LocalDateTime exitTime) {
        ParkingTicket ticket = ticketService.removeTicket(ticketId);
        ticket.getSlot().vacate();

        double amount = pricingStrategy.calculateCharge(
                ticket.getSlot().getSlotType(), ticket.getEntryTime(), exitTime);

        long hours = (java.time.Duration.between(ticket.getEntryTime(), exitTime).toMinutes() + 59) / 60;

        return new Bill(ticket, exitTime, hours, amount);
    }
}
