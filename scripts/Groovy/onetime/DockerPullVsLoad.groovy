import groovy.cli.commons.CliBuilder
import groovy.transform.Field

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

System.setProperty('java.util.logging.SimpleFormatter.format', '%5$s%n')
@Field static final Logger LOGGER = Logger.getLogger('DockerPullVsLoad.log')

def cli = new CliBuilder(usage: 'groovy DockerPullVsLoad.groovy [options]')
cli.i(longOpt: 'input', args: 1, argName: 'input', required:true, 'Input file containing the URL')
cli.o(longOpt: 'output-image', args: 1, argName: 'output-image', defaultValue: 'devatherock/minify-js:3.1.0',
        'Output image name')
cli.of(longOpt: 'output-file', args: 1, argName: 'output-file', defaultValue: 'minify-js-3.1.0.tar',
        'Output file name')
cli.n(longOpt: 'iterations', args: 1, argName: 'iterations', defaultValue: '10', 'Number of iterations')

def options = cli.parse(args)
if (!options) {
    System.exit(1)
}

String inputUrl = new File(options.i).text.trim()
String outputImageName = options.o
String outputFileName = options.of
long timeTaken = 0
int iterations = Integer.parseInt(options.n)

(0..iterations).each {
    long startTime = System.nanoTime()
    executeCommand("""
        curl -o ${outputFileName} --location '${inputUrl}'
        docker load --input ${outputFileName}
    """)
    timeTaken += TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)

    executeCommand("""
        rm ${outputFileName}
        docker rmi ${outputImageName}
    """)
}
LOGGER.info("Time taken for load: ${timeTaken}")

timeTaken = 0
(0..iterations).each {
    long startTime = System.nanoTime()
    executeCommand("docker pull ${outputImageName}")
    timeTaken += TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)

    executeCommand("docker rmi ${outputImageName}")
}
LOGGER.info("Time taken for pull: ${timeTaken}")

/**
 * Executes a command and returns the exit code and output
 *
 * @param command
 * @return exit code and output
 */
def executeCommand(def command) {
    LOGGER.info(command)
    def finalCommand = command instanceof List ? command : ['sh', '-c', command]
    Process process = finalCommand.execute()
    StringBuilder out = new StringBuilder()
    StringBuilder err = new StringBuilder()
    process.consumeProcessOutput(out, err)
    int exitCode = process.waitFor()

    if (out.length() > 0) {
        LOGGER.info(out.toString())
    }
    if (err.length() > 0) {
        LOGGER.severe(err.toString())
    }

    return [exitCode, "${out}${System.lineSeparator()}${err}".trim()]
}
