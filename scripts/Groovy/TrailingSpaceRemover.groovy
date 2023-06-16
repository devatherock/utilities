import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.logging.Logger

import groovy.transform.Field
import groovy.cli.commons.CliBuilder

System.setProperty('java.util.logging.SimpleFormatter.format',
        '%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %4$s %5$s%6$s%n')
@Field static final Logger LOGGER = Logger.getLogger('TrailingSpaceRemover.log')

def cli = new CliBuilder(usage: 'groovy TrailingSpaceRemover.groovy [options]')
cli.i(longOpt: 'input', args: 1, argName: 'input', 'Input file', required: true)
cli.o(longOpt: 'output', args: 1, argName: 'output', 'Output file')

def options = cli.parse(args)
if (!options) {
    cli.usage()
    System.exit(1)
}

StringBuilder outputFileContent = new StringBuilder()
int totalCount = 0
int trimmedCount = 0

File inputFile = new File(options.i)
inputFile.each { line ->
    totalCount++

    if (line.endsWith(' ')) {
        outputFileContent.append(line.stripTrailing())
        trimmedCount++
    } else {
        outputFileContent.append(line)
    }

    outputFileContent.append(System.lineSeparator())
}
LOGGER.info("Total lines: ${totalCount}, Lines trimmed: ${trimmedCount}")

File outputFile = options.o ? new File(options.o) : inputFile
Files.writeString(outputFile.toPath(), outputFileContent, StandardOpenOption.TRUNCATE_EXISTING)