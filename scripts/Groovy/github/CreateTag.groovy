import groovy.cli.commons.CliBuilder
import groovy.transform.Field

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path
import java.util.logging.Logger
import java.util.regex.Matcher
import java.util.regex.Pattern

System.setProperty('java.util.logging.SimpleFormatter.format', '%5$s%n')
@Field static final Logger LOGGER = Logger.getLogger('CreateTag.log')

def cli = new CliBuilder(usage: 'groovy CreateTag.groovy [options]', width: 100)
cli.nd(longOpt: 'not-dry-run', args: 0, argName: 'not-dry-run', 'Flag to test changes without committing to git')
cli._(longOpt: 'help', args: 0, argName: 'help', 'Displays script usage instructions')

def options = cli.parse(args)
if (options.help) {
    cli.usage()
    System.exit(0)
}

def notDryRun = options.nd
String branch = executeCommand('git branch --show-current')[1]

// Get current tag
String currentTag
def currentTagOutput = executeCommand('git describe --tags --abbrev=0')
if (currentTagOutput[0] == 0) {
    currentTag = currentTagOutput[1]
}

// Find next tag
String startTag = '0.1.0'
String nextVersionPrefix

if (currentTag) {
    Pattern pattern = Pattern.compile('v([0-9]+)\\.([0-9]+)\\.([0-9]+).*')
    Matcher matcher = pattern.matcher(currentTag)
    String majorVersion
    String minorVersion
    String patchVersion

    if (matcher.matches()) {
        majorVersion = matcher.group(1)
        minorVersion = matcher.group(2)
        patchVersion = matcher.group(3)

        nextVersionPrefix = "${majorVersion}.${Integer.parseInt(minorVersion) + 1}.${patchVersion}"
    }
} else {
    nextVersionPrefix = startTag
}

String nextVersion
if (['master', 'main'].contains(branch)) {
    nextVersion = nextVersionPrefix
} else {
    String gitLog = executeCommand('git log --oneline --no-decorate -n 1')[1]
    nextVersion = "${nextVersionPrefix}-dev.${gitLog.substring(0, 7)}"
}

if (nextVersion) {
    String gitLog
    if (['master', 'main'].contains(branch)) {
        if (currentTag) {
            gitLog = executeCommand("git log ${currentTag}..HEAD --oneline --no-decorate")[1]
        } else {
            gitLog = executeCommand('git log --oneline --no-decorate')[1]
        }

        Path tagMessageFile = Paths.get('tag-message.txt')
        Files.write(tagMessageFile, gitLog.bytes)

        String nextTag = "v${nextVersion}"
        executeCommand("git tag -a -F tag-message.txt ${nextTag}")

        if (notDryRun) {
            executeCommand("git push origin ${nextTag}")
        }
        Files.delete(tagMessageFile)
    }
}
LOGGER.info("Next version: ${nextVersion}")

/**
 * Executes a command and returns the exit code and output
 *
 * @param command
 * @return exit code and output
 */
def executeCommand(def command) {
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