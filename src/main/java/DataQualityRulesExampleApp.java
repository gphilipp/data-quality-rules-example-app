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
import java.util.Scanner;

public class DataQualityRulesExampleApp {

    private static final String TOPIC = "memberships";
    private static final String USER_SCHEMA = "memberships.avsc";


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

        try (KafkaProducer<String, Membership> producer = new KafkaProducer<>(properties)) {
            Membership membership = Membership.newBuilder()
                    .setEmail("joe@doe.com")
                    .setSsn("856-45-6789")
                    .setStartDate(LocalDate.of(2023, Month.JANUARY, 1))
                    .setEndDate(LocalDate.of(2023, Month.DECEMBER, 31))
                    .build();
            ProducerRecord<String, Membership> record = new ProducerRecord<>(TOPIC, membership);
            producer.send(record, (recordMetadata, e) -> {
                if (e == null) {
                    System.out.println("Record written to offset " +
                            recordMetadata.offset() + " timestamp " +
                            recordMetadata.timestamp());
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

    private static String loadSchemaFromFile(String filepath) {
        InputStream inputStream = DataQualityRulesExampleApp.class.getResourceAsStream(filepath);
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
