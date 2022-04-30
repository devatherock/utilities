@Grab(group = 'net.sf.supercsv', module = 'super-csv', version = '2.4.0')

import org.supercsv.io.CsvListReader
import org.supercsv.prefs.CsvPreference

import java.util.logging.Logger
import groovy.transform.Field
import groovy.cli.commons.CliBuilder

System.setProperty('java.util.logging.SimpleFormatter.format',
        '%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %4$s %5$s%6$s%n')
@Field static final Logger LOGGER = Logger.getLogger('ExtractColumnFromCsv.log')

def cli = new CliBuilder(usage: 'groovy ExtractColumnFromCsv.groovy [options]')
cli.i(longOpt: 'input', args: 1, argName: 'input', 'Input CSV file')
cli.o(longOpt: 'output', args: 1, argName: 'output', 'Output file to which to write the extracted column to')
cli.c(longOpt: 'column', args: 1, argName: 'column', 'The column number to extract. Starts with 1')

def options = cli.parse(args)
if (!(options.i && options.o && options.c)) {
    cli.usage()
    System.exit(1)
}

int columnNumber = Integer.parseInt(options.c) - 1
CsvListReader csvReader
File outputFile = new File(options.o)

try {
    csvReader = new CsvListReader(new FileReader(options.i), CsvPreference.STANDARD_PREFERENCE)
    def rowData = csvReader.read()

    while (rowData != null) {
        LOGGER.fine({ rowData.toString() })
        outputFile << rowData[columnNumber]
        outputFile << System.lineSeparator()

        // Read next row
        rowData = csvReader.read()
    }
}
finally {
    csvReader.close()
}
LOGGER.info('Extraction complete')