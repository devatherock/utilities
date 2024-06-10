@Grab(group = 'net.sf.supercsv', module = 'super-csv', version = '2.4.0')

import org.supercsv.io.CsvListReader
import org.supercsv.io.CsvMapWriter
import org.supercsv.prefs.CsvPreference

import lib.entities.Query
import lib.util.QueryUtil
import lib.util.AsyncCsvWriter

import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger
import java.util.logging.Level
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

import groovy.transform.Field
import groovy.cli.commons.CliBuilder

System.setProperty('java.util.logging.SimpleFormatter.format',
        '%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %4$s %5$s%6$s%n')
@Field static final Logger LOGGER = Logger.getLogger('CsvQuery.log')

long startTime = System.currentTimeMillis()
def cli = new CliBuilder(usage: 'groovy CsvQuery.groovy [options]')
cli.i(longOpt: 'input', args: 1, argName: 'input', 'Input CSV file')
cli.is(longOpt: 'input-file-separator', args: 1, argName: 'input-file-separator', defaultValue: ',',
        'The separator character used in the input file. Defaults to ,')
cli.o(longOpt: 'output', args: 1, argName: 'output', 'Output CSV file')
cli.q(longOpt: 'query', args: 1, argName: 'query', required: true, 'Yaml file containing the query')
cli.t(longOpt: 'threads', args: 1, argName: 'threads', defaultValue: '3', 'Number of threads to use for processing')

def options = cli.parse(args)
if (!options) {
    System.exit(1)
}

CsvListReader csvReader
CsvMapWriter csvWriter

char inputSeparator = options.is
Query query = QueryUtil.parseQuery(new File(options.q).text)
String inputFile = options.i ?: query.from
assert inputFile : 'Input file name not specified'

Writer writer = options.o ? new FileWriter(options.o) : new StringWriter()
AtomicInteger recordCount = new AtomicInteger(0)

int threads = Integer.parseInt(options.t)
ThreadPoolExecutor executor = new ThreadPoolExecutor(1, threads, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(threads * 5), new ThreadPoolExecutor.CallerRunsPolicy())
CsvPreference inputFilePref = new CsvPreference.Builder(
        CsvPreference.STANDARD_PREFERENCE.quoteChar, (int)inputSeparator, CsvPreference.STANDARD_PREFERENCE.endOfLineSymbols)
        .build()

try {
    csvReader = new CsvListReader(new FileReader(inputFile), inputFilePref)
    csvWriter = new CsvMapWriter(writer, CsvPreference.STANDARD_PREFERENCE)
    AsyncCsvWriter asyncWriter = new AsyncCsvWriter(csvWriter)
    String[] outputHeaders

    List inputHeadersOriginal = csvReader.getHeader(true) as List
    List inputHeaders = inputHeadersOriginal.collect { it.toLowerCase().trim() }
    if (!query.columns) {
        inputHeaders.eachWithIndex { header, index ->
            query.columns[header] = inputHeadersOriginal[index]
        }
    }

    List rowData = csvReader.read()
    while (rowData != null) {
        LOGGER.fine({ "Row data: ${rowData}".toString() })
        def localData = new ArrayList(rowData)

        executor.submit {
            try {
                def rowMap = [:]
                localData.eachWithIndex { def entry, int index ->
                    rowMap[inputHeaders[index]] = entry
                }

                Map filteredData = QueryUtil.execute(query, rowMap)
                LOGGER.fine({ "Filtered data: ${filteredData}".toString() })

                if (filteredData) {
                    recordCount.incrementAndGet()
                    asyncWriter.write(filteredData)
                }
            } catch (Exception exception) {
                LOGGER.log(Level.SEVERE, exception.message, exception)
            }
        }

        // Read next row
        rowData = csvReader.read()
    }

    executor.shutdown()
    while (executor.isTerminating()) {
        Thread.sleep(100)
    }
    asyncWriter.stop()
}
finally {
    csvReader.close()
    csvWriter.close()
}
LOGGER.info("Results: ${recordCount}, Time taken: ${(System.currentTimeMillis() - startTime)/1000} seconds")

if (writer instanceof StringWriter) {
    LOGGER.info("Output: \n${writer}")
}