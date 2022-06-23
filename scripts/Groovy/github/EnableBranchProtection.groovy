@Grab(group = 'org.springframework', module = 'spring-webflux', version = '5.3.2')
@Grab(group = 'io.projectreactor.netty', module = 'reactor-netty', version = '0.9.15.RELEASE')

import groovy.json.JsonOutput
import groovy.transform.Field
import groovy.cli.commons.CliBuilder

import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.ClientResponse

import java.util.logging.Logger

System.setProperty('java.util.logging.SimpleFormatter.format',
        '%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %4$s %5$s%6$s%n')
@Field Logger LOGGER = Logger.getLogger('EnableBranchProtection.log')

def cli = new CliBuilder(usage: 'groovy EnableBranchProtection.groovy -[options]', width: 200)
cli.t(longOpt: 'token', args: 1, argName: 'token', defaultValue: 'GIT_TOKEN', 'Github access token')
cli.n(longOpt: 'username', args: 1, argName: 'username', 'Github username corresponding to the access token')
cli.a(longOpt: 'api', args: 1, argName: 'api', defaultValue: 'https://api.github.com', 'Git API URL')
cli.o(longOpt: 'org', args: 1, argName: 'org', 'Github organization')
cli.r(longOpt: 'repo', args: 1, argName: 'repo', required: true, 'Repository to which to enable branch protection')
cli.b(longOpt: 'branches', args: 1, argName: 'branches', defaultValue: 'master,main',
        'Comma separated list of branches to protect')
cli.s(longOpt: 'status-checks', args: 1, argName: 'status-checks', 'Comma separated list of required status checks')
cli.rc(longOpt: 'review-count', args: 1, argName: 'review-count', defaultValue: '1', 'Number of reviews required')
cli._(longOpt: 'help', args: 0, argName: 'help', 'Displays script usage instructions')

def options = cli.parse(args)
if (!options) {
    System.exit(1)
} else if (options.help) {
    cli.usage()
    System.exit(0)
}

String gitUrl = options.a
def gitOrg = options.o
String gitToken = System.getenv(options.t) ?: options.t
String username = options.n
String repoName = options.r
int reviewCount = Integer.parseInt(options.rc)

WebClient httpClient = WebClient.builder()
        .baseUrl(gitUrl)
        .build()

def relevantBranches = options.b.split(',') as List
def requiredStatusChecks = options.s ? [
        'strict': true,
        'contexts': options.s.split(',') as List
]: null

def branches = httpClient.get()
        .uri("/repos/${gitOrg ?: username}/${repoName}/branches")
        .header('Authorization', "token ${gitToken}")
        .header('accept', 'application/vnd.github.v3+json')
        .retrieve()
        .bodyToMono(List)
        .block()

branches.findAll { relevantBranches.contains(it['name']) }.each { branch ->
    def requiredPullRequestReviews = [
            "dismiss_stale_reviews": true,
            "required_approving_review_count": reviewCount
    ]

    def dismissalRestrictions = null
    if (gitOrg) {
        dismissalRestrictions = [:]
        dismissalRestrictions['users'] = []
        dismissalRestrictions['teams'] = []

        requiredPullRequestReviews['dismissal_restrictions'] = dismissalRestrictions
    }

    def requestBody = [
            "required_status_checks"       : requiredStatusChecks,
            "required_pull_request_reviews": requiredPullRequestReviews,
            "restrictions"                 : null,
            "enforce_admins"               : false
    ]
    LOGGER.info({ JsonOutput.toJson(requestBody) })

    ClientResponse response = httpClient.put()
            .uri("/repos/${gitOrg ?: username}/${repoName}/branches/${branch['name']}/protection")
            .header('Authorization', "token ${gitToken}")
            .header('Content-Type', 'application/json')
            .header('accept', 'application/vnd.github.v3+json')
            .bodyValue(JsonOutput.toJson(requestBody))
            .exchange()
            .block()

    if (response.statusCode().isError()) {
        LOGGER.severe("Error response: ${response.bodyToMono(String).block()}")
    } else {
        LOGGER.fine({ "Success response: ${response.bodyToMono(String).block()}".toString() })
    }
}