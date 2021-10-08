import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils

import spock.lang.Specification

/**
 * Test class for {@code KafkaQuery} script
 */
@SpringBootTest(classes = [TestConfig])
@EmbeddedKafka(topics = ['test-topic'], controlledShutdown = true, partitions = 3)
class KafkaQuerySpec extends Specification {
    @Autowired
    EmbeddedKafkaBroker embeddedKafka

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    KafkaTemplate kafkaTemplate
    String topic = 'test-topic'
    GroovyShell shell = new GroovyShell(new Binding())

    void setup() {
        kafkaTemplate = initializeKafkaTemplate()
    }

    KafkaTemplate initializeKafkaTemplate() {
        new KafkaTemplate<String, String>(new DefaultKafkaProducerFactory<>(
                KafkaTestUtils.producerProps(embeddedKafka),
                new StringSerializer(),
                new StringSerializer()
        ))
    }

    void 'test query message by key'() {
        given:
        kafkaTemplate.send(new ProducerRecord(
                topic,
                null,
                'test',
                'hello'
        ))
        File outputFile = temporaryFolder.newFile('output.txt')

        when:
        shell.run(new File('scripts/Groovy/kafka/KafkaQuery.groovy'),
                '-k', 'test',
                '-b', embeddedKafka.getBrokersAsString(),
                '-t', topic,
                '-ml', '1',
                '-f', outputFile.getAbsolutePath())

        then:
        outputFile.text == 'hello\n'
    }

    @Configuration
    static class TestConfig {
    }
}
