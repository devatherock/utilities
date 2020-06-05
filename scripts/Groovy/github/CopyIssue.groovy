@Grab(group = 'org.kohsuke', module = 'github-api', version = '1.112')

import groovy.cli.commons.CliBuilder
import groovy.transform.Field
import org.kohsuke.github.GHIssue
import org.kohsuke.github.GHIssueBuilder
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub

import java.util.logging.Logger

System.setProperty('java.util.logging.SimpleFormatter.format',
        '%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %4$s %5$s%6$s%n')
@Field Logger logger = Logger.getLogger('CopyIssue.log')

def cli = new CliBuilder(usage: 'groovy CopyIssue.groovy -[options]')
cli.t(longOpt: 'token', args: 1, argName: 'token', 'Github access token or environment variable name for it')
cli.n(longOpt: 'username', args: 1, argName: 'username', 'Github username corresponding to the access token')
cli.a(longOpt: 'api', args: 1, argName: 'api', 'Git API URL')
cli.o(longOpt: 'org', args: 1, argName: 'org', 'Github organization')
cli.sr(longOpt: 'source-repo', args: 1, argName: 'source-repo', 'Repository containing the existing issue')
cli.tr(longOpt: 'target-repo', args: 1, argName: 'target-repo', 'Repository to which to copy the issue')
cli.i(longOpt: 'issue-id', args: 1, argName: 'issue-id', 'The id of the existing issue')

def options = cli.parse(args)
if (!(options.o && options.t && options.n && options.sr && options.tr && options.i)) {
    cli.usage()
    System.exit(1)
}

String gitUrl = options.a ?: 'https://api.github.com'
String gitOrg = options.o
String gitToken = System.getenv(options.t) ?: options.t
String username = options.n
String sourceRepoName = options.sr
String targetRepoName = options.tr
int issueId = Integer.parseInt(options.i)

GitHub github = GitHub.connectToEnterpriseWithOAuth(gitUrl + '/api/v3', username, gitToken)
GHOrganization organization = github.getOrganization(gitOrg)
GHRepository sourceRepo = organization.getRepository(sourceRepoName)
GHRepository targetRepo = organization.getRepository(targetRepoName)

GHIssue sourceIssue = sourceRepo.getIssue(issueId)
GHIssueBuilder targetIssueBuilder = targetRepo.createIssue(sourceIssue.title).body(sourceIssue.body)
        .milestone(sourceIssue.milestone)

// Labels
sourceIssue.labels.each { label ->
    targetIssueBuilder.label(label.name)
}

// Assignees
sourceIssue.assignees.each { assignee ->
    targetIssueBuilder.assignee(assignee)
}
GHIssue targetIssue = targetIssueBuilder.create()
logger.info("New issue id: ${targetIssue.number}")

sourceIssue.comments.each { comment ->
    String commentBody = comment.body
    logger.fine({ "Source Comment: ${System.lineSeparator()}${commentBody}".toString() })

    List commentLines = commentBody.split('[\n]') as List
    StringBuilder commentBuilder = new StringBuilder()
    commentBuilder.append("${comment.user.login} commented:\n")

    commentLines.each { commentLine ->
        commentBuilder.append(">${commentLine}\n")
    }
    commentBuilder.replace(commentBuilder.length() - 1, commentBuilder.length(), '')
    logger.fine({ "Target Comment: ${System.lineSeparator()}${commentBuilder}".toString() })

    targetIssue.comment(commentBuilder.toString())
}