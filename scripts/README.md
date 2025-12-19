# DCES Integration Test Scripts

This directory contains some scripts to aid testing of DCES endpoints exposed by
the [TempTestController](../dces-drc-integration/src/main/java/uk/gov/justice/laa/crime/dces/integration/controller/TempTestController.java).
This controller is not enabled in all environments by default, but is usually deployed in dev, test & uat. See the
`FEATURE_TEMPTESTENDPOINTS` setting in the `feature-flags` kubernetes secret in the environment to see if it's enabled.

Calling the endpoints is not straightforward because it involves getting an OAuth token and using the correct
certificates and keys to configure the mTLS client connection.

## Pre-requisites

### AWS CLI

The scripts download the certificates and keys from an S3 bucket using the AWS CLI which requires some authentication.
The credentials to access the bucket are stored in the `dces-ca-admin-user-prod` secret in the
`laa-dces-drc-integration-prod` namespace.
Run this command to view the credentials

```shell
cloud-platform -n laa-dces-drc-integration-prod decode-secret -s dces-ca-admin-user-prod
```

Run the following command to configure the AWS CLI to use the credentials:

```shell
$ aws configure --profile dces-cert-store
AWS Access Key ID [<enter the value of access_key_id>]
AWS Secret Access Key [<enter the value of secret_access_key>]
```

## Scripts

### download-certs.sh

This script will download the required certificates and keys for use by the other scripts.

```shell
% ./download-certs.sh
Usage: ./download-certs.sh <env>
  <env> must be one of: dev, test, uat, prod
```

### get-auth-token.sh

This script gets a fresh token from AWS Cognito OAuth server for use when calling the DCES APIs.

```shell
% ./get-auth-token.sh
Usage: ./get-auth-token.sh [-r] <env>
  -r       Output the raw token only
  env      The environment, one of dev|test|uat|prod
```

It will prompt for the client ID and secret which have to be accessed via the AWS Console, see below.

```shell
% ./get-auth-token.sh uat
Enter Cognito credentials for uat:
Client ID:
Client Secret:

Run this command in the shell to store the token

export DCES_AUTH_TOKEN=*******3Zw

Token expires at: Thu 18 Dec 2025 16:53:08 GMT
```

To reuse the same token (and avoid being prompted to enter client credentials), run the export command to set the token
as an environment variable which will be used by the other scripts.

#### Getting OAuth Client ID and Secret using AWS Console

1. [Login to the Cloud Platform AWS Account](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/getting-started/accessing-the-cloud-console.html)
2. Goto the Cognito Service
3. Type `dces` in the search box to filter for the DCES user pools
4. Select the user pool for your environment
5. Click `App clients` in the navigation panel on the left
6. Click the App client you want to use to login (usually `dces-drc-api-<env>` for testing)
7. The Client ID and Client secret fields are the ones needed by the `get-auth-token.sh` script.

### curl-test-endpoint.sh

This script will hit the `/api/dces/test` endpoint which simply returns a success message. It is the most useful
script for testing the inbound (to DCES) connectivity.

```shell
% ./curl-test-endpoint.sh
Usage: ./curl-test-endpoint.sh <env>
  <env> must be one of: dev, test, uat, prod

% ./curl-test-endpoint.sh uat
GET Test Successful
HTTP Status: 200
```

### curl-test-fdc-endpoint.sh

This script will call the `/api/dces/test/fdc` endpoint which sends a fake FDC message to the DRC (Advantis).
It is useful for testing outbound connectivity to the DRC. As fake details are sent, the script is not configured
for use in production.

```shell
% ./curl-test-fdc-endpoint.sh
Usage: ./curl-test-fdc-endpoint.sh <env>
  <env> must be one of: dev, test, uat

% ./curl-test-fdc-endpoint.sh uat
{"type":"https://laa-debt-collection.service.justice.gov.uk/problem-types#duplicate-id","title":"Conflict","status":409,"detail":"The FdcId [106] already exists in the database.","instance":"/api/fdc","traceId":"69442a635d46d4998b6c90abf91b83b7"}
HTTP Status: 409
```

It is likely that a 409 response is returned as above because the record already exists in the DRC database, but it will prove the connectivity is working.

### cleanup.sh

This script removes any certificate and key files downloaded and any intermediate files created by the scripts.

```shell
% ./cleanup.sh
/bin/rm -rf /Users/andrew.roberts/IdeaProjects/laa-dces-drc-integration/scripts/tls-certs-keys
/bin/rm -f /Users/andrew.roberts/IdeaProjects/laa-dces-drc-integration/scripts/.*.headers
```
