@GrabResolver(root = 'https://packages.confluent.io/maven/', name = 'Confluent')
@GrabConfig(systemClassLoader = true)
@Grab(group = 'org.apache.kafka', module = 'kafka-clients', version = '2.8.2')
@Grab(group = 'ch.qos.logback', module = 'logback-classic', version = '1.4.7')
@Grab(group = 'io.confluent', module = 'kafka-avro-serializer', version = '5.5.15')
@Grab(group = 'com.jayway.jsonpath', module = 'json-path', version = '2.8.0')
@Grab(group = 'io.micrometer', module = 'micrometer-registry-jmx', version = '1.5.7')

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.jmx.JmxConfig
import io.micrometer.jmx.JmxMeterRegistry

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndTimestamp
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.serialization.StringDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level

import groovy.cli.commons.CliBuilder
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import groovy.transform.Field

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger
import java.util.regex.Pattern

if (LoggerFactory.getLogger('root') instanceof ch.qos.logback.classic.Logger) {
    LoggerFactory.getLogger('root').setLevel(Level.INFO)
}

System.setProperty('java.util.logging.SimpleFormatter.format',
        '%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %4$s %5$s%6$s%n')
@Field static final Pattern PTRN_EPOCH_TIME = Pattern.compile('[0-9]{1,}')
@Field static final DateTimeFormatter LOCAL_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").
        withZone(ZoneId.systemDefault())
@Field static final Logger LOGGER = Logger.getLogger('KafkaQuery.log')

def cli = new CliBuilder(usage: 'groovy KafkaQuery.groovy [options]', width: 100)
cli.b(longOpt: 'bootstrap-servers', args: 1, argName: 'bootstrap-servers', 'Comma separated list of kafka brokers')
cli.g(longOpt: 'group', args: 1, argName: 'group', defaultValue: 'KafkaQuery', 'Consumer group id')
cli.f(longOpt: 'file', args: 1, argName: 'file', 'Output file to write matched records to')
cli.t(longOpt: 'topic', args: 1, argName: 'topic', required: true, 'Kafka topic to consume the records from')
cli.k(longOpt: 'keys', args: 1, argName: 'keys', 'Comma separated list of keys to match')
cli.ik(longOpt: 'include-key', args: 0, argName: 'include-key', 'Indicates whether to include the key in the output')
cli.p(longOpt: 'property', args: 1, argName: 'property', 'Json path property to query for')
cli.v(longOpt: 'property-value', args: 1, argName: 'property-value', 'Property value to query for')
cli.r(longOpt: 'registry', args: 1, argName: 'registry', 'Schema registry URL')
cli.c(longOpt: 'command-config', args: 1, argName: 'command-config',
        'File containing config for connecting to kafka, most commonly SSL settings')
cli.pt(longOpt: 'poll-timeout', args: 1, argName: 'poll-timeout', 'Poll timeout, in milliseconds')
cli.s(longOpt: 'start-time', args: 1, argName: 'start-time', 'Start time from which to look for messages')
cli.e(longOpt: 'end-time', args: 1, argName: 'end-time', 'End time upto which to look for messages')
cli.so(longOpt: 'start-offset', args: 1, argName: 'start-offset', 'Start offset from which to look for messages')
cli.cl(longOpt: 'consume-limit', args: 1, argName: 'consume-limit', 'Maximum number of messages to consume')
cli.ml(longOpt: 'match-limit', args: 1, argName: 'match-limit', 'Maximum number of matches to find')
cli._(longOpt: 'avro', args: 0, argName: 'avro', 'Flag to indicate that the message format is avro')
cli._(longOpt: 'debug', args: 0, argName: 'debug', 'Enables debug logs')

def options = cli.parse(args)
if (!options) {
    System.exit(1)
} else if (!(options.c || options.b)) {
    cli.usage()
    System.exit(1)
}

if (options.debug) {
    Logger root = Logger.getLogger('')
    root.setLevel(java.util.logging.Level.FINE)
    root.getHandlers().each { it.setLevel(java.util.logging.Level.FINE) }
}

@Field LinkedBlockingQueue kafkaMessageQueue = new LinkedBlockingQueue(1000)
@Field File outputFile = null
@Field long startTime
@Field Long startOffset
@Field AtomicInteger matchedCount = new AtomicInteger(0)
@Field MeterRegistry meterRegistry = new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM)
@Field int maxMessagesToConsume
@Field int maximumMatches
@Field Map endOffsetsMap = [:]
@Field boolean includeKey

startTime = options.s ? parseTimestamp(options.s) : 0
startOffset = options.so ? Long.parseLong(options.so) : null
includeKey = options.ik
maxMessagesToConsume = options.cl ? Integer.parseInt(options.cl) : Integer.MAX_VALUE
maximumMatches = options.ml ? Integer.parseInt((options.ml)) : Integer.MAX_VALUE
if (options.f) {
    outputFile = new File(options.f)
}

// Write kafka messages to output in a single thread
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
final long endTime = options.e ? parseTimestamp(options.e) : processStartTime
final int giveUp = 10
final int pollTimeout = options.pt ? Integer.parseInt(options.pt) : 5000
final Duration pollDuration = Duration.of(pollTimeout, ChronoUnit.MILLIS)
AtomicInteger consumedCount = new AtomicInteger(0)

Gauge.builder("counter.messages.all", consumedCount, { it.doubleValue() })
        .register(meterRegistry)
Gauge.builder("counter.messages.matched", matchedCount, { it.doubleValue() })
        .register(meterRegistry)

