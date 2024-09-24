@Grab(group = 'net.sf.supercsv', module = 'super-csv', version = '2.4.0')

import org.supercsv.io.CsvMapReader
import org.supercsv.io.CsvMapWriter
import org.supercsv.prefs.CsvPreference

import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.util.logging.Logger
import groovy.transform.Field
import groovy.cli.commons.CliBuilder

System.setProperty('java.util.logging.SimpleFormatter.format',
        '%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %4$s %5$s%6$s%n')
@Field static final Logger LOGGER = Logger.getLogger('FundsIndiaToClearTax.log')

@Field static final String INPUT_NAME = 'Scheme Name'
@Field static final String INPUT_ISIN = 'ISIN'
@Field static final String INPUT_SALE_DATE = 'Sale Date'
@Field static final String INPUT_SALE_PRICE = 'Sale Value'
@Field static final String INPUT_PURCHASE_DATE = 'Purchase Date'
@Field static final String INPUT_COST_PRICE = 'Purchase Price'
@Field static final String INPUT_UNITS = 'Units'
@Field static final def INPUT_COLUMNS = [
        INPUT_NAME, INPUT_ISIN, null, null, null, null,
        INPUT_SALE_DATE, INPUT_UNITS, INPUT_SALE_PRICE, null, null, null,
        INPUT_PURCHASE_DATE, null, INPUT_COST_PRICE,
        null, null, null, null, null, null, null, null, null, null
] as String[]

@Field static final String OUTPUT_NAME = 'name'
@Field static final String OUTPUT_ISIN = 'isin'
@Field static final String OUTPUT_SALE_DATE = 'sale_date'
@Field static final String OUTPUT_SALE_PRICE_PER_UNIT = 'sale_price_per_unit'
@Field static final String OUTPUT_PURCHASE_DATE = 'purchase_date'
@Field static final String OUTPUT_COST_PRICE = 'cost_price'
@Field static final String OUTPUT_UNITS = 'units'
@Field static final def OUTPUT_COLUMNS =
        [OUTPUT_ISIN, OUTPUT_NAME, OUTPUT_UNITS, OUTPUT_PURCHASE_DATE, OUTPUT_COST_PRICE,
         OUTPUT_SALE_DATE, OUTPUT_SALE_PRICE_PER_UNIT] as String[]

@Field static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ofPattern('dd-MMM-yyyy')
@Field static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ofPattern('dd/MM/yyyy')

def cli = new CliBuilder(usage: 'groovy FundsIndiaToClearTax.groovy [options]')
cli.i(longOpt: 'input', args: 1, argName: 'input', required: true, 'Input CSV file')
cli.o(longOpt: 'output', args: 1, argName: 'output', required: true, 'Output file')

def options = cli.parse(args)
if (!options) {
    cli.usage()
    System.exit(1)
}

CsvMapReader csvReader
CsvMapWriter csvWriter

try {
    csvReader = new CsvMapReader(new FileReader(options.i), CsvPreference.STANDARD_PREFERENCE)
    csvReader.getHeader(true) // To skip the header

    csvWriter = new CsvMapWriter(new FileWriter(options.o), CsvPreference.EXCEL_PREFERENCE)
    csvWriter.writeHeader(OUTPUT_COLUMNS)

    def rowData = csvReader.read(INPUT_COLUMNS)
    while (rowData != null) {
        LOGGER.fine({ rowData.toString() })

        def outputRow = [
                (OUTPUT_ISIN)               : rowData[INPUT_ISIN],
                (OUTPUT_NAME)               : rowData[INPUT_NAME],
                (OUTPUT_UNITS)              : rowData[INPUT_UNITS],
                (OUTPUT_PURCHASE_DATE)      : OUTPUT_FORMAT.format(INPUT_FORMAT.parse(rowData[INPUT_PURCHASE_DATE])),
                (OUTPUT_COST_PRICE)         : rowData[INPUT_COST_PRICE],
                (OUTPUT_SALE_DATE)          : OUTPUT_FORMAT.format(INPUT_FORMAT.parse(rowData[INPUT_SALE_DATE])),
                (OUTPUT_SALE_PRICE_PER_UNIT): rowData[INPUT_SALE_PRICE]
        ]
        csvWriter.write(outputRow, OUTPUT_COLUMNS)

        // Read next row
        rowData = csvReader.read(INPUT_COLUMNS)
    }
}
finally {
    csvReader.close()
    csvWriter.close()
}