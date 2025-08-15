package com.anoop.vechile.Telimatry.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_telemetry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleTelemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vehicleId;
    private double speed;
    private double fuelLevel;
    private double engineTemp;

    private LocalDateTime timestamp;
}
