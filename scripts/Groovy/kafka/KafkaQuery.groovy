@GrabResolver(root = 'http://packages.confluent.io/maven/', name = 'Confluent')
@Grab(group = 'org.apache.kafka', module = 'kafka-clients', version = '2.2.1-cp1')
@Grab(group = 'ch.qos.logback', module = 'logback-classic', version = '1.2.3')
@Grab(group = 'io.confluent', module = 'kafka-avro-serializer', version = '5.2.2')
@Grab(group = 'com.jayway.jsonpath', module = 'json-path', version = '2.4.0')

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.serialization.StringDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level

import groovy.cli.commons.CliBuilder
import com.jayway.jsonpath.JsonPath
import groovy.transform.Field

import java.time.temporal.ChronoUnit
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

LoggerFactory.getLogger('root').setLevel(Level.INFO)
System.setProperty('java.util.logging.SimpleFormatter.format',
        '%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %4$s %5$s%6$s%n')
@Field Logger logger = Logger.getLogger('KafkaQuery.log')

def cli = new CliBuilder(usage: 'groovy KafkaQuery.groovy [options]', width: 100)
cli.b(longOpt: 'bootstrap-servers', args: 1, argName: 'bootstrap-servers', 'Comma separated list of kafka brokers')
cli.g(longOpt: 'group', args: 1, argName: 'group', 'Consumer group id')
cli.f(longOpt: 'file', args: 1, argName: 'file', 'Output file to write matched records to')
cli.t(longOpt: 'topic', args: 1, argName: 'topic', 'Kafka topic to consume the records from')
cli.k(longOpt: 'keys', args: 1, argName: 'keys', 'Comma separated list of keys to match')
cli.p(longOpt: 'property', args: 1, argName: 'property', 'Json path property to query for')
cli.v(longOpt: 'property-value', args: 1, argName: 'property-value', 'Property value to query for')
cli.r(longOpt: 'registry', args: 1, argName: 'registry', 'Schema registry URL')
cli.c(longOpt: 'command-config', args: 1, argName: 'command-config',
        'File containing config for connecting to kafka, most commonly SSL settings')
cli.pt(longOpt: 'poll-timeout', args: 1, argName: 'poll-timeout', 'Poll timeout, in milliseconds')
cli.s(longOpt: 'start-time', args: 1, argName: 'start-time', 'Start time from which to look for messages')
cli.e(longOpt: 'end-time', args: 1, argName: 'end-time', 'End time upto which to look for messages')
cli.l(longOpt: 'limit', args: 1, argName: 'limit', 'Maximum of messages to consume, to check for a match')
cli._(longOpt: 'avro', args: 0, argName: 'avro', 'Flag to indicate that the message format is avro')
cli._(longOpt: 'debug', args: 0, argName: 'debug', 'Enables debug logs')

def options = cli.parse(args)
if (!(options.t && (options.k || (options.p && options.v)) && (options.c || (options.b && options.g)))) {
    cli.usage()
    System.exit(1)
}

if (options.debug) {
    Logger root = Logger.getLogger('')
    root.setLevel(java.util.logging.Level.FINE)
    root.getHandlers().each { it.setLevel(java.util.logging.Level.FINE) }
}

// Write kafka messages to output in a single thread
@Field LinkedBlockingQueue kafkaMessageQueue = new LinkedBlockingQueue(1000)
@Field File outputFile = null
if (options.f) {
    outputFile = new File(options.f)
}

boolean isRunning = true
Thread messageWriteThread = Thread.start {
    def messagesToWrite = []

    while (isRunning || kafkaMessageQueue.size() > 0) {
        kafkaMessageQueue.drainTo(messagesToWrite)

        // In case the queue is empty, add a wait time to reduce CPU usage
        if (!messagesToWrite) {
            def message = kafkaMessageQueue.poll(1000, TimeUnit.MILLISECONDS)
            if (message) {
                messagesToWrite.add(message)
            }
        }

        messagesToWrite.each { kafkaMessage ->
            if (outputFile) {
                outputFile << "${kafkaMessage}${System.properties['line.separator']}"
            } else {
                println kafkaMessage
            }
        }
        messagesToWrite.clear()
    }
}


