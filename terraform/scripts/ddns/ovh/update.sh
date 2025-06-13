#!/bin/sh

set -e

# Configuration
USER="$1"
PASSWORD="$2"
HOSTNAME="$3"
IP="$4"

echo "[OVH DynHost] Mise à jour du DNS pour $HOSTNAME -> $IP"

# Create a temporary file to store the response
RESPONSE_FILE="$(mktemp)"
trap 'rm -f "$RESPONSE_FILE"' EXIT

# Build the URL
URL="https://www.ovh.com/nic/update?system=dyndns&hostname=${HOSTNAME}&myip=${IP}"

# Debug: Show wget command (without password)
echo "[OVH DynHost] Exécution de la commande wget..."

# Make the API call with wget
set +e
/usr/bin/wget -q -O "$RESPONSE_FILE" \
  --user="$USER" --password="$PASSWORD" \
  --no-check-certificate \
  "$URL" 2>&1
WGET_EXIT=$?
set -e

# Get the response
RESPONSE=$(cat "$RESPONSE_FILE" 2>/dev/null || echo "[No response]")

# Clean up the response file
rm -f "$RESPONSE_FILE"

if [ $WGET_EXIT -ne 0 ]; then
    echo "[OVH DynHost] Erreur lors de l'exécution de wget (code $WGET_EXIT)"
    echo "[OVH DynHost] Sortie de wget:"
    echo "$RESPONSE"
    exit 1
fi

echo "[OVH DynHost] Réponse: $RESPONSE"

# Check for error responses
if echo "$RESPONSE" | grep -q -E '^nohost|^notfqdn|^badagent|^badauth|^badresolv|^badparam|^error'; then
    echo "[OVH DynHost] Erreur lors de la mise à jour DNS"
    exit 1
fi

echo "[OVH DynHost] Mise à jour DNS réussie"
exit 0
