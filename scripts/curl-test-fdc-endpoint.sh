#!/usr/bin/env bash
set -euo pipefail

# Usage message
usage() {
  echo "Usage: $0 <env>"
  echo "  <env> must be one of: dev, test, uat"
  exit 1
}

# Check argument count
if [[ $# -ne 1 ]]; then
  usage
fi

ENV="$1"

# Validate environment
case "$ENV" in
  dev|test|uat)
    ;; # valid
  *)
    echo "Invalid environment: $ENV"
    usage
    ;;
esac

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

$SCRIPT_DIR/curl-endpoint.sh $ENV /api/dces/test/fdc
