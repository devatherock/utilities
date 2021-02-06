@Grab(group = 'org.kohsuke', module = 'github-api', version = '1.112')

import groovy.cli.commons.CliBuilder
import groovy.transform.Field
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub

import java.util.logging.Logger

System.setProperty('java.util.logging.SimpleFormatter.format',
        '%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %4$s %5$s%6$s%n')
@Field Logger logger = Logger.getLogger('CloseIssues.log')

CliBuilder cli = new CliBuilder(usage: 'groovy CloseIssues.groovy -[options]', header: 'Options:')
cli.t(longOpt: 'token', args: 1, argName: 'token', required: true,
        'Github access token or environment variable name for it')
cli.n(longOpt: 'username', args: 1, argName: 'username', required: true,
        'Github username corresponding to the access token')
cli.a(longOpt: 'api', args: 1, argName: 'api', defaultValue: 'https://api.github.com', 'Git API URL')
cli.o(longOpt: 'org', args: 1, argName: 'org', 'Github organization')
cli.r(longOpt: 'repo', args: 1, argName: 'source-repo', required: true, 'Repository containing the issue(s) to close')
cli.l(longOpt: 'label', args: 1, argName: 'label', required: true, 'The label to look for in issue(s) to close')

def options = cli.parse(args)
// options is null when there is a ParseException - https://github.com/groovy/groovy-core/blob/master/src/main/groovy/util/CliBuilder.groovy#L269
if (!options) {
    System.exit(1)
}

String gitUrl = options.a
def gitOrg = options.o
String gitToken = System.getenv(options.t) ?: options.t
String username = options.n
String repoName = options.r
String label = options.l

GitHub github = GitHub.connectToEnterpriseWithOAuth(gitUrl, username, gitToken)

// If org is supplied, look for the repo within the org. Else look for the repo within the supplied user name
GHRepository repository = gitOrg ? github.getOrganization(gitOrg).getRepository(repoName) :
        github.getUser(username).getRepository(repoName)

repository.getIssues(GHIssueState.OPEN)
        .findAll { it.labels.any { it.name == label } }
        .each { issue ->
            issue.close()
            logger.info({ "Closed issue-${issue.number}".toString() })
        }