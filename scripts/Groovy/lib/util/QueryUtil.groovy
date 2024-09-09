package lib.util

@Grab(group = 'org.yaml', module = 'snakeyaml', version = '2.2')

import lib.entities.Query
import lib.entities.CompoundFilter
import lib.entities.Condition
import lib.entities.Operator

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.LoaderOptions

import java.util.logging.Logger
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.util.regex.Pattern

class QueryUtil {
    static final Logger LOGGER = Logger.getLogger('QueryUtil.log')

    static def execute(Query query, Map<String, Object> rowData) {
        LOGGER.fine({ "Mapped data to filter: ${rowData}".toString() })
        def output = null

        if (!query.where || filter(query.where, rowData)) {
            if (query.columns) {
                output = [:]

                query.columns.each { column, outputColumn ->
                    output[outputColumn] = rowData[column]

                    if (query.transformations) {
                        query.transformations.eachWithIndex { transform, index ->
                            if (transform.fields.contains(column)) {
                                output[outputColumn] = query.transformers[index].transform(output[outputColumn])
                            }
                        }
                    }
                }
            } else {
                output = rowData
            }
        }

        return output
    }

    static Query parseQuery(String query) {
        Query parsedQuery = new Yaml(new LoaderOptions(
                enumCaseSensitive: false
        )).loadAs(query, Query)

        parsedQuery.columns.clear()
        if (parsedQuery.select) {
            parsedQuery.select.each { column ->
                parsedQuery.columns[(column.toLowerCase())] = column
            }
        }

        if (parsedQuery.transformations) {
            parsedQuery.transformations.each { transform ->
                if (transform.name == 'convert_date') {
                    ConvertDate transformer = new ConvertDate()
                    String inputTimeZone = transform.parameters['input_time_zone']
                    String outputTimeZone = transform.parameters['output_time_zone']
                    ZoneId defaultZoneId = ZoneId.systemDefault()
                    ZoneId inputZoneId = inputTimeZone ? ZoneId.of(inputTimeZone) : defaultZoneId
                    ZoneId outputZoneId = outputTimeZone ? ZoneId.of(outputTimeZone) : defaultZoneId

                    transformer.inputFormat =
                            DateTimeFormatter.ofPattern(transform.parameters['input_format'])
                                    .withZone(inputZoneId)
                    transformer.outputFormat =
                            DateTimeFormatter.ofPattern(transform.parameters['output_format'])
                                    .withZone(outputZoneId)
                    parsedQuery.transformers.add(transformer)
                } else if (transform.name == 'concat') {
                    Concat transformer = new Concat()
                    transformer.left = transform.parameters['left']
                    transformer.right = transform.parameters['right']

                    parsedQuery.transformers.add(transformer)
                } else if (transform.name == 'upper') {
                    parsedQuery.transformers.add(Upper.DEFAULT)
                } else if (transform.name == 'extract') {
                    Extract transformer = new Extract()
                    String regex = transform.parameters['regex']
                    Pattern pattern = Pattern.compile(regex)

                    transformer.pattern = pattern
                    parsedQuery.transformers.add(transformer)
                }

                transform.fields = transform.fields.collect { it.toLowerCase() }
            }
        }

        LOGGER.fine({ "Query: $parsedQuery".toString() })

        return parsedQuery
    }

    static boolean filter(CompoundFilter filter, Map<String, Object> rowData) {
        boolean isSelected

        if (filter.filters) {
            isSelected = filter.operator == Operator.AND ? true : false

            for (int index = 0; index < filter.filters.size(); index++) {
                boolean filterResult = QueryUtil.filter(filter.filters[index], rowData)

                if (filter.operator == Operator.AND) {
                    isSelected = isSelected && filterResult

                    if (!isSelected) {
                        break
                    }
                } else {
                    isSelected = isSelected || filterResult

                    if (isSelected) {
                        break
                    }
                }
            }
        } else {
            if (filter.condition == Condition.NIN) {
                isSelected = !filter.values.contains(rowData[filter.field.toLowerCase()])
            } else if (filter.condition == Condition.IN) {
                isSelected = filter.values.contains(rowData[filter.field.toLowerCase()])
            } else if (filter.condition == Condition.LIKE) {
                isSelected = filter.values.any { rowData[filter.field.toLowerCase()] =~ it}
            }
        }

        return isSelected
    }
}