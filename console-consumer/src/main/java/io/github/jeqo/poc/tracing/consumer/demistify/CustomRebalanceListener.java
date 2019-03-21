package io.github.jeqo.poc.tracing.consumer.demistify;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomRebalanceListener implements ConsumerRebalanceListener {
  private static final Logger LOG = LoggerFactory.getLogger(CustomRebalanceListener.class);
  private final KafkaConsumer kafkaConsumer;
  // contains processed record(s) offset in current poll, should be cleared after all poll is
  // processed
  private Map<TopicPartition, OffsetAndMetadata> processedRecordsOffsets = Collections.emptyMap();

  CustomRebalanceListener(KafkaConsumer kafkaConsumer) {
    this.kafkaConsumer = kafkaConsumer;
  }

  public void addOffset(String topic, int partition, long offset) {
    processedRecordsOffsets.put(
        new TopicPartition(topic, partition), new OffsetAndMetadata(offset, "commit"));
  }

  @Override
  public void onPartitionsRevoked(Collection<TopicPartition> partitions) {

    final String partitionsRevoked =
        partitions.stream()
            .map(topicPartition -> Collections.singletonList(topicPartition.partition()).toString())
            .collect(Collectors.joining(","));
    LOG.info("Following partitions revoked: {}", partitionsRevoked);

    final String processedOffsetsBeforeRevoke =
        processedRecordsOffsets.keySet().stream()
            .map(topicPartition -> Collections.singletonList(topicPartition.partition()).toString())
            .collect(Collectors.joining(","));
    LOG.info("Following partitions will be committed: {}", processedOffsetsBeforeRevoke);

    // commit sync all processed records offsets
    kafkaConsumer.commitSync(processedRecordsOffsets);
    clearProcessedRecordsOffsets();
  }

  @Override
  public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
    final String partitionsAssigned =
        partitions.stream()
            .map(topicPartition -> Collections.singletonList(topicPartition.partition()).toString())
            .collect(Collectors.joining(","));
    LOG.info("Following partitions assigned: {}", partitionsAssigned);
  }

  // clear when partition revoke and each new poll
  void clearProcessedRecordsOffsets() {
    processedRecordsOffsets.clear();
  }
}
