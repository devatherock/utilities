@Grab(group = 'org.kohsuke', module = 'github-api', version = '1.112')

import groovy.cli.commons.CliBuilder
import groovy.transform.Field
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub

import java.util.logging.Logger

System.setProperty('java.util.logging.SimpleFormatter.format',
        '%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %4$s %5$s%6$s%n')
@Field Logger logger = Logger.getLogger('AddLabelToIssues.log')

def cli = new CliBuilder(usage: 'groovy AddLabelToIssues.groovy -[options]', header: 'Options:')
cli.t(longOpt: 'token', args: 1, argName: 'token', 'Github access token or environment variable name for it')
cli.n(longOpt: 'username', args: 1, argName: 'username', 'Github username corresponding to the access token')
cli.a(longOpt: 'api', args: 1, argName: 'api', 'Git API URL')
cli.o(longOpt: 'org', args: 1, argName: 'org', 'Github organization')
cli.r(longOpt: 'repo', args: 1, argName: 'source-repo', 'Repository containing the issue(s) to label')
cli.s(longOpt: 'search-string', args: 1, argName: 'search-string', 'The search string to look for in issue title')
cli.l(longOpt: 'label', args: 1, argName: 'label', 'The label to apply to matching issue(s)')

def options = cli.parse(args)
if (!(options.t && options.n && options.r && options.s && options.l)) {
    cli.usage()
    System.exit(1)
}

String gitUrl = options.a ?: 'https://api.github.com'
def gitOrg = options.o
String gitToken = System.getenv(options.t) ?: options.t
String username = options.n
String repoName = options.r
String searchString = options.s
String label = options.l

GitHub github = GitHub.connectToEnterpriseWithOAuth(gitUrl, username, gitToken)

// If org is supplied, look for the repo within the org. Else look for the repo within the supplied user name
GHRepository repository = gitOrg ? github.getOrganization(gitOrg).getRepository(repoName) :
        github.getUser(username).getRepository(repoName)

repository.getIssues(GHIssueState.ALL)
        .findAll { it.title.contains(searchString) }
        .each { issue ->
            issue.setLabels(label)
            logger.info({ "Added label to issue-${issue.number}".toString() })
        }