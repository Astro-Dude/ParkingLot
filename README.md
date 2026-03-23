# Multilevel Parking Lot - Low Level Design

## Problem Statement

Design a multilevel parking lot system that supports:
- Three slot types: **Small** (2-wheelers), **Medium** (cars), **Large** (buses)
- Configurable hourly rates per slot type
- Multiple entry gates with nearest-slot assignment
- Vehicle upsizing (smaller vehicles can park in larger slots)
- Billing based on **slot type**, not vehicle type

## Class Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          <<enum>> VehicleType                           │
│  TWO_WHEELER, CAR, BUS                                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                          <<enum>> SlotType                              │
│  SMALL, MEDIUM, LARGE                                                   │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────┐   ┌───────────────┐   ┌────────────────┐  ┌──────────┐
│   Vehicle    │   │  ParkingSlot  │   │ ParkingTicket  │  │ EntryGate│
│──────────────│   │───────────────│   │────────────────│  │──────────│
│- licensePlate│   │- slotId       │   │- ticketId      │  │- gateId  │
│- vehicleType │   │- slotType     │   │- vehicle       │  │- floor   │
│              │   │- floor        │   │- slot          │  │- position│
│              │   │- position     │   │- entryTime     │  └──────────┘
│              │   │- occupied     │   └────────────────┘
│              │   │+ occupy()     │         ┌──────────┐
│              │   │+ vacate()     │         │   Bill   │
│              │   └───────────────┘         │──────────│
│              │                             │- ticket  │
└──────────────┘                             │- exitTime│
                                             │- hours   │
                                             │- amount  │
                                             └──────────┘

    <<interface>>                    <<interface>>
┌──────────────────────────┐   ┌──────────────────────┐
│  SlotAssignmentStrategy  │   │   PricingStrategy    │
│──────────────────────────│   │──────────────────────│
│+ assignSlot(             │   │+ calculateCharge(    │
│    VehicleType,          │   │    SlotType,         │
│    SlotType,             │   │    entryTime,        │
│    EntryGate,            │   │    exitTime)         │
│    ParkingSlotManager)   │   └──────────┬───────────┘
└──────────┬───────────────┘              │
           │                              │
           ▼                              ▼
