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

#### Running Application or Integration Tests Locally

You will need to have a spring datasource configuration in order to run the application or it's Integration Test Module.
It requires a Postgresql database, of which each environment has their own.
For running locally, you will need at least the following:

```
spring.datasource.url
spring.datasource.username
spring.datasource.password
```

These can either be added in your application settings, or via Environment properties.

```
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

If you are connecting to a specific environment's database, you can get these details from the datasource secret of that environment.

#### Maat Court Data API

You will also need to configure the application to use an instance of the Maat CD API. This can either be ran locally via docker, or by using one of the Maat API's environments.
If using an environment's Maat API ( such as Development ) you can get the url and details via the usual channels, or taking them from an environment's secrets.

#### Running Application Locally

Then pull down the environment file and run the Docker container with the script:

```shell
./start-local.sh
```

laa-dces-drc-integration application will be running on http://localhost:8089, with the actuator available at http://localhost:8188/actuator/
