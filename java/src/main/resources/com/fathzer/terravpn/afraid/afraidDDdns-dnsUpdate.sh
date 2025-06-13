#!/bin/sh

set -e

# Configuration
TOKEN="$1"
HOSTNAME="$2"
IP="$3"

echo "[Afraid.org] Mise à jour du DNS pour $HOSTNAME -> $IP"

# Create a temporary file to store the response
RESPONSE_FILE="$(mktemp)"
trap 'rm -f "$RESPONSE_FILE"' EXIT

# Build the URL
URL="https://freedns.afraid.org/nic/update?hostname=${HOSTNAME}&myip=${IP}&v=2&token=${TOKEN}"

# Debug: Show wget command (without token)
echo "[Afraid.org] Exécution de la commande wget..."

# Make the API call with wget
set +e
/usr/bin/wget -q -O "$RESPONSE_FILE" \
  --no-check-certificate \
  "$URL" 2>&1
WGET_EXIT=$?
set -e

# Get the response
RESPONSE=$(cat "$RESPONSE_FILE" 2>/dev/null || echo "[No response]")

# Clean up the response file
rm -f "$RESPONSE_FILE"

if [ $WGET_EXIT -ne 0 ]; then
    echo "[Afraid.org] Erreur lors de l'exécution de wget (code $WGET_EXIT)"
    echo "[Afraid.org] Sortie de wget:"
    echo "$RESPONSE"
    exit 1
fi

echo "[Afraid.org] Réponse: $RESPONSE"

# Check for error responses
if echo "$RESPONSE" | grep -q -E '^nohost|^notfqdn|^badauth|^badip|^error'; then
    echo "[Afraid.org] Erreur lors de la mise à jour DNS"
    exit 1
fi

echo "[Afraid.org] Mise à jour DNS réussie"
exit 0
