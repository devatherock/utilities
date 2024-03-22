package lib.util

@Grab(group = 'org.yaml', module = 'snakeyaml', version = '2.2')

import lib.entities.Query
import lib.entities.CompoundFilter
import lib.entities.Condition
import lib.entities.Operator

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.LoaderOptions

import java.util.logging.Logger

class QueryUtil {
    static final Logger LOGGER = Logger.getLogger('QueryUtil.log')

    static def execute(Query query, Map<String, Object> rowData) {
        def output = null

        if (!query.where || filter(query.where, rowData)) {
            if (query.select) {
                output = [:]

                query.select.each { column ->
                    output[column] = rowData[column]
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

        if (parsedQuery.select) {
            parsedQuery.select = parsedQuery.select.collect { it.toLowerCase() }
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