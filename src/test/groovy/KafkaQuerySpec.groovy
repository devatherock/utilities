import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Specification

/**
 * Test class for {@code KafkaQuery} script
 */
class KafkaQuerySpec extends Specification {
    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    String topic = 'test-topic'
    String brokers = 'localhost:9092'
    GroovyShell shell = new GroovyShell(new Binding())

    void 'test query message by key'() {
        given:
        File inputFile = temporaryFolder.newFile('input.txt')
        inputFile << 'hello'
        File outputFile = temporaryFolder.newFile('output.txt')

        and:
        shell.run(new File('scripts/Groovy/kafka/KafkaPublisher.groovy'),
                '-k', 'test',
                '-b', brokers,
                '-t', topic,
                '-f', inputFile.getAbsolutePath())

        when:
        shell.run(new File('scripts/Groovy/kafka/KafkaQuery.groovy'),
                '-k', 'test',
                '-b', brokers,
                '-t', topic,
                '-ml', '1',
                '-f', outputFile.getAbsolutePath())

        then:
        outputFile.text == 'hello\n'
    }
}
