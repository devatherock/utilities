import groovy.json.JsonSlurper

import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Specification

/**
 * Test class for {@code KafkaQuery} script
 */
class KafkaQuerySpec extends Specification {
    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    String brokers = 'localhost:9092'
    GroovyShell shell = new GroovyShell(new Binding())

    void 'test query message by key'() {
        given:
        String topic = 'test-topic'
        String key = "test-${System.currentTimeMillis()}"

        and:
        File inputFile = temporaryFolder.newFile('input.txt')
        inputFile << 'hello'
        File outputFile = temporaryFolder.newFile('output.txt')

        and:
        shell.run(new File('scripts/Groovy/kafka/KafkaPublisher.groovy'),
                '-k', key,
                '-b', brokers,
                '-t', topic,
                '-f', inputFile.getAbsolutePath())

        when:
        shell.run(new File('scripts/Groovy/kafka/KafkaQuery.groovy'),
                '-k', key,
                '-b', brokers,
                '-t', topic,
                '-ml', '1',
                '-f', outputFile.getAbsolutePath())

        then:
        outputFile.text == 'hello\n'
    }

    void 'test query avro message by key'() {
        given:
        String topic = 'test-topic-2'
        String key = "test2-${System.currentTimeMillis()}"
        String message = '{"body":"hello"}'

        and:
        File inputFile = temporaryFolder.newFile('input-avro.txt')
        inputFile << message
        File outputFile = temporaryFolder.newFile('output-avro.txt')

        and:
        File configFile = temporaryFolder.newFile('config.properties')
        configFile << 'schema.registry.url=http://localhost:8081\n'
        configFile << 'value.subject.name.strategy=io.confluent.kafka.serializers.subject.TopicRecordNameStrategy'

        and:
        shell.run(new File('scripts/Groovy/kafka/KafkaPublisher.groovy'),
                '-k', key,
                '-b', brokers,
                '-t', topic,
                '-fo', 'AVRO',
                '-s', new File(this.class.classLoader.getResource('Example.avsc').toURI()).getAbsolutePath(),
                '-c', configFile.getAbsolutePath(),
                '-f', inputFile.getAbsolutePath())

        when:
        shell.run(new File('scripts/Groovy/kafka/KafkaQuery.groovy'),
                '-k', key,
                '-b', brokers,
                '-t', topic,
                '-ml', '1',
                '--avro',
                '-c', configFile.getAbsolutePath(),
                '-f', outputFile.getAbsolutePath())

        then:
        new JsonSlurper().parseText(outputFile.text) == ['body': 'hello']
    }
}
