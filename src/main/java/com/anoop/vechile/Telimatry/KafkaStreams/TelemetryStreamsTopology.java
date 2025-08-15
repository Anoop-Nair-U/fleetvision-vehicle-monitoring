package com.anoop.vechile.Telimatry.KafkaStreams;

import com.anoop.vechile.Telimatry.DTO.TelemetryWindowMetric;
import com.anoop.vechile.Telimatry.KafkaStreams.TelemetryAgg;
import com.anoop.vechile.Telimatry.Entity.VehicleTelemetry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Duration;

@Configuration
public class TelemetryStreamsTopology {

    private static final Logger log = LoggerFactory.getLogger(TelemetryStreamsTopology.class);

    @Value("${kafka.telemetry.topic:VEHICLE-TELEMETRY}")
    private String telemetryTopic;

    @Bean
    public KStream<String, VehicleTelemetry> telemetryStream(
            StreamsBuilder builder,
            ObjectMapper objectMapper,
            SimpMessagingTemplate messagingTemplate
    ) {

        // Serdes
        final Serde<String> stringSerde = Serdes.String();
        final JsonSerde<VehicleTelemetry> vehicleSerde = new JsonSerde<>(VehicleTelemetry.class);
        final JsonSerde<TelemetryAgg> telemetryAggSerde = new JsonSerde<>(TelemetryAgg.class);

        // 1) consume raw messages as String (safer if topic contains mixed formats)
        KStream<String, String> source = builder.stream(
                telemetryTopic,
                Consumed.with(stringSerde, stringSerde)
        );

        // 2) parse JSON -> VehicleTelemetry, filter bad messages, and key by vehicleId
        KStream<String, VehicleTelemetry> byVehicle = source
                .mapValues(json -> {
                    try {
                        return objectMapper.readValue(json, VehicleTelemetry.class);
                    } catch (Exception ex) {
                        // skip malformed records
                        log.warn("Skipping malformed telemetry message: {}", json);
                        return null;
                    }
                })
                .filter((k, v) -> v != null)
                .selectKey((k, v) -> v.getVehicleId());

        // 3) group by key (vehicleId)
        TimeWindows windows = TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(10));

        KTable<Windowed<String>, TelemetryAgg> aggregated = byVehicle
                .groupByKey(Grouped.with(stringSerde, vehicleSerde))
                .windowedBy(windows)
                .aggregate(
                        TelemetryAgg::empty, // initializer
                        (vehicleId, telemetry, aggregate) -> {
                            // update aggregate with incoming telemetry and return it
                            aggregate.add(
                                    telemetry.getSpeed(),
                                    telemetry.getEngineTemp(),
                                    telemetry.getFuelLevel(),
                                    telemetry.getTimestamp() != null
                                            ? telemetry.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                                            : System.currentTimeMillis()
                            );
                            return aggregate;
                        },
                        // IMPORTANT: use WindowStore for windowed aggregations
                        Materialized.<String, TelemetryAgg, WindowStore<Bytes, byte[]>>as("telemetry-agg-store")
                                .withKeySerde(stringSerde)
                                .withValueSerde(telemetryAggSerde)
                );

        // 4) Send aggregated DTO to WebSocket topic
        aggregated.toStream().foreach((windowedKey, aggState) -> {
            if (aggState == null) return;
            TelemetryWindowMetric dto = TelemetryWindowMetric.builder()
                    .vehicleId(windowedKey.key())
                    .windowStartEpochMs(windowedKey.window().start())
                    .windowEndEpochMs(windowedKey.window().end())
                    .avgSpeed(aggState.avgSpeed())
                    .maxEngineTemp(aggState.getMaxEngineTemp())
                    .lastFuelLevel(aggState.getLastFuelLevel())
                    .build();

            log.info("Broadcasting metric to /topic/metrics: {}", dto);
            // convert to simple map/string if you had serialization issues in the past
            messagingTemplate.convertAndSend("/topic/metrics", dto);
        });

        // 5) Mirror parsed raw telemetry to live feed topic
        byVehicle.foreach((vehicleId, telemetry) -> {
            log.debug("Broadcasting telemetry to /topic/telemetry: {}", telemetry);
            messagingTemplate.convertAndSend("/topic/telemetry", telemetry);
        });

        return byVehicle;
    }
}
