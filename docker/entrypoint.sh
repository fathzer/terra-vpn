#!/bin/sh

# Generate keys if they don't exist
if [ ! -f /root/.ssh/id_rsa ]; then
  echo "[*] No SSH key found, generating new keys..."
  mkdir -p /root/.ssh
  ssh-keygen -t rsa -b 4096 -N "" -f /root/.ssh/id_rsa

  echo "\n[*] SSH key successfully generated."
  echo "\n👉 Copy this public key to Scaleway (console > SSH Keys):"
  echo "------------------------------------------------------------"
  cat /root/.ssh/id_rsa.pub
  echo "------------------------------------------------------------"
  echo "\n❗ Important: Redeploy Terraform once the key has been added to Scaleway."
  echo "\n➡️  Manual SSH access command:"
  echo "ssh -i ~/.ssh/id_rsa root@<IP_PUBLIQUE_DU_SERVEUR>"
  echo

  # Pause interactive pour ne pas exécuter Terraform tout de suite
  echo "Press Enter to continue once the key has been added..."
  read dummy
fi

# Execute the Terraform command passed to the container
exec terraform "$@"