┌──────────────────────────┐   ┌──────────────────────┐
│ NearestSlotAssignment    │   │ HourlyPricingStrategy│
│        Strategy          │   │──────────────────────│
│──────────────────────────│   │- hourlyRates         │
│- COMPATIBILITY map       │   │+ calculateCharge()   │
│- FLOOR_WEIGHT            │   └──────────────────────┘
│+ assignSlot()            │
│- distance()              │
└──────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                    ParkingSlotManager                   │
│─────────────────────────────────────────────────────────│
│- slotsByType: Map<SlotType, List<ParkingSlot>>          │
│─────────────────────────────────────────────────────────│
│+ getAvailableSlots(slotType): List<ParkingSlot>         │
│+ getStatusMap(): Map<SlotType, int[]>                   │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                      TicketService                      │
│─────────────────────────────────────────────────────────│
│- activeTickets: Map<String, ParkingTicket>              │
│─────────────────────────────────────────────────────────│
│+ createTicket(vehicle, slot, entryTime): ParkingTicket  │
│+ getTicket(ticketId): ParkingTicket                     │
│+ removeTicket(ticketId): ParkingTicket                  │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│              ParkingLot  (Facade)                       │
│─────────────────────────────────────────────────────────│
│- slotManager: ParkingSlotManager                        │
│- ticketService: TicketService                           │
│- assignmentStrategy: SlotAssignmentStrategy             │
│- pricingStrategy: PricingStrategy                       │
│- gates: Map<String, EntryGate>                          │
│─────────────────────────────────────────────────────────│
│+ park(Vehicle, entryTime, SlotType, gateId): Ticket     │
│+ exit(ticketId, exitTime): Bill                         │
│+ status(): Map<SlotType, int[]>                         │
└─────────────────────────────────────────────────────────┘
```

## Design Patterns and SOLID Principles

### Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Strategy** | `SlotAssignmentStrategy`, `PricingStrategy` | Encapsulates interchangeable algorithms. Slot assignment can be swapped (nearest, random, floor-priority) and pricing can be swapped (hourly, flat-rate, surge) without touching the `ParkingLot` class. |
| **Facade** | `ParkingLot` | Provides a single, simplified entry point (`park`, `exit`, `status`) that hides the complexity of coordinating `ParkingSlotManager`, `TicketService`, and the strategy classes. |

### SOLID Principles Applied

| Principle | How It's Applied |
|-----------|-----------------|
| **S - Single Responsibility** | `ParkingSlotManager` only manages slot inventory. `TicketService` only manages ticket lifecycle. Strategies only compute their respective algorithm. Each class has exactly one reason to change. |
| **O - Open/Closed** | Adding a new pricing model (e.g., `SurgePricingStrategy`) or a new assignment algorithm requires creating a new class implementing the interface — no existing code is modified. |
| **L - Liskov Substitution** | Any `SlotAssignmentStrategy` implementation can replace `NearestSlotAssignmentStrategy` without breaking `ParkingLot`. Same for `PricingStrategy`. |
| **I - Interface Segregation** | `SlotAssignmentStrategy` and `PricingStrategy` are small, focused interfaces with a single method each. No class is forced to implement methods it doesn't need. |
| **D - Dependency Inversion** | `ParkingLot` depends on abstractions (`SlotAssignmentStrategy`, `PricingStrategy` interfaces), not on concrete implementations. Concrete strategies are injected at construction time. |

### Other Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Encapsulated state transitions** | `ParkingSlot.occupy()` / `vacate()` control their own state with validation, preventing external code from setting an invalid state. |
| **Immutable models** | `Vehicle`, `ParkingTicket`, `Bill`, `EntryGate` are immutable (all fields `final`) — prevents accidental mutation and makes the system easier to reason about. |
| **Distance as a pluggable concept** | Distance calculation lives inside `NearestSlotAssignmentStrategy`, not in the model. If a different distance metric is needed, only the strategy changes. |
| **Billing by slot type** | The `PricingStrategy` receives `SlotType` (not `VehicleType`), enforcing the requirement that billing is based on the allocated slot, not the vehicle. |

## Vehicle-Slot Compatibility

```
Vehicle Type    Compatible Slot Types
─────────────   ──────────────────────
TWO_WHEELER  →  SMALL, MEDIUM, LARGE
CAR          →  MEDIUM, LARGE
BUS          →  LARGE only
```

## Nearest Slot Assignment Algorithm

```
distance(gate, slot) = |gate.floor - slot.floor| × FLOOR_WEIGHT + |gate.position - slot.position|
```

- `FLOOR_WEIGHT = 100` — moving between floors is much more costly than horizontal movement
- Among all available compatible slots, the one with minimum distance is assigned
- If a specific slot type is requested, only that type is considered (after compatibility validation)

## APIs

### `park(Vehicle, entryTime, requestedSlotType, entryGateId) → ParkingTicket`
1. Validates the entry gate exists
2. Determines compatible slot types for the vehicle (or uses the requested type after validation)
3. Delegates to `SlotAssignmentStrategy` to find the nearest available slot
4. Marks the slot as occupied
5. Creates and returns a `ParkingTicket`

### `status() → Map<SlotType, [total, available]>`
Returns current slot availability grouped by type.

### `exit(ticketId, exitTime) → Bill`
1. Removes the ticket from active tickets
2. Vacates the slot
3. Delegates to `PricingStrategy` to calculate the charge (based on slot type)
4. Returns a `Bill` with duration and amount

## Project Structure

```
src/
├── Main.java                              # Demo driver
├── enums/
│   ├── VehicleType.java                   # TWO_WHEELER, CAR, BUS
│   └── SlotType.java                      # SMALL, MEDIUM, LARGE
├── model/
│   ├── Vehicle.java                       # License plate + type
│   ├── ParkingSlot.java                   # Slot with occupy/vacate encapsulation
│   ├── ParkingTicket.java                 # Immutable ticket
│   ├── EntryGate.java                     # Gate with location
│   └── Bill.java                          # Exit bill
├── strategy/
│   ├── SlotAssignmentStrategy.java        # Interface (Strategy Pattern)
│   ├── NearestSlotAssignmentStrategy.java # Nearest-first implementation
│   ├── PricingStrategy.java               # Interface (Strategy Pattern)
│   └── HourlyPricingStrategy.java         # Per-hour billing implementation
└── service/
    ├── ParkingSlotManager.java            # Slot inventory (SRP)
    ├── TicketService.java                 # Ticket lifecycle (SRP)
    └── ParkingLot.java                    # Facade
```

## How to Run

```bash
# Compile
javac -sourcepath src -d out src/Main.java

# Run
java -cp out Main
```

## Sample Output

```
=== Multilevel Parking Lot Demo ===

── Parking Status ──
  SMALL : 5/5 available
  MEDIUM: 7/7 available
  LARGE : 5/5 available

Parked: Ticket[...] BIKE-001 (TWO_WHEELER) -> S1 (SMALL, Floor 0)
Parked: Ticket[...] CAR-001 (CAR) -> S12 (LARGE, Floor 1)
Parked: Ticket[...] BUS-001 (BUS) -> S6 (LARGE, Floor 0)
Parked (requested MEDIUM): Ticket[...] BIKE-002 (TWO_WHEELER) -> S4 (MEDIUM, Floor 0)

── Parking Status ──
  SMALL : 4/5 available
  MEDIUM: 6/7 available
  LARGE : 3/5 available

Exit: Bill[CAR-001 | Slot: LARGE | 3 hrs | Rs. 90.0]
Exit: Bill[BIKE-002 | Slot: MEDIUM | 1 hrs | Rs. 20.0]
  (Bike billed at MEDIUM rate because it occupied a MEDIUM slot)

── Parking Status ──
  SMALL : 4/5 available
  MEDIUM: 7/7 available
  LARGE : 4/5 available

Expected error: BUS cannot park in SMALL slot
```

## Extensibility Examples

| Change Needed | What to Do |
|---------------|-----------|
| Add surge pricing | Create `SurgePricingStrategy implements PricingStrategy`, inject it into `ParkingLot` |
| Add random slot assignment | Create `RandomSlotAssignmentStrategy implements SlotAssignmentStrategy` |
| Add new vehicle type (e.g., TRUCK) | Add to `VehicleType` enum, update compatibility map in strategy |
| Add exit gates | Create `ExitGate` model, add to `ParkingLot` |
| Add payment processing | Create a `PaymentService` and compose it into the facade |
