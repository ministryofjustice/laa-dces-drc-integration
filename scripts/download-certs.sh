#!/usr/bin/env bash
set -euo pipefail

# Usage message
usage() {
  echo "Usage: $0 <env>"
  echo "  <env> must be one of: dev, test, uat, prod"
  exit 1
}

# Check argument count
if [[ $# -ne 1 ]]; then
  usage
fi

ENV="$1"

# Validate environment
case "$ENV" in
  dev|test|uat|prod)
    ;; # valid
  *)
    echo "Invalid environment: $ENV"
    usage
    ;;
esac

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEST_DIR="$SCRIPT_DIR"/tls-certs-keys/"$ENV"
mkdir -p "$DEST_DIR"

aws --profile dces-cert-store s3 cp s3://cloud-platform-ef31e00222971340edc20336f58525cb/tls-certs-keys/"$ENV"/dcestesting-int-"$ENV".crt "$DEST_DIR"
aws --profile dces-cert-store s3 cp s3://cloud-platform-ef31e00222971340edc20336f58525cb/tls-certs-keys/"$ENV"/dcestesting-int-"$ENV".key "$DEST_DIR"

# Set all file to read only and make all directories navigable
find "$DEST_DIR" -type f -exec chmod 400 {} +
find "$DEST_DIR" -type d -exec chmod 700 {} +
