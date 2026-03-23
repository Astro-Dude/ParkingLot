package strategy;

import enums.SlotType;

import java.time.LocalDateTime;

public interface PricingStrategy {

    double calculateCharge(SlotType slotType, LocalDateTime entryTime, LocalDateTime exitTime);
}
