#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Double check SCRIPT_DIR is the right directory
if [[ ! "$SCRIPT_DIR" =~ laa-dces-drc-integration/scripts$ ]]; then
  echo "Unexpected script directory: $SCRIPT_DIR"
  exit 1
fi

# Remove the TLS certificates and key files
echo /bin/rm -rf "$SCRIPT_DIR"/tls-certs-keys

# Remove any header files that may (but shouldn't) get left behind
echo /bin/rm -f "$SCRIPT_DIR/.*.headers"