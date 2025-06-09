#!/bin/bash

set -e

USER="$1"
PASSWORD="$2"
HOSTNAME="$3"
IP="$4"

echo "[DynHost] Mise à jour du DNS pour $HOSTNAME -> $IP"

RESPONSE=$(curl -s "https://www.ovh.com/nic/update?system=dyndns&hostname=${HOSTNAME}&myip=${IP}" \
  --user "${USER}:${PASSWORD}")

echo "[DynHost] Réponse : $RESPONSE"
