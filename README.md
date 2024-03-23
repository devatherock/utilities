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

### CreateTag.groovy
Creates a git tag with commit comments as message and pushes to remote. Needs to be executed with the git repo as the working directory

**Sample:**

```shell
groovy /absolute/path/to/utilities/scripts/Groovy/github/CreateTag.groovy
```

**Parameters:**

```
    --help           Displays script usage instructions
 -nd,--not-dry-run   Flag to test changes without committing to git
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

### CsvQuery.groovy
Queries a CSV file and outputs the results to console or to a file

**Sample:**

```
groovy CsvQuery.groovy -i /path/to/input.csv -q /path/to/query.yml -o /path/to/output.csv
```

**Parameters:**

```
 -i,--input <input>       Input CSV file
 -o,--output <output>     Output CSV file
 -q,--query <query>       Yaml file containing the query
 -t,--threads <threads>   Number of threads to use for processing
```

**query.yml sample:**

```yaml
select:
  - full_name
  - age
  - country
reductions:
  - distinct
where:
  operator: and
  filters:
    - operator: or
      filters:
        - field: first_name
          condition: in
          values:
            - John
            - Jack
        - field: last_name
          condition: in
          values:
            - Smith
    - field: last_name
      condition: nin
      values:
        - Snow
    - field: country
      condition: like
      values:
        - ^Canada$|^Mexico$
```

### BackupCreator.groovy
Backs up specific files/folders within a source folder into a target folder. Files/folders to be backed up need to be specified in a config file

**Sample:**

```
groovy BackupCreator.groovy -s /path/to/source -t /path/to/target -c backup-config.yml
```

**Parameters:**

```
 -c,--config <config>             File containing the config to use for the backup
 -l,--log-level <log-level>       Log level to use. Defaults to INFO
 -s,--source-path <source-path>   Path from which to backup the files
 -t,--target-path <target-path>   Path to which to backup the files
```

**Config file sample:**

```yaml
inclusions:
  - .gradle/gradle.properties
  - .circleci/cli.yml
  - .docker/config.json
  - .jenkins/init.groovy.d
  - .jenkins/secrets
  - .kube/config
  - .minikube/ca.crt
  - .minikube/certs
  - .minikube/config
  - .minikube/machines
  - .minikube/profiles
  - .ssh
  - .gitconfig
  - .bashrc
  - .bash_history
  - .dbshell
  - .netrc
  - .zshrc
```