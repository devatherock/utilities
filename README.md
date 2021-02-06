# utilities
Utility scripts to perform common, repetitive tasks

## Google
### GoogleAccessToken.groovy
- To generate an access token for Firebase cloud messaging

```
groovy GoogleAccessToken.groovy -c "path/to/service-accounts.json" -s "https://www.googleapis.com/auth/firebase.messaging"
```


## Github
When using these scripts with github enterprise, an API URL including the `/api/v3` path would need to be supplied

### AddLabelToIssues.groovy
Adds a supplied label to issues with a specific string in title

- To use with a repo in an org
```shell
groovy github/AddLabelToIssues.groovy -t <GIT_TOKEN> -n <username> -o <org> -r <repo-name> \
  -s <search-string> -l <label>
```

- To use with a repo under a username
```shell
groovy github/AddLabelToIssues.groovy -t <GIT_TOKEN> -n <username> -r <repo-name> -s <search-string> -l <label>
```

### CloseIssues.groovy
Closes issues that have a specific label

- To use with a repo in an org
```shell
groovy github/CloseIssues.groovy -t <GIT_TOKEN> -n <username> -o <org> -r <repo-name> -l <label>
```

- To use with a repo under a username
```shell
groovy github/CloseIssues.groovy -t <GIT_TOKEN> -n <username> -r <repo-name> -l <label>
```

## Other scripts
### Base64.groovy
- To encode a string
```
groovy Base64.groovy -i hello
```

- To encode contents of a file
```
groovy Base64.groovy -f /path/to/file
```

- To decode a string
```
groovy Base64.groovy -D -i aGVsbG8=
```

### Deduplicater.groovy
```
groovy Deduplicater.groovy -i /path/to/input/file -o /path/to/output/file
```

### ExtractColumnFromCsv.groovy
```
groovy ExtractColumnFromCsv.groovy -i /path/to/input/file -o /path/to/output/file -c <1-based column number>
```