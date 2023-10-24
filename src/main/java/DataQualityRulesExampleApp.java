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

    public static void main(String[] args) throws IOException {
        final Properties properties = loadProps();
        KafkaProducer<String, Membership> producer = new KafkaProducer<>(properties);
        Membership membership = Membership.newBuilder()
                .setEmail("john.doe")
                .setSsn("fizzbuzz")
                .setStartDate(LocalDate.of(2023, Month.JANUARY, 1))
                .setEndDate(LocalDate.of(1970, Month.JANUARY, 1))
                .build();
        ProducerRecord<String, Membership> record = new ProducerRecord<>(TOPIC, membership);
        producer.send(record, (recordMetadata, e) -> {
            if (e == null) {
                System.out.println("Record written to partition " + recordMetadata.partition() +
                        ", offset " + recordMetadata.offset() +
                        ", timestamp " + recordMetadata.timestamp());
            } else {
                e.printStackTrace(System.err);
            }
        });
        producer.flush();
    }

    private static Properties loadProps() throws IOException {
        if (!Files.exists(Paths.get("client.properties"))) {
            throw new IOException("client.properties" + " not found.");
        }
        final Properties props = new Properties();
        try (InputStream inputStream = new FileInputStream("client.properties")) {
            props.load(inputStream);
        }
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION, true);
        props.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, false);
        return props;
    }

}
