package lib.util

@Grab(group='org.yaml', module='snakeyaml', version='2.2')

import lib.entities.Query

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.LoaderOptions

import java.util.logging.Logger

class QueryUtil {
    static final Logger LOGGER = Logger.getLogger('QueryUtil.log')

    static def execute(Query query, Map<String, Object> rowData) {
        def output = [:]

        query.select.each { column ->
            output[column] = rowData[column]
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

        LOGGER.fine("Query: $query")

        return parsedQuery
    }
}