terraform {
  required_providers {
    %required_providers%
  }
  required_version = ">= 1.12.2"
}

%vps_script%


resource "null_resource" "provision_openvpn" {
  depends_on = [%vps_completed%]

  connection {
    type        = "ssh"
    user        = "root"
    host        = local.vps_ip_address
    private_key = file("${path.module}/.ssh/id_rsa")
  }

  # Installation d'OpenVPN
  provisioner "remote-exec" {
    inline = [
      "docker pull kylemanna/openvpn"
    ]
  }

  # Copie de la configuration OpenVPN si le dossier existe et n'est pas vide
  provisioner "file" {
    source      = "openvpn/"
    destination = "/etc/openvpn"
    on_failure  = continue
  }

  # Copie du script de création d'utilisateur
  provisioner "file" {
    source      = "scripts/create_vpn_user.sh"
    destination = "/usr/local/bin/create_vpn_user"
  }

  # Rendre le script exécutable
  provisioner "remote-exec" {
    inline = [
      "chmod +x /usr/local/bin/create_vpn_user"
    ]
  }  

  # Vérifie si la configuration OpenVPN existe et initialise si nécessaire
  provisioner "remote-exec" {
    inline = [
      <<-EOSHELL
        #!/bin/bash
        set -e
        if [ ! -d /etc/openvpn/pki ] || [ -z "$(ls -A /etc/openvpn/pki 2>/dev/null)" ]; then
          echo 'Initializing OpenVPN PKI and creating server certificates...'
          
          # Build DNS options
          DNS_OPTS="-p 'block-outside-dns'"
          for dns in ${join(" ", var.dns_servers)}; do
            DNS_OPTS="\$DNS_OPTS -p 'dhcp-option DNS \$dns'"
          done
          
          # Generate OpenVPN config
          docker run -v /etc/openvpn:/etc/openvpn --rm -it kylemanna/openvpn ovpn_genconfig \
            -u ${var.openvpn_protocol}://${var.ddns_hostname}:${var.openvpn_port} \
            $DNS_OPTS \
            -p 'redirect-gateway def1'
            
          echo 'yes' | docker run -v /etc/openvpn:/etc/openvpn --rm -i kylemanna/openvpn ovpn_initpki nopass
          
          # Create a marker file to indicate this is a new installation
          touch /tmp/new_installation_marker
        else
          echo 'Using existing OpenVPN configuration'
        fi
      EOSHELL
    ]
  }

  # Démarre le conteneur OpenVPN avec les privilèges nécessaires
  provisioner "remote-exec" {
    inline = [
      <<-EOSHELL
        #!/bin/bash
        set -e

        # Stop and remove any existing container
        docker rm -f openvpn 2>/dev/null || true

        # Build DNS options
        DNS_OPTS=""
        for dns in ${join(" ", var.dns_servers)}; do
          DNS_OPTS="$DNS_OPTS --dns $dns"
        done

        # Start OpenVPN with DNS leak protection
        eval docker run -d \
          --name openvpn \
          --restart unless-stopped \
          --cap-add=NET_ADMIN \
          --device=/dev/net/tun \
          --sysctl net.ipv6.conf.all.disable_ipv6=0 \
          $DNS_OPTS \
          -v /etc/openvpn:/etc/openvpn \
          -p ${var.openvpn_port}:${var.openvpn_port}/${var.openvpn_protocol} \
          kylemanna/openvpn
      EOSHELL
    ]
  }

  # Clean up the marker file if this was a new installation
  provisioner "remote-exec" {
    inline = [
      "if [ -f /tmp/new_installation_marker ]; then",
      "  echo 'New OpenVPN installation detected. Configuration is available in /etc/openvpn on the server.'",
      "  echo 'To download the configuration, you can use:'",
      "  echo '  scp -r root@${local.vps_ip_address}:/etc/openvpn ./openvpn'",
      "  rm -f /tmp/new_installation_marker",
      "fi"
    ]
  }

  # Mise à jour DNS via le fournisseur configuré
  provisioner "local-exec" {
    command = <<-EOT
      #!/bin/bash
      set -e

      # Variables
      DDNS_SCRIPT="${path.root}/scripts/dnsUpdate.sh"
      PUBLIC_IP="${local.vps_ip_address}"
      
      echo "=== Mise à jour DNS pour ${var.ddns_hostname} ==="
      
      # Vérification du script
      if [ ! -f "$DDNS_SCRIPT" ]; then
        echo "Erreur: Script de mise à jour DNS introuvable"
        exit 1
      fi
      
      # Rendre le script exécutable
      chmod +x "$DDNS_SCRIPT"
      
      # Exécution du script spécifique au fournisseur
      echo "=== Executing script ==="
      # Convert Windows line endings to Unix and execute
      tr -d '\r' < $DDNS_SCRIPT > /tmp/update_dynhost.sh && \
      chmod +x /tmp/update_dynhost.sh && \
      sh /tmp/update_dynhost.sh %authent_arguments% '${var.ddns_hostname}' '${local.vps_ip_address}'
      
      echo "=== Mise à jour DNS terminée avec succès ==="
    EOT
  }
}
