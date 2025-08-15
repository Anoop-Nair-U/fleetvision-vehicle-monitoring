package com.anoop.vechile.Telimatry.KafkaConsumer;

import com.anoop.vechile.Telimatry.Entity.VehicleTelemetry;
import com.anoop.vechile.Telimatry.Repository.VehicleTelemetryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class VehicleTelemetryConsumer {

    private final VehicleTelemetryRepository repository;
    private final ObjectMapper objectMapper;

    public VehicleTelemetryConsumer(VehicleTelemetryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${kafka.telemetry.topic}", groupId = "vehicle-telemetry-group")
    public void consumeTelemetry(String message) {
        try {
            VehicleTelemetry telemetry = objectMapper.readValue(message, VehicleTelemetry.class);
            repository.save(telemetry);
            System.out.println("✅ Telemetry data saved: " + telemetry);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
