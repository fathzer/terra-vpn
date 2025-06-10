#!/bin/bash

# Vérifier si un nom d'utilisateur est fourni
if [ $# -eq 0 ]; then
    echo "Usage: $0 <nom_utilisateur>"
    echo "Exemple: $0 johndoe"
    exit 1
fi

USERNAME=$1
CONFIG_FILE="${USERNAME}.ovpn"

# Vérifier si l'utilisateur existe déjà
if [ -f "/etc/openvpn/pki/private/${USERNAME}.key" ]; then
    echo "[ERREUR] L'utilisateur '${USERNAME}' existe déjà."
    exit 1
fi

echo "Création du certificat client pour ${USERNAME}..."
docker run -v /etc/openvpn:/etc/openvpn --rm -it kylemanna/openvpn easyrsa build-client-full "${USERNAME}" nopass

# Vérifier si la création du certificat a réussi
if [ $? -ne 0 ]; then
    echo "[ERREUR] Échec de la création du certificat pour ${USERNAME}"
    exit 1
fi

echo "Génération du fichier de configuration..."
docker run -v /etc/openvpn:/etc/openvpn --rm kylemanna/openvpn ovpn_getclient "${USERNAME}" > "${CONFIG_FILE}"

# Vérifier si la génération du fichier a réussi
if [ $? -eq 0 ] && [ -f "${CONFIG_FILE}" ]; then
    echo ""
    echo "=================================================================="
    echo "Configuration VPN créée avec succès pour l'utilisateur: ${USERNAME}"
    echo "Fichier de configuration: ${PWD}/${CONFIG_FILE}"
    echo "=================================================================="
    echo ""
    
    # Afficher le contenu du fichier
    cat "${CONFIG_FILE}"
else
    echo "[ERREUR] Échec de la génération du fichier de configuration"
    exit 1
fi
