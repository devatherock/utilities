import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka

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

    String topic = 'test-topic'
    GroovyShell shell = new GroovyShell(new Binding())

    void 'test query message by key'() {
        given:
        File inputFile = temporaryFolder.newFile('input.txt')
        inputFile << 'hello'
        File outputFile = temporaryFolder.newFile('output.txt')

        and:
        shell.run(new File('scripts/Groovy/kafka/KafkaPublisher.groovy'),
                '-k', 'test',
                '-b', embeddedKafka.getBrokersAsString(),
                '-t', topic,
                '-f', inputFile.getAbsolutePath())

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
