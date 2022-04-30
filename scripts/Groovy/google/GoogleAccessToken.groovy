@GrabResolver(name='google', root='https://maven.google.com/')
@Grab(group='com.google.api-client', module='google-api-client', version='1.30.8')

import groovy.cli.commons.CliBuilder
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential

def cli = new CliBuilder(usage: 'groovy GoogleAccessToken.groovy [options]')
cli.c(longOpt: 'credentials', args: 1, argName: 'credentials', 'Path to credentials JSON')
cli.s(longOpt: 'scopes', args: 1, argName: 'scopes', 'Comma separated list of scopes')

def options = cli.parse(args)
if(!(options.c && options.s)) {
	cli.usage()
	System.exit(1)
}

GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(options.c))
	.createScoped(options.s.split(',') as List)
credential.refreshToken()
println credential.accessToken