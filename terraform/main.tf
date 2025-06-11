terraform {
  required_providers {
    scaleway = {
      source  = "scaleway/scaleway"
      version = "~> 2.55.0"
    }
  }
  required_version = ">= 1.0.0"
}

provider "scaleway" {
  access_key = var.scaleway_access_key
  secret_key = var.scaleway_secret_key
  project_id = var.scaleway_project_id
  zone       = var.zone
}

# Create an instance IP for the server
resource "scaleway_instance_ip" "vpn_ip" {
  zone = var.zone
}

resource "scaleway_instance_server" "vpn_server" {
  name            = "openvpn"
  image           = "docker"
  type            = var.instance_type
  zone            = var.zone
  tags            = ["openvpn","docker"]
  ip_id           = scaleway_instance_ip.vpn_ip.id

  root_volume {
    size_in_gb  = var.root_volume_size_gb
    volume_type = "l_ssd"
  }
  
  user_data = {
    cloud-init = <<-EOT
      #cloud-config
      ssh_authorized_keys:
        - "${file("~/.ssh/id_rsa.pub")}"
    EOT
  }
}

resource "null_resource" "provision_openvpn" {
  depends_on = [scaleway_instance_server.vpn_server]

  connection {
    type        = "ssh"
    user        = "root"
    host        = scaleway_instance_ip.vpn_ip.address
    private_key = file("~/.ssh/id_rsa")
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
            -u ${var.openvpn_protocol}://${var.dynhost_hostname}:${var.openvpn_port} \
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
      "  echo '  scp -r root@${scaleway_instance_ip.vpn_ip.address}:/etc/openvpn ./openvpn'",
      "  rm -f /tmp/new_installation_marker",
      "fi"
    ]
  }

  # Appel d'un script DynHost local pour mettre à jour l'IP publique sur OVH
  provisioner "local-exec" {
    command = <<-EOT
      # Convert line endings and execute
      if [ -f "/workspace/scripts/update_dynhost.sh" ]; then
        echo "=== Executing script ==="
        # Convert Windows line endings to Unix and execute
        tr -d '\r' < /workspace/scripts/update_dynhost.sh > /tmp/update_dynhost.sh && \
        chmod +x /tmp/update_dynhost.sh && \
        sh /tmp/update_dynhost.sh '${var.dynhost_user}' '${var.dynhost_password}' '${var.dynhost_hostname}' '${scaleway_instance_ip.vpn_ip.address}'
      else
        echo "Error: Script not found at /workspace/scripts/update_dynhost.sh"
        exit 1
      fi
    EOT
  }
}
