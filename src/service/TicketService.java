package service;

import model.ParkingSlot;
import model.ParkingTicket;
import model.Vehicle;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TicketService {

    private final Map<String, ParkingTicket> activeTickets = new HashMap<>();

    public ParkingTicket createTicket(Vehicle vehicle, ParkingSlot slot, LocalDateTime entryTime) {
        String ticketId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ParkingTicket ticket = new ParkingTicket(ticketId, vehicle, slot, entryTime);
        activeTickets.put(ticketId, ticket);
        return ticket;
    }

    public ParkingTicket getTicket(String ticketId) {
        ParkingTicket ticket = activeTickets.get(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket not found: " + ticketId);
        }
        return ticket;
    }

    public ParkingTicket removeTicket(String ticketId) {
        ParkingTicket ticket = activeTickets.remove(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket not found: " + ticketId);
        }
        return ticket;
    }
}
