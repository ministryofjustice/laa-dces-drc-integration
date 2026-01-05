#!/usr/bin/env bash
set -eo pipefail

usage() {
  echo "Usage: $0 [-r] <env>"
  echo "  -r       Output the raw token only"
  echo "  env      The environment, one of dev|test|uat|prod"
}

raw_output=false
while getopts ":rh" opt; do
  case "$opt" in
    r) raw_output=true ;;
    h) usage; exit 0 ;;
    \?) echo "Unknown option: -$OPTARG" >&2; usage; exit 2 ;;
  esac
done

shift $((OPTIND -1))

ENV=$1
case "$ENV" in
  dev|test|uat|prod)
    ;;
  *)
    usage; exit 1 ;;
esac

# Read credentials from stdin without echoing it
echo "Enter Cognito credentials for $ENV:" >/dev/stderr
CLIENT_ID=""
while [[ -z "$CLIENT_ID" ]]; do
    printf "Client ID: " >/dev/stderr
    read -rs CLIENT_ID
    printf "\n" >/dev/stderr

    if [[ -z "$CLIENT_ID" ]]; then
      echo "Client ID cannot be empty"
    fi
done

CLIENT_SECRET=""
while [[ -z "$CLIENT_SECRET" ]]; do
    printf "Client Secret: " >/dev/stderr
    read -rs CLIENT_SECRET
    printf "\n" >/dev/stderr

    if [[ -z "$CLIENT_SECRET" ]]; then
      echo "Client Secret cannot be empty"
    fi
done

# Set the host for Cognito
COGNITO_DOMAIN="dces-drc-api-${ENV}.auth.eu-west-2.amazoncognito.com"

# Base64 encode client_id:client_secret
AUTH_HEADER=$(echo -n "${CLIENT_ID}:${CLIENT_SECRET}" | base64)

# Put headers in a file so the AUTH_HEADER is not exposed as command line argument
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HEADERS_FILE="${SCRIPT_DIR}/.${ENV}.headers"
touch $HEADERS_FILE
chmod 600 "$HEADERS_FILE"
trap "/bin/rm -f $HEADERS_FILE" EXIT
{
  printf 'Content-Type: application/x-www-form-urlencoded\n'
  printf 'Authorization: Basic %s\n' "$AUTH_HEADER"
} >"$HEADERS_FILE"

# Request access token
OUTPUT=$(curl -s -X POST "https://${COGNITO_DOMAIN}/oauth2/token" \
  -H "@$HEADERS_FILE" \
  -d "grant_type=client_credentials")

# Check we have a valid token
access_token=$(echo "$OUTPUT" | jq -r .access_token)
if [[ "$access_token" == "null" ]]; then
  echo $OUTPUT
  exit 1
fi

if $raw_output; then
    echo $access_token
else
    TTL=$(echo "$OUTPUT" | jq -r .expires_in)
    echo
    echo "Run this command in the shell to store the token"
    echo
    echo "export DCES_AUTH_TOKEN=$access_token"
    echo
    echo "Token expires at: $(date -v+${TTL}S)"
    echo
fi