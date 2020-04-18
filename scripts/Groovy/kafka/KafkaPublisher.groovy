import groovy.transform.Field
@Grab(group='org.apache.kafka', module='kafka-clients', version='2.3.1')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.2.3')
@Grab(group='com.jayway.jsonpath', module='json-path', version='2.4.0')

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord

import groovy.cli.commons.CliBuilder
import com.jayway.jsonpath.JsonPath
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level

import java.util.logging.Logger

LoggerFactory.getLogger('root').setLevel(Level.INFO)
System.setProperty('java.util.logging.SimpleFormatter.format', '%5$s%n')
@Field static final Logger LOGGER = Logger.getLogger('KafkaPublisher.log')

def cli = new CliBuilder(usage: 'groovy KafkaPublisher.groovy [options]')
cli.c(longOpt: 'command-config', args: 1, argName: 'command-config',
        'File containing config for connecting to kafka, most commonly SSL settings')
cli.b(longOpt: 'bootstrap-servers', args: 1, argName: 'bootstrap-servers', 'Comma separated list of kafka brokers')
cli.f(longOpt: 'file', args: 1, argName: 'file', 'Input file containing records to send to kafka')
cli.t(longOpt: 'topic', args: 1, argName: 'topic', 'Kafka topic to send the records to')
cli.k(longOpt: 'key', args: 1, argName: 'key', '''Key to write the kafka message with.
Should be a Json path representing the property to use as key in case of multiple messages''')
cli._(longOpt: 'multiple', args: 0, argName: 'multiple', 'Indicates if multiple messages are present in the input file')

def options = cli.parse(args)
if (!(options.t && options.k && (options.c || options.b ))) {
    cli.usage()
    System.exit(1)
}

Properties props = new Properties()
!options.b ?: props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, options.b)
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.name)
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.name)

// Use configuration from file if specified
if(options.c) {
    new File(options.c).withInputStream { stream ->
        props.load(stream)
    }
}

Producer<String, String> producer = new KafkaProducer<>(props)

int count = 0
String topic = options.t
String key = options.k

if(options.multiple) {
    new File(options.f).each { request ->
        producer.send(new ProducerRecord<String, String>(topic, JsonPath.read(request, key), request))
        count++
    }
}
else {
    producer.send(new ProducerRecord<String, String>(topic, key, new File(options.f).text))
    count++
}

LOGGER.info ("Produced ${count} records")
producer.close()