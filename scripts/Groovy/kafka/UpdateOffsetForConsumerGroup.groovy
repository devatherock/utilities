@Grab(group = 'org.apache.kafka', module = 'kafka-clients', version = '2.5.0')
@Grab(group = 'ch.qos.logback', module = 'logback-classic', version = '1.2.3')

import groovy.transform.Field
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.logging.Logger
import ch.qos.logback.classic.Level

LoggerFactory.getLogger('org.apache.kafka').setLevel(Level.INFO)
System.setProperty('java.util.logging.SimpleFormatter.format',
        '%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %4$s %5$s%6$s%n');
@Field Logger logger = Logger.getLogger('UpdateOffsetForConsumerGroup.log')

def cli = new CliBuilder(usage: 'groovy UpdateOffsetForConsumerGroup.groovy [options]', width: 100)
cli.b(longOpt: 'bootstrap-servers', args: 1, argName: 'bootstrap-servers',
        'Comma separated list of kafka brokers')
cli.g(longOpt: 'group', args: 1, argName: 'group', 'Consumer group id')
cli.t(longOpt: 'topic', args: 1, argName: 'topic', required: true,
        'Kafka topic for which to update the offset')
cli.c(longOpt: 'command-config', args: 1, argName: 'command-config',
        'File containing config for connecting to kafka, most commonly SSL settings')
cli.p(longOpt: 'partition', args: 1, argName: 'partition',
        'Partition for which to update the offset')
cli.o(longOpt: 'offset', args: 1, argName: 'offset', required: true,
        'New offset for the partition. A number or it can be earliest/latest')
cli.pt(longOpt: 'poll-timeout', args: 1, argName: 'poll-timeout', defaultValue: '15000',
        'Poll timeout, in milliseconds')

def options = cli.parse(args)
if (!options || !(options.c || (options.b && options.g))) {
    cli.usage()
    System.exit(1)
}

Properties props = new Properties()

// Use configuration from file if specified
if (options.c) {
    new File(options.c).withInputStream { stream ->
        props.load(stream)
    }
}
!options.b ?: props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, options.b)
!options.g ?: props.put(ConsumerConfig.GROUP_ID_CONFIG, options.g)
props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName())
props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName())
props.put('max.poll.records', 1)
props.put('enable.auto.commit', false)

// Create the consumer using props.
final Consumer<Long, String> consumer = new KafkaConsumer<>(props)
String topic = options.t
String offset = options.o

long pollTimeout = Long.parseLong(options.pt)
consumer.subscribe([topic])

List<TopicPartition> partitions = []
if (options.p) {
    partitions.add(new TopicPartition(topic, Integer.parseInt(options.p)))
} else {
    List<PartitionInfo> partitionInfoList = consumer.partitionsFor(topic)
    partitionInfoList.each { partitionInfo ->
        partitions.add(new TopicPartition(topic, partitionInfo.partition()))
    }
}
consumer.poll(Duration.of(pollTimeout, ChronoUnit.MILLIS))

if (offset == 'earliest') {
    consumer.seekToBeginning(partitions)
} else if (offset == 'latest') {
    consumer.seekToEnd(partitions)
} else {
    partitions.each { partition ->
        consumer.seek(partition, Long.parseLong(offset))
        logger.info("Updated offset to ${offset} in partition ${partition.partition} of topic ${topic} for group ${options.g}")
    }
}

consumer.poll(Duration.of(pollTimeout, ChronoUnit.MILLIS))
consumer.commitSync()
consumer.close()