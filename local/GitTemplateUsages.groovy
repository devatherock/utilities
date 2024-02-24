import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def orgs = [
        'devatherock',
        'cortinico',
]
String templateRepo = 'cortinico/kotlin-gradle-plugin-template'

HttpClient client = HttpClient.newBuilder().build()
String gitToken = System.getenv('GIT_TOKEN')
JsonSlurper jsonSlurper = new JsonSlurper()
def matchedRepos = []

orgs.each { org ->
    def results
    String startAfter = null

    do {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create('https://api.github.com/graphql'))
                .header('Content-Type', 'application/json')
                .header('Authorization', "Bearer ${gitToken}")
                .POST(BodyPublishers.ofString(JsonOutput.toJson(['query': buildQuery(org, startAfter)])))
                .build()
        HttpResponse response = client.send(request, BodyHandlers.ofString())
        def parsedResponse = jsonSlurper.parseText(response.body())

        results = parsedResponse.data.repositoryOwner.repositories.nodes
        startAfter = parsedResponse.data.repositoryOwner.repositories.pageInfo.endCursor

        results.each { repo ->
            if (repo.templateRepository && repo.templateRepository.nameWithOwner == templateRepo) {
                matchedRepos.add(repo.nameWithOwner)
            }
        }
    } while (startAfter != null)
}
println(JsonOutput.prettyPrint(JsonOutput.toJson(matchedRepos)))

String buildQuery(String org, String startAfter) {
    String query
    String startAfterFilter = ''

    if (startAfter != null) {
        startAfterFilter = """after: "${startAfter}", """
    }

    query = """
        query {
            repositoryOwner(login: "${org}") {
                repositories(${startAfterFilter}first: 100) {
                    nodes {
                        nameWithOwner
                        templateRepository {
                            nameWithOwner
                        }
                    }
                    pageInfo {
                        endCursor
                    }
                }
            }
        }
    """.stripIndent().trim()

    return query
}