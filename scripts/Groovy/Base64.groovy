import groovy.cli.commons.CliBuilder

import java.nio.charset.StandardCharsets

def cli = new CliBuilder(usage: 'groovy Base64.groovy [options]')
cli.D(longOpt: 'decode', args: 0, argName: 'decode', 'Flag to indicate decode operation')
cli.i(longOpt: 'input', args: 1, argName: 'input', 'Input text to encode/decode')
cli.f(longOpt: 'input-file', args: 1, argName: 'input-file', 'Input file to encode/decode')
cli.o(longOpt: 'output-file', args: 1, argName: 'output-file', 'Output file to write encoded/decoded content to')

def options = cli.parse(args)
if (!(options.i || options.f)) {
    cli.usage()
    System.exit(1)
}

def input = options.i ? options.i.bytes : new File(options.f).bytes
def output = options.o ? new File(options.o) : null

if (options.D) {
    if (output) {
        output << java.util.Base64.decoder.decode(input)
    } else {
        println(new String(java.util.Base64.decoder.decode(input), StandardCharsets.UTF_8))
    }
} else {
    println(new String(java.util.Base64.getEncoder().encode(input), StandardCharsets.UTF_8))
}