// Initialize the consumers
List consumers = createConsumers(options.t, props)
LOGGER.fine({ "End offsets: ${endOffsetsMap}".toString() })

if (endOffsetsMap) {
    // Execute each consumer in its own thread
    List<Thread> threads = []
    consumers.each { consumer ->
        threads.add(Thread.start {
            int noRecordsCount = 0
            long currentMessageTimestamp = 0
            ConsumerRecord currentRecord

            while (matchedCount.get() < maximumMatches && consumedCount.get() < maxMessagesToConsume &&
                    noRecordsCount < giveUp && currentMessageTimestamp <= endTime && (!currentRecord ||
                    (currentRecord.offset() + 1) < endOffsetsMap[currentRecord.partition()])) {
                ConsumerRecords consumerRecords = consumer.poll(pollDuration)

                if (consumerRecords.count() == 0) {
                    LOGGER.fine('No records found')
                    noRecordsCount++
                } else {
                    noRecordsCount = 0 // Reset each time we find records
                    consumerRecords.each { record ->
                        currentRecord = record
                        LOGGER.fine(
                                { "Current record: ${currentRecord.partition()}:${currentRecord.offset()}".toString() })
                        consumedCount.incrementAndGet()
                        Counter.builder('counter.messages')
                                .tag('partition', String.valueOf(record.partition()))
                                .register(meterRegistry)
                                .increment()

                        if (ids) {
                            if (ids.contains(record.key)) {
                                writeOutput(record)
                            }
                        } else if (propertyValues) {
                            if (record.value) { // To handle messages with null body
                                String recordValue = record.value.toString()

                                if (propertyJsonPath == '__value') {
                                    if (propertyValues.any { recordValue.contains(it) }) {
                                        writeOutput(record)
                                    }
                                } else {
                                    try {
                                        if (propertyValues.contains(JsonPath.read(recordValue, propertyJsonPath).toString())) {
                                            writeOutput(record)
                                        }
                                    } catch (PathNotFoundException exception) {
                                        LOGGER.fine(exception.getMessage())
                                    }
                                }
                            }
                        } else {
                            writeOutput(record)
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
}

isRunning = false
messageWriteThread.join()
LOGGER.info "Consumed count: ${consumedCount}, Matched count: ${matchedCount}, Time taken: ${(System.currentTimeMillis() - processStartTime) / 1000} seconds"

/**
 * Parses timestamps specified in different formats
 *
 * @param date
 * @return an epoch timestamp
 */
Long parseTimestamp(String timestamp) {
    Long parsedTimestamp = 0

    if (PTRN_EPOCH_TIME.matcher(timestamp).matches()) {
        parsedTimestamp = Long.parseLong(timestamp)
    } else {
        parsedTimestamp = Instant.from(LOCAL_TIME_FORMATTER.parse(timestamp)).toEpochMilli()
    }

    return parsedTimestamp
}

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

    return consumers.findAll { !it.closed }
}

/**
 * Assign a partition to the consumer
 *
 * @param consumer
 * @param partitionInfo
 */
void initializeConsumer(KafkaConsumer consumer, PartitionInfo partitionInfo) {
    List<TopicPartition> assignment = [new TopicPartition(partitionInfo.topic(), partitionInfo.partition())]

    Map<TopicPartition, Long> startOffsets = [:]
    Map<TopicPartition, Long> endOffsets = consumer.endOffsets(assignment)
    endOffsets.each { key, value ->
        endOffsetsMap[key.partition()] = value
    }

    // Start offset within the time range
    if (startTime > 0) {
        Map<TopicPartition, Long> timestampsToSearch = assignment.collectEntries { [it, startTime] }
        Map<TopicPartition, OffsetAndTimestamp> timeOffsets = consumer.offsetsForTimes(timestampsToSearch)
        LOGGER.fine({ "Time based offsets: ${timeOffsets}".toString() })

        timeOffsets.each { partition, offsetAndTime ->
            if (offsetAndTime) {
                startOffsets[partition] = offsetAndTime.offset()
            }
        }

        // Don't consume from partitions that don't have messages newer than start time
        endOffsets.findAll { key, value -> !startOffsets[key] }.each { partition, offset ->
            assignment.remove(partition)
            endOffsetsMap.remove(partition.partition())
        }
    }

    // Start offset specified
    if (startOffset) {
        startOffsets[assignment[0]] = startOffset
    }

    // Start offset based on limit
    if (!startOffsets && maxMessagesToConsume < Integer.MAX_VALUE) {
        assignment.each { partition ->
            startOffsets[partition] = endOffsets[partition] - maxMessagesToConsume
        }
    }

    // Assignment will be empty if start time is specified and no messages newer than the start time exist
    if (assignment) {
        consumer.assign(assignment)

        if (startOffsets) {
            startOffsets.each { partition, offset ->
                LOGGER.fine({ "Seeking partition ${partition.partition()} to ${offset}".toString() })
                consumer.seek(partition, offset)
            }
        } else {
            consumer.seekToBeginning(assignment)
        }
    } else {
        consumer.close()
    }
}

/**
 * Writes the matched record to file/console
 *
 * @param record
 */
def writeOutput(ConsumerRecord record) {
    matchedCount.incrementAndGet()
    LOGGER.fine({ "Matched record: ${record.partition()}:${record.offset()}, timestamp: ${record.timestamp()}".toString() })

    if (includeKey) {
        kafkaMessageQueue.put("${record.key},${record.value}")
    } else {
        // Using GString to prevent null values from causing NullPointerException in LinkedBlockingQueue
        kafkaMessageQueue.put("${record.value}")
    }
}