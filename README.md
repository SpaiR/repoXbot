[![Build Status](https://travis-ci.org/SpaiR/repoXbot.svg?branch=master)](https://travis-ci.org/SpaiR/repoXbot)

# RepoXBot
Small bot for Space Station 13 repositories to automate some maintaining work.

Next features are available:
 - changelog creation
 - changelog validation
 - labeling of pull requests and issues
 - working with separate repository for changelog
 
Bot interacts with repository through GitHub API and GitHub Webhooks so make sure that you understand how to create
GitHub token and configure webhook in your repository.

## How to run
RepoXBot docker image is available [here](https://hub.docker.com/r/spair/repoxbot/). With run command you should provide some 
environmental variables to configure application.

```
docker run -d -p <your port>:8080 \
-e github_org=<your organization> \
-e github_repo=<your repository> \
-e github_token=<token to access GitHub API> \
-e github_secret=<secret code which you configured with webhook> \
--name=rxbot spair/repoxbot
```

Example:
```
docker run -d -p 8080:8080 \
-e github_org=TauCetiStation \
-e github_repo=TauCetiClassic \
-e github_token=c45y9tgxh47jf8347cmvidumls43rdfgvks8zalq \
-e github_secret=md7clqQ213 \
--name=rxbot spair/repoxbot
```

After application has started you should configure webhook to send events on your application address.
The only events you should send is `pull_request` and `issues`.
While sending `ping` event you should receive next response: `Pong! Zen: '<zen message here>'`.

## Application options
### Mandatory
- github_org - name of your GitHub organization
- github_repo - name of your GitHub repository
- github_token - personal access token you created to interact with GitHub
- github_secret - secret code which you configured with webhook

### Optional
- check_sign - enable/disable check for signature sent with webhook (default: true)
- config_path - path to json in repository to configure application features (default: .repoxbot.config.json)
- agent_name - agent name which will be used during interaction with GitHub APi (default: RepoXBot-Automation-Agent)
- use_hash - enable/disable hash creating after changelog updating (default: true)
- hash_path - if hash functionality enabled this can change file path to save hash (default: hash.txt)

## Features configuration
To enable changelog generation / changelog validation / pull request labeling you should create configuration json
in the root of you repository (may be changed with `config_path` option).

Configuration example:

```
{
  "changelogLocation": {
    "org": "TauCetiStation",
    "repo": "changelog",
    "path": "index.html"
  },
  "changelogClasses": {
    "rscadd": "Add",
    "bugfix": "Fix"
  },
  "diffPathsLabels": {
    "^diff.+/maps/.+\\.dmm$": "Map Edit"
  }
}
```

- changelogLocation - location to changelog file (it could be configured to absolutely different repository then codebase location)
- changelogClasses - key is class name used in 'inbody' changelog, value is label which will be added to PR if class name was found
- diffPathsLabels - key is regex expression (Java rules) and the value is label which will be added to PR if expression will match anything in PR diff
