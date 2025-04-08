# laa-dces-drc-integration

This is a Java based Spring Boot application hosted on [MOJ Cloud Platform](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/concepts/what-is-the-cloud-platform.html).

[![MIT license](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

### Modifying docker-compose.override.yml

The `docker-compose.override.yml` and some other files used to be encrypted using `git-crypt`.
However, the use of `git-crypt` is now deprecated and has since been removed from this repository.

If you make local changes to `docker-compose.override.yml`, be sure not to commit them.
In fact, it may make sense to remove `docker-compose.override.yml` and add it to `.gitignore`.

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
