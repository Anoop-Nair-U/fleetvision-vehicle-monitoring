package com.anoop.vechile.Telimatry.Controller;

import com.anoop.vechile.Telimatry.Entity.VehicleTelemetry;
import com.anoop.vechile.Telimatry.KafkaProducer.KafkaProducerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/telemetry")
public class VehicleTelemetryController {

    @Value("${kafka.telemetry.topic}")
    private String telemetryTopic;

    private final KafkaProducerService kafkaProducerService;

    public VehicleTelemetryController(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostMapping
    public String sendTelemetry(@RequestBody VehicleTelemetry telemetry) {
        telemetry.setTimestamp(LocalDateTime.now());
        kafkaProducerService.sendMessage(telemetryTopic, telemetry);
        return "Telemetry data sent to Kafka!";
    }
}
