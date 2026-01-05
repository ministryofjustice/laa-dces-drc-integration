#!/usr/bin/env bash
set -eo pipefail

# Usage message
usage() {
  echo "Usage: $0 <env> <uri>"
  echo "  <env> must be one of: dev, test, uat, prod"
  exit 1
}

# Check argument count
if [[ $# -ne 2 ]]; then
  usage
fi

ENV="$1"
URI="$2"

# Validate environment
case "$ENV" in
  dev|test|uat|prod)
    ;; # valid
  *)
    echo "Invalid environment: $ENV"
    usage
    ;;
esac

# Validate URI (basic check: non-empty and starts with http/https)
if [[ -z "$URI" ]]; then
  echo "Invalid URI: $URI"
  usage
else
  URI="${URI#/}"  # Remove any leading slash
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Get the OAuth token, either from the environment or get a new one
if [[ -z "$DCES_AUTH_TOKEN" ]]; then
    echo DCES_AUTH_TOKEN not set, getting a new token
    DCES_AUTH_TOKEN=$(./get-auth-token.sh -r $ENV)
fi

CERTS_DIR=$SCRIPT_DIR/tls-certs-keys/$ENV
if [[ ! -f $CERTS_DIR/dcestesting-int-${ENV}.crt || ! -f $CERTS_DIR/dcestesting-int-${ENV}.key ]]; then
  echo "Could not find TLS cert/key files, run download-certs.sh first"
  exit 1
fi

# Put headers in a file so the AUTH_HEADER is not exposed as command line argument
HEADERS_FILE="${SCRIPT_DIR}/.${ENV}.headers"
touch "$HEADERS_FILE"
chmod 600 "$HEADERS_FILE"
trap "/bin/rm -f $HEADERS_FILE" EXIT
{
  printf 'Authorization: Bearer %s\n' "$DCES_AUTH_TOKEN"
} >"$HEADERS_FILE"

curl https://api.${ENV}.laa-debt-collection.service.justice.gov.uk/${URI} \
    --cert $CERTS_DIR/dcestesting-int-${ENV}.crt \
    --key $CERTS_DIR/dcestesting-int-${ENV}.key \
    -H "@$HEADERS_FILE" \
    -w "\nHTTP Status: %{http_code}\n"
