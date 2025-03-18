# laa-dces-drc-integration

This is a Java based Spring Boot application hosted on [MOJ Cloud Platform](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/concepts/what-is-the-cloud-platform.html).

[![MIT license](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

### Decrypting docker-compose.override.yml

The `docker-compose.override.yml` is encrypted using [git-crypt](https://github.com/AGWA/git-crypt).

To run the app locally you need to be able to decrypt this file.

You will first need to create a GPG key. See [Create a GPG Key](https://docs.publishing.service.gov.uk/manual/create-a-gpg-key.html) for details on how to do this with `GPGTools` (GUI) or `gpg` (command line).
You can install either from a terminal or just download the UI version.

```
brew update
brew install gpg
brew install git-crypt
```

Once you have done this, a team member who already has access can add your key by running `git-crypt add-gpg-user USER_ID`\* and creating a pull request to this repo.

Once this has been merged you can decrypt your local copy of the repository by running `git-crypt unlock`.

\*`USER_ID` can be your key ID, a full fingerprint, an email address, or anything else that uniquely identifies a public key to GPG (see "HOW TO SPECIFY A USER ID" in the gpg man page).
The apps should then startup cleanly if you run

### Application Set up

Clone Repository

```sh
git clone git@github.com:ministryofjustice/laa-dces-drc-integration.git
```

Setup git-secrets to prevent commiting secrets and credentials into git repository.
https://github.com/awslabs/git-secrets

```
git secrets --install
git secrets --register-aws
```
This will configure git-secrets to scan this Git repository on each commit
by registering the AWS rule set (Git hooks).

Run the following command to start scanning your repository
```
git secrets -–scan
```
The tool will generate an output file if it finds any vulnerability in the repository.

You can add custom patterns to scan for.
```
git secrets --add 'custom-pattern-here'
```

Make sure all tests are passed by running following ‘gradle’ Command

```sh
cd laa-dces-drc-integration/dces-drc-integration
./gradlew clean test
```

You will need to build the artifacts for the source code, using `gradle`.

```sh
./gradlew clean build
```

```sh
docker-compose build
docker-compose up
```

laa-dces-drc-integration application will be running on http://localhost:8089, with the actuator available at http://localhost:8188/actuator/
