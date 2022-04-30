@Grab(group = 'org.apache.commons', module = 'commons-text', version = '1.9')

import groovy.cli.commons.CliBuilder
import org.apache.commons.text.StringEscapeUtils

def cli = new CliBuilder(usage: 'groovy JsonUnescape.groovy [options]')
cli.i(longOpt: 'input', args: 1, argName: 'input', 'Input text to unescape')
cli.f(longOpt: 'input-file', args: 1, argName: 'input-file', 'Input file with content to unescape')

def options = cli.parse(args)
if (!(options.i || options.f)) {
    cli.usage()
    System.exit(1)
}

String escapedContent = options.i ?: new File(options.f).text
println(StringEscapeUtils.unescapeJson(escapedContent))