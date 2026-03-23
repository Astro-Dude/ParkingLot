import enums.SlotType;
import enums.VehicleType;
import model.*;
import service.ParkingLot;
import service.ParkingSlotManager;
import service.TicketService;
import strategy.HourlyPricingStrategy;
import strategy.NearestSlotAssignmentStrategy;

import java.time.LocalDateTime;
import java.util.*;

public class Main {

    public static void main(String[] args) {

        List<ParkingSlot> slots = new ArrayList<>();
        int id = 1;

        int[][][] floorConfig = {
                {{3, 2, 1}},
                {{2, 3, 1}},
                {{0, 2, 3}},
        };
        SlotType[] types = {SlotType.SMALL, SlotType.MEDIUM, SlotType.LARGE};

        for (int floor = 0; floor < floorConfig.length; floor++) {
            int pos = 0;
            for (int t = 0; t < types.length; t++) {
                int count = floorConfig[floor][0][t];
                for (int i = 0; i < count; i++) {
                    slots.add(new ParkingSlot("S" + id++, types[t], floor, pos++));
                }
            }
        }

        Map<String, EntryGate> gates = new LinkedHashMap<>();
        gates.put("G1", new EntryGate("G1", 0, 0));
        gates.put("G2", new EntryGate("G2", 1, 5));

        Map<SlotType, Double> rates = Map.of(
                SlotType.SMALL, 10.0,
                SlotType.MEDIUM, 20.0,
                SlotType.LARGE, 30.0
        );

        ParkingLot lot = new ParkingLot(
                new ParkingSlotManager(slots),
                new TicketService(),
                new NearestSlotAssignmentStrategy(),
                new HourlyPricingStrategy(rates),
                gates
        );

        LocalDateTime now = LocalDateTime.of(2026, 3, 23, 10, 0);

        System.out.println("=== Multilevel Parking Lot Demo ===");
        printStatus(lot);

        Vehicle bike1 = new Vehicle("BIKE-001", VehicleType.TWO_WHEELER);
        ParkingTicket t1 = lot.park(bike1, now, null, "G1");
        System.out.println("Parked: " + t1);

        Vehicle car1 = new Vehicle("CAR-001", VehicleType.CAR);
        ParkingTicket t2 = lot.park(car1, now, null, "G2");
        System.out.println("Parked: " + t2);

        Vehicle bus1 = new Vehicle("BUS-001", VehicleType.BUS);
        ParkingTicket t3 = lot.park(bus1, now, null, "G1");
        System.out.println("Parked: " + t3);

        Vehicle bike2 = new Vehicle("BIKE-002", VehicleType.TWO_WHEELER);
        ParkingTicket t4 = lot.park(bike2, now, SlotType.MEDIUM, "G1");
        System.out.println("Parked (requested MEDIUM): " + t4);

        printStatus(lot);

        LocalDateTime exitTime1 = now.plusHours(2).plusMinutes(30);
        Bill bill1 = lot.exit(t2.getTicketId(), exitTime1);
        System.out.println("\nExit: " + bill1);

        LocalDateTime exitTime2 = now.plusHours(1);
        Bill bill2 = lot.exit(t4.getTicketId(), exitTime2);
        System.out.println("Exit: " + bill2);
        System.out.println("  (Bike billed at MEDIUM rate because it occupied a MEDIUM slot)");

        printStatus(lot);

        try {
            Vehicle bus2 = new Vehicle("BUS-002", VehicleType.BUS);
            lot.park(bus2, now, SlotType.SMALL, "G1");
        } catch (IllegalArgumentException e) {
            System.out.println("\nExpected error: " + e.getMessage());
        }
    }

    private static void printStatus(ParkingLot lot) {
        System.out.println("\n── Parking Status ──");
        for (Map.Entry<SlotType, int[]> entry : lot.status().entrySet()) {
            int[] info = entry.getValue();
            System.out.printf("  %-6s: %d/%d available%n", entry.getKey(), info[1], info[0]);
        }
        System.out.println();
    }
}
