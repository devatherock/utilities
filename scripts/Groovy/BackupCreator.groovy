@Grab(group = 'org.yaml', module = 'snakeyaml', version = '2.0')

import groovy.cli.commons.CliBuilder
import groovy.transform.Field

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.logging.Level
import java.util.logging.Logger

import org.yaml.snakeyaml.Yaml

System.setProperty('java.util.logging.SimpleFormatter.format',
        '%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %4$s %5$s%6$s%n')

@Field static final Logger LOGGER = Logger.getLogger('BackupCreator.log')
@Field List<String> fileInclusions = []
@Field List<String> folderInclusions = []

def cli = new CliBuilder(usage: 'groovy BackupCreator.groovy [options]', width: 100)
cli.s(longOpt: 'source-path', args: 1, argName: 'source-path', required: true, 'Path from which to backup the files')
cli.t(longOpt: 'target-path', args: 1, argName: 'target-path', required: true, 'Path to which to backup the files')
cli.c(longOpt: 'config', args: 1, argName: 'config', required: true, 'File containing the config to use for the backup')
cli.l(longOpt: 'log-level', args: 1, argName: 'log-level', 'Log level to use. Defaults to INFO')

def options = cli.parse(args)
if (!options) {
    System.exit(1)
}

String source = options.s
String target = options.t
String configFile = options.c

// Set log level
if (options.l) {
    Level level = Level.parse(options.l.toUpperCase())
    Logger root = Logger.getLogger('')
    root.setLevel(level)

    for (def handler : root.handlers) {
        handler.setLevel(level)
    }
}

Yaml yaml = new Yaml()
def config = yaml.load(new File(configFile).text)

def inclusions
if (config && config['inclusions']) {
    inclusions = config['inclusions']
} else {
    LOGGER.warning({ "Config file ${configFile} doesn't contain any inclusions".toString() })
    System.exit(1)
}

def inclusionsAbsolute = []
inclusions.each { inclusion ->
    inclusionsAbsolute.add(Paths.get(source, inclusion).toString())
}
LOGGER.fine({ "Inclusions: ${inclusionsAbsolute}".toString() })

inclusionsAbsolute.each { inclusion ->
    if (Files.isDirectory(Paths.get(inclusion))) {
        folderInclusions.add(inclusion)
    } else {
        fileInclusions.add(inclusion)
    }
}

backupFiles(source, target)
LOGGER.info('Processed folder inclusions')

fileInclusions.each { sourceFile ->
    backupFile(sourceFile, getAbsoluteTargetPath(sourceFile, source, target))
}
LOGGER.info('Processed file inclusions')

/**
 * Backs up folders
 *
 * @param source
 * @param target
 */
void backupFiles(String source, String target) {
    LOGGER.fine({ "Source: ${source}".toString() })
    Path sourcePath = Paths.get(source)

    if (Files.isDirectory(sourcePath)) {
        sourcePath.toFile().listFiles().each { path ->
            String absolutePath = path.getAbsolutePath()
            LOGGER.fine({ "Path being filtered: ${absolutePath}".toString() })

            if (folderInclusions.any { inclusion ->
                // For when we process a child folder whose parent is included
                absolutePath.startsWith(inclusion) ||
                        // For when we process a parent folder whose child is included
                        inclusion.startsWith(absolutePath)
            }) {
                // Current folder also gets returned in this loop. Need to skip it
                if (source != absolutePath) {
                    String targetChildPath = getAbsoluteTargetPath(absolutePath, source, target)

                    LOGGER.fine({ "Source child path: ${absolutePath}, Target child path: ${targetChildPath}".toString() })

                    backupFiles(absolutePath, targetChildPath)
                }
            }
        }
    } else {
        backupFile(source, target)
    }
}

/**
 * Backs up a file
 *
 * @param source
 * @param target
 */
void backupFile(String source, String target) {
    String targetFolder = getContainingFolder(target)
    Files.createDirectories(Paths.get(targetFolder))
    Files.copy(Paths.get(source), Paths.get(target), StandardCopyOption.REPLACE_EXISTING)

    LOGGER.info({ "Copied ${source} to ${target}".toString() })
}

/**
 * Finds the immediate parent folder name from an absolute file name
 *
 * @param file
 * @return the folder name
 */
String getContainingFolder(String file) {
    return file.substring(0, file.lastIndexOf(File.separator))
}

/**
 * Calculates the absolute target path for a file using the supplied parameters
 *
 * @param absoluteSourcePath
 * @param sourceRoot
 * @param targetRoot
 * @return the absolute target path
 */
String getAbsoluteTargetPath(String absoluteSourcePath, String sourceRoot, String targetRoot) {
    return Paths.get(targetRoot, absoluteSourcePath.replaceFirst(sourceRoot, '')).toString()
}