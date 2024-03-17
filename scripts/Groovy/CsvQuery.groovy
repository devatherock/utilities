@Grab(group = 'net.sf.supercsv', module = 'super-csv', version = '2.4.0')

import org.supercsv.io.CsvListReader
import org.supercsv.io.CsvMapWriter
import org.supercsv.prefs.CsvPreference

import lib.entities.Query
import lib.util.QueryUtil

import java.util.logging.Logger
import groovy.transform.Field
import groovy.cli.commons.CliBuilder

System.setProperty('java.util.logging.SimpleFormatter.format',
        '%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %4$s %5$s%6$s%n')
@Field static final Logger LOGGER = Logger.getLogger('CsvQuery.log')

LOGGER.info('Script started')
def cli = new CliBuilder(usage: 'groovy CsvQuery.groovy [options]')
cli.i(longOpt: 'input', args: 1, argName: 'input', required: true, 'Input CSV file')
cli.o(longOpt: 'output', args: 1, argName: 'output', 'Output CSV file')
cli.q(longOpt: 'query', args: 1, argName: 'query', required: true, 'Yaml file containing the query')

def options = cli.parse(args)
if (!options) {
    System.exit(1)
}

CsvListReader csvReader
CsvMapWriter csvWriter

Query query = QueryUtil.parseQuery(new File(options.q).text)
Writer writer = options.o ? new FileWriter(options.o) : new StringWriter()

try {
    csvReader = new CsvListReader(new FileReader(options.i), CsvPreference.STANDARD_PREFERENCE)
    csvWriter = new CsvMapWriter(writer, CsvPreference.STANDARD_PREFERENCE)
    boolean headerNotWritten = true
    String[] outputHeaders

    List inputHeaders = csvReader.getHeader(true).collect { it.toLowerCase() }
    List rowData = csvReader.read()

    while (rowData != null) {
        LOGGER.fine({ "Row data: ${rowData}".toString() })

        def rowMap = [:]
        rowData.eachWithIndex { def entry, int index ->
            rowMap[inputHeaders[index]] = entry
        }

        Map filteredData = QueryUtil.execute(query, rowMap)
        LOGGER.fine({ "Filtered data: ${filteredData}".toString() })

        if (filteredData) {
            if (headerNotWritten) {
                outputHeaders = filteredData.keySet().toArray(new String[1])
                csvWriter.writeHeader(outputHeaders)
                headerNotWritten = false
            }

            csvWriter.write(filteredData, outputHeaders)
        }

        // Read next row
        rowData = csvReader.read()
    }
}
finally {
    csvReader.close()
    csvWriter.close()
}
LOGGER.info('Query executed')

if (writer instanceof StringWriter) {
    LOGGER.info("Output: \n${writer}")
}