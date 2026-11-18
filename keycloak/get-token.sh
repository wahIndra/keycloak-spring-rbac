#!/usr/bin/env bash
# Get an access token from the LOCAL Keycloak (see docker-compose.yml).
#
# Usage:
#   ./get-token.sh client <client-id>        # client credentials; asks for the secret
#   ./get-token.sh user <username>           # user login; asks for the password
#
# Examples:
#   ./get-token.sh client partner-xyz
#   ./get-token.sh user alice
#
# Secrets and passwords are read from the terminal (or from CLIENT_SECRET / USER_PASSWORD),
# so they don't end up in your shell history.
#
# Requires: curl, jq

set -euo pipefail

ISSUER="${KEYCLOAK_ISSUER_URI:-http://localhost:8080/realms/myrealm}"
TOKEN_URL="$ISSUER/protocol/openid-connect/token"
MODE="${1:-}"
NAME="${2:-}"

usage() {
  sed -n '2,15p' "$0"
  exit 1
}

[ -n "$MODE" ] && [ -n "$NAME" ] || usage

request_token() {
  local response
  response=$(curl -sS --fail-with-body -X POST "$TOKEN_URL" "$@") || {
    echo "Token request failed: $response" >&2
    exit 1
  }
  echo "$response" | jq -er .access_token
}

case "$MODE" in
  client)
    if [ -z "${CLIENT_SECRET:-}" ]; then
      read -rsp "Client secret for $NAME: " CLIENT_SECRET </dev/tty; echo >&2
    fi
    request_token \
      --data-urlencode grant_type=client_credentials \
      --data-urlencode client_id="$NAME" \
      --data-urlencode client_secret="$CLIENT_SECRET"
    ;;
  user)
    if [ -z "${USER_PASSWORD:-}" ]; then
      read -rsp "Password for $NAME: " USER_PASSWORD </dev/tty; echo >&2
    fi
    # "dev-cli" exists in the LOCAL demo realm only. Real apps log users in with
    # authorization code + PKCE (see the web-portal client).
    request_token \
      --data-urlencode grant_type=password \
      --data-urlencode client_id=dev-cli \
      --data-urlencode username="$NAME" \
      --data-urlencode password="$USER_PASSWORD"
    ;;
  *)
    usage
    ;;
esac
