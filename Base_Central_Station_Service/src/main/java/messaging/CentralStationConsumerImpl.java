package messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import dto.WeatherStatus;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import service.WeatherProcessingService;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static utility.EnvironmentVariablesUtility.getEnvOrDefault;

public class CentralStationConsumerImpl implements CentralStationConsumer {
    private static final String TOPIC_NAME = getEnvOrDefault("KAFKA_TOPIC", "weather_statuses");
    private static final String BOOTSTRAP_SERVERS = getEnvOrDefault("KAFKA_BOOTSTRAP_SERVERS", "127.0.0.1:9092");
    private static final String GROUP_ID = getEnvOrDefault("KAFKA_GROUP_ID", "central-station-group");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WeatherProcessingService weatherService;

    public CentralStationConsumerImpl(WeatherProcessingService weatherService) {
        this.weatherService = weatherService;
    }

    public void startConsuming() {
        Properties properties = getProperties();

        System.out.printf("Starting consumer connecting to Kafka at %s...%n", BOOTSTRAP_SERVERS);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(Collections.singletonList(TOPIC_NAME));

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        WeatherStatus status = objectMapper.readValue(record.value(), WeatherStatus.class);
                        weatherService.processStatus(status);
                    } catch (Exception e) {
                        weatherService.handleFailedMessage(record.value(), e);
                    }
                }
            }
        }
    }

    private static Properties getProperties() {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return properties;
    }
}
