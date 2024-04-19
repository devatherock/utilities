# Changelog

## 2024-04-19
### Added
- `upper` function to `CsvQuery`

## 2024-04-08
### Added
- `concat` and `convert_date` functions to `CsvQuery`

### Changed
- Retained casing of output columns in `CsvQuery`

## 2024-03-27
### Added
- Script to find lines that are different between 2 files

## 2024-03-24
### Added
- `from` clause to the query in `CsvQuery`

### Changed
- Upgraded `kafka-avro-serializer` to `7.6.0`

## 2024-03-23
### Changed
- `CsvQuery` to improve speed through asynchronous processing

## 2024-03-22
### Added
- Filtering by `like` condition to `CsvQuery`

## 2024-03-20
### Added
- Filtering to `CsvQuery`

## 2024-03-17
### Added
- Script to query a CSV file

## 2023-11-20
### Added
- Script to create a git tag

## 2023-07-19
### Changed
- Upgraded `kafka-avro-serializer` to `7.4.0`

## 2023-07-18
### Added
- Tests for avro kafka producer and consumer

## 2023-07-13
### Added
- Script to backup files/folders

## 2023-06-16
### Added
- Script to trim trailing spaces

## 2023-05-22
### Changed
- EnableBranchProtection: Used `1.4.7` version of `logback-classic`

## 2023-05-10
### Changed
- Used `1.4.7` version of `logback-classic` to get Slf4j logs working in Groovy 4.x. Refer [this issue](https://issues.apache.org/jira/browse/GROOVY-11049) for more info
- Upgraded gradle to 7.6.1
- Upgraded spotless to 6.18.0

## 2023-03-08
### Fixed
- KafkaQuery - Corrected JSON value matching when the field value is not a String

## 2022-08-06
### Added
- Script to convert `FundsIndia` capital gains statement to `ClearTax` format

## 2022-06-23
### Added
- EnableBranchProtection: Ability to specify required review count

## 2022-04-29
### Added
- Groovy 4 compatibility

## 2022-03-18
### Added
- feat(KafkaQuery): Ability to start querying from a certain offset

## 2021-12-01
### Added
- EnableBranchProtection: Ability to specify status checks

## 2021-11-24
### Fixed
- KafkaQuery: Exception when querying a message with null body

## 2021-10-08
### Added
- test(KafkaQuery): An unit test
- Script to enable branch protection on a Git Repo
- CI pipeline for unit testing

## 2021-10-06
### Added
- feat(UpdateOffsetForConsumerGroup): Ability to seek to earliest or latest offset
- feat(KafkaQuery): Metrics to track progress of kafka consumption([#6](https://github.com/devatherock/utilities/issues/6))

## 2021-08-20
### Added
- feat(Base64): Ability to write decoded output to a file
- feat(KafkaQuery): Ability to include key in the output and also to do string matching in non-JSON messages
- Script to unescape JSON

### Changed
- feat(UpdateOffsetForConsumerGroup): Made poll timeout configurable

## 2021-06-28
### Changed
- fix(KafkaPublisher): Fixed JSON kafka messages not being produced with correct key

## 2021-03-12
### Added
- feat(KafkaQuery): Ability to specify timestamps in `yyyy-MM-dd'T'HH:mm:ss` format([#11](https://github.com/devatherock/utilities/issues/11))

## 2021-02-24
### Added
- feat(KafkaQuery): Ability to read all messages without key or json path filter

## 2021-02-06
### Added
- feat(CloseIssues): Script to close git issues with a specific label

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
