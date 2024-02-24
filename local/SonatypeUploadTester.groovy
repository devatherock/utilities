//@GrabResolver(name = 'sonatype-snapshots', root = 'https://oss.sonatype.org/content/repositories/snapshots')
@Grab(group = 'io.github.devatherock', module = 'jul-jsonformatter', version = '1.2.0')
@Grab(group = 'io.github.devatherock', module = 'simple-yaml', version = '0.3.0')

import java.util.logging.Logger

import io.github.devatherock.json.formatter.JSONFormatter
import io.github.devatherock.simpleyaml.SimpleYamlOutput

Logger root = Logger.getLogger('')
for (def handler : root.handlers) {
    handler.setFormatter(new JSONFormatter())
}

Logger.getLogger('testLogger').info('Hello, World!')
println(SimpleYamlOutput.toYaml([
        'foo'    : 'bar',
        'version': '1',
        'colors' : ['red', 'blue']
]))