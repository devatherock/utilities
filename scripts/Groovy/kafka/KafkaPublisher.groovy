@GrabResolver(root = 'http://packages.confluent.io/maven/', name = 'Confluent')
@Grab(group = 'org.apache.kafka', module = 'kafka-clients', version = '2.3.1')
@Grab(group = 'io.confluent', module = 'kafka-avro-serializer', version = '5.2.2')
@Grab(group = 'ch.qos.logback', module = 'logback-classic', version = '1.2.3')
@Grab(group = 'com.jayway.jsonpath', module = 'json-path', version = '2.4.0')

import groovy.transform.Field
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.Decoder
import org.apache.avro.io.DatumReader
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.Schema
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
System.setProperty('java.util.logging.SimpleFormatter.format',
        '%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %4$s %5$s%6$s%n')
@Field static final Logger LOGGER = Logger.getLogger('KafkaPublisher.log')

def cli = new CliBuilder(usage: 'groovy KafkaPublisher.groovy [options]')
cli.c(longOpt: 'command-config', args: 1, argName: 'command-config',
        'File containing config for connecting to kafka, most commonly SSL settings')
cli.b(longOpt: 'bootstrap-servers', args: 1, argName: 'bootstrap-servers', 'Comma separated list of kafka brokers')
cli.f(longOpt: 'file', args: 1, argName: 'file', 'Input file containing records to send to kafka')
cli.t(longOpt: 'topic', args: 1, argName: 'topic', 'Kafka topic to send the records to')
cli.k(longOpt: 'key', args: 1, argName: 'key', '''Key to write the kafka message with.
Should be a Json path representing the property to use as key in case of multiple messages''')
cli.s(longOpt: 'schema', args: 1, argName: 'schema', 'File containing the AVRO schema')
cli.fo(longOpt: 'format', args: 1, argName: 'format', 'The message format. Possible values are AVRO, JSON, TEXT')
cli._(longOpt: 'multiple', args: 0, argName: 'multiple', 'Indicates if multiple messages are present in the input file')

// Parse arguments and look for required parameters common to all formats
def options = cli.parse(args)
if (!(options.t && options.k && (options.c || options.b))) {
    cli.usage()
    System.exit(1)
}

String messageFormat = options.fo
boolean isAvro = (messageFormat == 'AVRO')
if (isAvro && !options.s) {
    LOGGER.severe('Schema file must be specified')
    cli.usage()
    System.exit(1)
}

Properties props = new Properties()
!options.b ?: props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, options.b)
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.name)

if (isAvro) {
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.name)
    props.put(KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY,
            'io.confluent.kafka.serializers.subject.TopicRecordNameStrategy')
} else {
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.name)
}

// Use configuration from file if specified
if (options.c) {
    new File(options.c).withInputStream { stream ->
        props.load(stream)
    }
}

Producer<String, String> producer = new KafkaProducer<>(props)

int count = 0
String topic = options.t
String key = options.k
String inputFileName = options.f

if (options.multiple) {
    new File(inputFileName).each { request ->
        producer.send(new ProducerRecord<String, String>(topic, JsonPath.read(request, key), request))
        count++
    }
} else {
    String request = new File(inputFileName).text
    ProducerRecord producerRecord

    if (isAvro) {
        Schema schema = new Schema.Parser().parse(new File(options.s).text)
        producerRecord = new ProducerRecord<String, String>(topic, JsonPath.read(request, key),
                convertObjectToGenericRecord(request, schema))
    } else {
        producerRecord = new ProducerRecord<String, String>(topic, key, request)
    }

    producer.send(producerRecord)
    count++
}

producer.close()
LOGGER.info("Produced ${count} records")

/**
 * Converts an object into a avro record
 *
 * @param input
 * @param schema
 * @return a generic record
 */
GenericRecord convertObjectToGenericRecord(String input, Schema schema) {
    Decoder decoder = DecoderFactory.get().jsonDecoder(schema, input)
    DatumReader<GenericData.Record> reader = new GenericDatumReader<>(schema)
    return reader.read(null, decoder)
}