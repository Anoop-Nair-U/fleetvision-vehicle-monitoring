package com.anoop.vechile.Telimatry.KafkaConsumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    @KafkaListener(topics = "${kafka.user.topic}", groupId = "vehicle-telemetry-group")
    public void consumeMessage(String message) {
        System.out.println("🚗 Consumed Message: " + message);

        // TODO: Store this in DB or trigger analytics pipeline
    }
}
