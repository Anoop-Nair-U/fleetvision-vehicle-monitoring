package com.anoop.vechile.Telimatry.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelemetryWindowMetric {
    private String vehicleId;
    private long windowStartEpochMs;
    private long windowEndEpochMs;

    private double avgSpeed;
    private double maxEngineTemp;
    private double lastFuelLevel;
}
