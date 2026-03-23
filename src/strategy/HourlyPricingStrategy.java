package strategy;

import enums.SlotType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

public class HourlyPricingStrategy implements PricingStrategy {

    private final Map<SlotType, Double> hourlyRates;

    public HourlyPricingStrategy(Map<SlotType, Double> hourlyRates) {
        this.hourlyRates = hourlyRates;
    }

    @Override
    public double calculateCharge(SlotType slotType, LocalDateTime entryTime, LocalDateTime exitTime) {
        long totalMinutes = Duration.between(entryTime, exitTime).toMinutes();
        long hours = (totalMinutes + 59) / 60;

        Double rate = hourlyRates.get(slotType);
        if (rate == null) {
            throw new IllegalArgumentException("No rate configured for slot type: " + slotType);
        }

        return hours * rate;
    }
}
