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

public class ParkingLotTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== Parking Lot Test Suite ===\n");

        testParkApi();
        testStatusApi();
        testExitApi();
        testNearestSlotAssignment();
        testVehicleUpsizing();
        testBillingBasedOnSlotType();
        testCeilHourBilling();
        testInvalidSlotForVehicle();
        testInvalidGate();
        testInvalidTicketOnExit();
        testSlotFreedAfterExit();
        testParkingFullScenario();
        testDoubleExitSameTicket();

        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed out of " + (passed + failed) + " ===");
        if (failed > 0) System.exit(1);
    }

    private static ParkingLot buildLot() {
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
        return new ParkingLot(
                new ParkingSlotManager(slots),
                new TicketService(),
                new NearestSlotAssignmentStrategy(),
                new HourlyPricingStrategy(rates),
                gates
        );
    }

    private static void testParkApi() {
        ParkingLot lot = buildLot();
        LocalDateTime now = LocalDateTime.of(2026, 3, 23, 10, 0);

        ParkingTicket t = lot.park(new Vehicle("BIKE-001", VehicleType.TWO_WHEELER), now, null, "G1");
        assertNotNull("park() returns ticket", t);
        assertEquals("park() ticket has vehicle", "BIKE-001", t.getVehicle().getLicensePlate());
        assertNotNull("park() ticket has slot", t.getSlot());
        assertEquals("park() ticket has entry time", now, t.getEntryTime());
    }

    private static void testStatusApi() {
        ParkingLot lot = buildLot();

        Map<SlotType, int[]> status = lot.status();
        assertEquals("status() SMALL total", 5, status.get(SlotType.SMALL)[0]);
        assertEquals("status() SMALL available", 5, status.get(SlotType.SMALL)[1]);
        assertEquals("status() MEDIUM total", 7, status.get(SlotType.MEDIUM)[0]);
        assertEquals("status() LARGE total", 5, status.get(SlotType.LARGE)[0]);

        lot.park(new Vehicle("CAR-001", VehicleType.CAR), LocalDateTime.now(), null, "G1");
        status = lot.status();
        assertEquals("status() after 1 car parked, MEDIUM available", 6, status.get(SlotType.MEDIUM)[1]);
    }

    private static void testExitApi() {
        ParkingLot lot = buildLot();
        LocalDateTime entry = LocalDateTime.of(2026, 3, 23, 10, 0);
        LocalDateTime exit = entry.plusHours(2);

        ParkingTicket t = lot.park(new Vehicle("CAR-001", VehicleType.CAR), entry, null, "G1");
        Bill bill = lot.exit(t.getTicketId(), exit);

        assertNotNull("exit() returns bill", bill);
        assertEquals("exit() bill hours", 2L, bill.getHoursParked());
        assertEquals("exit() bill amount (MEDIUM rate 20 * 2hrs)", 40.0, bill.getAmount());
    }

    private static void testNearestSlotAssignment() {
        ParkingLot lot = buildLot();
        LocalDateTime now = LocalDateTime.now();

        ParkingTicket t1 = lot.park(new Vehicle("B1", VehicleType.TWO_WHEELER), now, null, "G1");
        assertEquals("nearest slot to G1(floor=0,pos=0) is floor 0", 0, t1.getSlot().getFloor());

        ParkingTicket t2 = lot.park(new Vehicle("C1", VehicleType.CAR), now, null, "G2");
        assertEquals("nearest MEDIUM/LARGE to G2(floor=1,pos=5) is floor 1", 1, t2.getSlot().getFloor());
    }

    private static void testVehicleUpsizing() {
        ParkingLot lot = buildLot();
        LocalDateTime now = LocalDateTime.now();

        ParkingTicket t = lot.park(new Vehicle("B1", VehicleType.TWO_WHEELER), now, SlotType.MEDIUM, "G1");
        assertEquals("bike parked in MEDIUM slot", SlotType.MEDIUM, t.getSlot().getSlotType());

        ParkingTicket t2 = lot.park(new Vehicle("B2", VehicleType.TWO_WHEELER), now, SlotType.LARGE, "G1");
        assertEquals("bike parked in LARGE slot", SlotType.LARGE, t2.getSlot().getSlotType());

        ParkingTicket t3 = lot.park(new Vehicle("C1", VehicleType.CAR), now, SlotType.LARGE, "G1");
        assertEquals("car parked in LARGE slot", SlotType.LARGE, t3.getSlot().getSlotType());
    }

    private static void testBillingBasedOnSlotType() {
        ParkingLot lot = buildLot();
        LocalDateTime entry = LocalDateTime.of(2026, 3, 23, 10, 0);
        LocalDateTime exit = entry.plusHours(1);

        ParkingTicket t = lot.park(new Vehicle("B1", VehicleType.TWO_WHEELER), entry, SlotType.MEDIUM, "G1");
        Bill bill = lot.exit(t.getTicketId(), exit);
        assertEquals("bike in MEDIUM billed at MEDIUM rate (20*1)", 20.0, bill.getAmount());

        ParkingTicket t2 = lot.park(new Vehicle("B2", VehicleType.TWO_WHEELER), entry, SlotType.LARGE, "G1");
        Bill bill2 = lot.exit(t2.getTicketId(), exit);
        assertEquals("bike in LARGE billed at LARGE rate (30*1)", 30.0, bill2.getAmount());
    }

    private static void testCeilHourBilling() {
        ParkingLot lot = buildLot();
        LocalDateTime entry = LocalDateTime.of(2026, 3, 23, 10, 0);

        ParkingTicket t = lot.park(new Vehicle("C1", VehicleType.CAR), entry, null, "G1");
        Bill bill = lot.exit(t.getTicketId(), entry.plusMinutes(90));
        assertEquals("1.5 hrs rounds up to 2 hrs", 2L, bill.getHoursParked());
        assertEquals("2 hrs at MEDIUM rate 20", 40.0, bill.getAmount());

        ParkingTicket t2 = lot.park(new Vehicle("C2", VehicleType.CAR), entry, null, "G1");
        Bill bill2 = lot.exit(t2.getTicketId(), entry.plusMinutes(1));
        assertEquals("1 min rounds up to 1 hr", 1L, bill2.getHoursParked());
    }

    private static void testInvalidSlotForVehicle() {
        ParkingLot lot = buildLot();
        LocalDateTime now = LocalDateTime.now();

        try {
            lot.park(new Vehicle("BUS-001", VehicleType.BUS), now, SlotType.SMALL, "G1");
            fail("bus in SMALL should throw");
        } catch (IllegalArgumentException e) {
            pass("bus in SMALL slot rejected");
        }

        try {
            lot.park(new Vehicle("BUS-002", VehicleType.BUS), now, SlotType.MEDIUM, "G1");
            fail("bus in MEDIUM should throw");
        } catch (IllegalArgumentException e) {
            pass("bus in MEDIUM slot rejected");
        }

        try {
            lot.park(new Vehicle("CAR-001", VehicleType.CAR), now, SlotType.SMALL, "G1");
            fail("car in SMALL should throw");
        } catch (IllegalArgumentException e) {
            pass("car in SMALL slot rejected");
        }
    }

    private static void testInvalidGate() {
        ParkingLot lot = buildLot();
        try {
            lot.park(new Vehicle("C1", VehicleType.CAR), LocalDateTime.now(), null, "G99");
            fail("invalid gate should throw");
        } catch (IllegalArgumentException e) {
            pass("invalid gate rejected");
        }
    }

    private static void testInvalidTicketOnExit() {
        ParkingLot lot = buildLot();
        try {
            lot.exit("INVALID-ID", LocalDateTime.now());
            fail("invalid ticket should throw");
        } catch (IllegalArgumentException e) {
            pass("invalid ticket on exit rejected");
        }
    }

    private static void testSlotFreedAfterExit() {
        ParkingLot lot = buildLot();
        LocalDateTime entry = LocalDateTime.of(2026, 3, 23, 10, 0);

        int beforeSmall = lot.status().get(SlotType.SMALL)[1];
        ParkingTicket t = lot.park(new Vehicle("B1", VehicleType.TWO_WHEELER), entry, SlotType.SMALL, "G1");
        int duringSmall = lot.status().get(SlotType.SMALL)[1];
        assertEquals("slot count decreases after park", beforeSmall - 1, duringSmall);

        lot.exit(t.getTicketId(), entry.plusHours(1));
        int afterSmall = lot.status().get(SlotType.SMALL)[1];
        assertEquals("slot count restored after exit", beforeSmall, afterSmall);
    }

    private static void testParkingFullScenario() {
        List<ParkingSlot> slots = new ArrayList<>();
        slots.add(new ParkingSlot("S1", SlotType.LARGE, 0, 0));
        Map<String, EntryGate> gates = Map.of("G1", new EntryGate("G1", 0, 0));
        Map<SlotType, Double> rates = Map.of(SlotType.LARGE, 30.0);
        ParkingLot lot = new ParkingLot(
                new ParkingSlotManager(slots),
                new TicketService(),
                new NearestSlotAssignmentStrategy(),
                new HourlyPricingStrategy(rates),
                gates
        );

        lot.park(new Vehicle("BUS-001", VehicleType.BUS), LocalDateTime.now(), null, "G1");
        try {
            lot.park(new Vehicle("BUS-002", VehicleType.BUS), LocalDateTime.now(), null, "G1");
            fail("parking full should throw");
        } catch (IllegalStateException e) {
            pass("parking full scenario handled");
        }
    }

    private static void testDoubleExitSameTicket() {
        ParkingLot lot = buildLot();
        LocalDateTime entry = LocalDateTime.of(2026, 3, 23, 10, 0);
        ParkingTicket t = lot.park(new Vehicle("C1", VehicleType.CAR), entry, null, "G1");

        lot.exit(t.getTicketId(), entry.plusHours(1));
        try {
            lot.exit(t.getTicketId(), entry.plusHours(2));
            fail("double exit should throw");
        } catch (IllegalArgumentException e) {
            pass("double exit with same ticket rejected");
        }
    }

    private static void assertEquals(String testName, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            pass(testName);
        } else {
            fail(testName + " | expected: " + expected + ", got: " + actual);
        }
    }

    private static void assertNotNull(String testName, Object value) {
        if (value != null) {
            pass(testName);
        } else {
            fail(testName + " | expected non-null");
        }
    }

    private static void pass(String testName) {
        passed++;
        System.out.println("  PASS  " + testName);
    }

    private static void fail(String testName) {
        failed++;
        System.out.println("  FAIL  " + testName);
    }
}
