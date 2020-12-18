import java.util.logging.Logger
import groovy.transform.Field

System.setProperty('java.util.logging.SimpleFormatter.format',
        '%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %4$s %5$s%6$s%n')
@Field static final Logger LOGGER = Logger.getLogger('Deduplicater.log')

def cli = new CliBuilder(usage: 'groovy Deduplicater.groovy [options]')
cli.i(longOpt: 'input', args: 1, argName: 'input', 'Input file')
cli.o(longOpt: 'output', args: 1, argName: 'output', 'Output file to which to write unique values to')

def options = cli.parse(args)
if (!(options.i && options.o)) {
    cli.usage()
    System.exit(1)
}

File outputFile = new File(options.o)
Set uniqueValues = new HashSet()
int totalCount = 0
int duplicateCount = 0

new File(options.i).each { line ->
    totalCount++

    if (uniqueValues.contains(line)) {
        duplicateCount++
        LOGGER.fine({ "Duplicate '${line}' encountered".toString() })
    } else {
        uniqueValues.add(line)

        outputFile << line
        outputFile << System.lineSeparator()
    }
}
LOGGER.info("${duplicateCount} duplicates removed; ${totalCount - duplicateCount} unique values remain")