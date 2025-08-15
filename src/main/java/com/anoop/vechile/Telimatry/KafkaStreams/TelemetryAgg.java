package com.anoop.vechile.Telimatry.KafkaStreams;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelemetryAgg {
    private long count;
    private double sumSpeed;
    private double maxEngineTemp;
    private double lastFuelLevel;
    private long lastTimestampMs;

    public static TelemetryAgg empty() {
        return new TelemetryAgg(0, 0.0, Double.MIN_VALUE, 0.0, 0L);
    }

    public TelemetryAgg add(double speed, double engineTemp, double fuelLevel, long tsMs) {
        this.count += 1;
        this.sumSpeed += speed;
        this.maxEngineTemp = Math.max(this.maxEngineTemp, engineTemp);
        this.lastFuelLevel = fuelLevel;
        this.lastTimestampMs = tsMs;
        return this;
    }

    public double avgSpeed() {
        return count == 0 ? 0.0 : (sumSpeed / count);
    }
}
