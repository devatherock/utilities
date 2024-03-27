import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.Logger

import groovy.transform.Field
import groovy.cli.commons.CliBuilder

System.setProperty('java.util.logging.SimpleFormatter.format',
        '%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %4$s %5$s%6$s%n')
@Field static final Logger LOGGER = Logger.getLogger('FileDiff.log')

def cli = new CliBuilder(usage: 'groovy file/FileDiff.groovy [options]')
cli.f(longOpt: 'first-file', args: 1, argName: 'first-file', required: true, 'The first file')
cli.s(longOpt: 'second-file', args: 1, argName: 'second-file', required: true, 'The second file')
cli.o(longOpt: 'output', args: 1, argName: 'output', required: true,
        'Output file containing lines present in only one of the files')

def options = cli.parse(args)
if (!options) {
    System.exit(1)
}

String firstFile = options.f
String secondFile = options.s
File outputFile = new File(options.o)
int diffCount = findDiffs(firstFile, secondFile, outputFile)
diffCount += findDiffs(secondFile, firstFile, outputFile)

LOGGER.info("${diffCount} different lines found")

int findDiffs(String firstFile, String secondFile, File outputFile) {
    Set<String> firstFileContents = Files.readAllLines(Paths.get(firstFile)).toSet()
    int currentDiffCount = 0

    new File(secondFile).each { line ->
        if (!firstFileContents.contains(line)) {
            currentDiffCount++
            LOGGER.fine({ "Diff line '${line}' encountered".toString() })

            outputFile << line
            outputFile << System.lineSeparator()
        }
    }
    LOGGER.info("${currentDiffCount} additional lines present in ${secondFile}")

    return currentDiffCount
}