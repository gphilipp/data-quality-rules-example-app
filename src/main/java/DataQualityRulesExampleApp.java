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

    public static void main(String[] args) throws IOException, InterruptedException {
        final Properties props = loadClientProperties();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION, true);
        props.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, false);
        KafkaProducer<String, Membership> producer = new KafkaProducer<>(props);
        Membership membership = Membership.newBuilder()
                .setStartDate(LocalDate.of(2023, Month.JANUARY, 1))
                .setEndDate(LocalDate.of(1970, Month.JANUARY, 1))
                .setEmail("john.doe")
                .setSsn("fizzbuzz")
                .build();
        ProducerRecord<String, Membership> record = new ProducerRecord<>("memberships", membership);
        try {
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
        } catch (Exception e) {
            Thread.sleep(10000);
            System.err.println("Error");
        }
    }

    private static Properties loadClientProperties() throws IOException {
        if (!Files.exists(Paths.get("client.properties"))) {
            throw new IOException("client.properties" + " not found.");
        }
        final Properties props = new Properties();
        try (InputStream inputStream = new FileInputStream("client.properties")) {
            props.load(inputStream);
        }
        return props;
    }

}
