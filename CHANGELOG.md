# Changelog

## 2021-02-05
### Added
- feat(AddLabelToIssues): Script to add a label to git issues with a specific string in title

## 2021-01-29
### Added
- feat(KafkaPublisher): Ability to produce multiple avro messages

## 2021-01-26
### Changed
- [#9](https://github.com/devatherock/utilities/issues/9): Retained only consumers that are open

## 2020-12-18
### Added
- Script to extract a specific column from a CSV file
- Script to deduplicate records in a file

## 2020-11-23
### Added
- [#7](https://github.com/devatherock/utilities/issues/7): KafkaPublisher - Counter to keep track of number of messages 
published
- Script to base64 encode and decode

## 2020-07-01
### Changed
- UpdateOffsetForConsumerGroup - Disabled auto commit. Set `max.poll.records` to 1

## 2020-06-29
### Added
- Script to update offset of a kafka consumer group

## 2020-06-09
### Added
- KafkaQuery - A required null check

## 2020-06-05
### Added
- CopyIssue - Copies a github issue from one repository to another
- KafkaQuery - An option to limit number of results

### Changed
- [Issue 8](https://github.com/devatherock/utilities/issues/8): When start time is specified, consumed only from offsets
that are newer instead of consuming from the beginning

## 2020-06-03
### Changed
- KafkaQuery - Decreased total runtime by consuming only up to whichever was the last offset at the start of execution

## 2020-06-01
### Added
- `--debug` flag to view debug logs
- `--limit` parameters to specify maximum number of messages to consume, starting from the last

### Changed
- Default value of poll timeout to `5000` milliseconds from `1000`, to account for slow networks

## 2020-05-27
### Added
- KafkaQuery - Ability to match against multiple values for a JSON path variable

## 2020-05-20
### Added
- KafkaPublisher - Throttling to not produce messages too fast so as to not run out of memory

### Changed
- [Issue 4](https://github.com/devatherock/utilities/issues/4): Wrote consumed messages to file in a single thread to
prevent messages from getting mixed up

## 2020-04-26
### Added
- [Issue 3](https://github.com/devatherock/utilities/issues/3): Ability to produce avro kafka messages

## 2020-04-18
### Added
- KafkaQuery - Ability to get kafka messages between a specific start time and end time
- KafkaPublisher - Publishes the content of input file as a single message or publishes each line from the file, which
should be valid JSON, as a message

## 2020-03-06
### Added
- Script to generate an access token for Google APIs

## 2020-03-01
### Added
- Script to look for a kafka message by key or by the value of a field in case of JSON/AVRO messages
