# utilities
Utility scripts to perform common, repetitive tasks

## Usage
### GoogleAccessToken.groovy
- To generate an access token for Firebase cloud messaging

```
groovy GoogleAccessToken.groovy -c "path/to/service-accounts.json" -s "https://www.googleapis.com/auth/firebase.messaging"
```

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