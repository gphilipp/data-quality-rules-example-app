import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.util.Properties;

public class DataQualityRulesExampleApp {

    private static final String TOPIC = "memberships";


    public static Properties loadConfig(final String configFile) throws IOException {
        if (!Files.exists(Paths.get(configFile))) {
            throw new IOException(configFile + " not found.");
        }
        final Properties cfg = new Properties();
        try (InputStream inputStream = new FileInputStream(configFile)) {
            cfg.load(inputStream);
        }
        return cfg;
    }

    public static void main(String[] args) throws IOException {
        final Properties properties = loadConfig("client.properties");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        properties.put(AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION, true);
        properties.put("rules.executors", "checkSsnLen");
        properties.put("rule.executors.checkSsnLen.class", "io.confluent.kafka.schemaregistry.rules.cel.CelExecutor");

        KafkaProducer<String, Membership> producer = new KafkaProducer<>(properties);
        try {
            // Can be produced ok

            /*produceMembership(producer,
                    makeMembership(
                            LocalDate.of(2023, Month.JANUARY, 1),
                            LocalDate.of(2023, Month.DECEMBER, 31),
                            "joe@doe.com",
                            "856-45-6789"
                    )
            )*/
            ;

            produceMembership(producer,
                    makeMembership(
                            LocalDate.of(2023, Month.JANUARY, 1),
                            LocalDate.of(1970, Month.JANUARY, 1),
                            "john.doe",
                            "fizzbuzz"
                    ));
        } finally {
            producer.close();
        }
    }

    private static Membership makeMembership(LocalDate startDate, LocalDate endDate, String email, String ssn) {
        return Membership.newBuilder()
                .setEmail(email)
                .setSsn(ssn)
                .setStartDate(startDate)
                .setEndDate(endDate)
                .build();
    }

    private static void produceMembership(KafkaProducer<String, Membership> producer, Membership membership) {
        try {
            ProducerRecord<String, Membership> record = new ProducerRecord<>(TOPIC, membership);
            producer.send(record, (recordMetadata, e) -> {
                if (e == null) {
                    System.out.println("Record written to partition " + recordMetadata.partition() +
                            ", offset " + recordMetadata.offset() +
                            ", timestamp " + recordMetadata.timestamp());
                } else {
                    System.err.println("An error occurred");
                    e.printStackTrace(System.err);
                }
            });
            producer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