Properties props = new Properties()
!options.b ?: props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, options.b)
!options.g ?: props.put(ConsumerConfig.GROUP_ID_CONFIG, options.g)
props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName())
props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, 'earliest')
!options.r ?: props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, options.r)
options.avro ? props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName()) :
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName())

// Use configuration from file if specified
if (options.c) {
    new File(options.c).withInputStream { stream ->
        props.load(stream)
    }
}

final def ids = options.k ? options.k.split('[,]') as List : false
final String propertyJsonPath = options.p
final def propertyValues = options.v ? options.v.split('[,]') as List : null
final long processStartTime = System.currentTimeMillis()
final long endTime = options.e ? Long.parseLong(options.e) : processStartTime
final long startTime = options.s ? Long.parseLong(options.s) : 0
final int giveUp = 10
final int pollTimeout = options.pt ? Integer.parseInt(options.pt) : 5000
final Duration pollDuration = Duration.of(pollTimeout, ChronoUnit.MILLIS)
@Field AtomicInteger matchedCount = new AtomicInteger(0)
AtomicInteger consumedCount = new AtomicInteger(0)

// Initialize the consumers
@Field int maxMessagesToConsume
maxMessagesToConsume = options.l ? Integer.parseInt(options.l) : 0
List consumers = createConsumers(options.t, props)

// Execute each consumer in its own thread
List<Thread> threads = []
consumers.each { consumer ->
    threads.add(Thread.start {
        int noRecordsCount = 0
        long currentMessageTimestamp = 0

        while (noRecordsCount < giveUp && currentMessageTimestamp <= endTime) {
            ConsumerRecords consumerRecords = consumer.poll(pollDuration)

            if (consumerRecords.count() == 0) {
                logger.fine('No records found')
                noRecordsCount++
            } else {
                noRecordsCount = 0 // Reset each time we find records
                consumerRecords.each { record ->
                    consumedCount.incrementAndGet()

                    if (record.timestamp() >= startTime) {
                        if (ids) {
                            if (ids.contains(record.key)) {
                                writeOutput(record)
                            }
                        } else if (propertyValues.contains(JsonPath.read(record.value.toString(), propertyJsonPath))) {
                            writeOutput(record)
                        }
                    }
                    currentMessageTimestamp = record.timestamp()
                }
            }
        }
        consumer.close()
    })
}

// Wait for all consumers to exit
threads.each { it.join() }
isRunning = false
messageWriteThread.join()
logger.info "Consumed count: ${consumedCount}, Matched count: ${matchedCount}, Time taken: ${(System.currentTimeMillis() - processStartTime) / 1000} seconds"


/**
 * Creates as many consumers as the number of partitions in the topic
 *
 * @param topicName
 * @param config
 * @return a list of consumers
 */
List<KafkaConsumer> createConsumers(String topicName, Properties config) {
    // Initialize the consumers
    KafkaConsumer consumer = new KafkaConsumer(config)
    List consumers = [consumer]

    // Create one consumer per partition for the rest of the consumers
    List<PartitionInfo> partitions = consumer.partitionsFor(topicName)
    initializeConsumer(consumer, partitions.last())

    for (int index = 0; index < partitions.size() - 1; index++) {
        consumer = new KafkaConsumer(config)
        consumers.add(consumer)
        initializeConsumer(consumer, partitions[index])
    }

    return consumers
}

/**
 * Assign a partition to the consumer
 *
 * @param consumer
 * @param partitionInfo
 */
void initializeConsumer(KafkaConsumer consumer, PartitionInfo partitionInfo) {
    List<TopicPartition> assignment = [new TopicPartition(partitionInfo.topic(), partitionInfo.partition())]
    consumer.assign(assignment)

    if (maxMessagesToConsume > 0) {
        Map<TopicPartition, Long> endOffsets = consumer.endOffsets(assignment)

        endOffsets.each { key, value ->
            logger.fine("Seeking ${key} to ${value - maxMessagesToConsume}")
            consumer.seek(key, value - maxMessagesToConsume)
        }
    } else {
        consumer.seekToBeginning(assignment)
    }
}

/**
 * Writes the matched record to file/console
 *
 * @param record
 */
def writeOutput(ConsumerRecord record) {
    matchedCount.incrementAndGet()
    kafkaMessageQueue.put(record.value)
}