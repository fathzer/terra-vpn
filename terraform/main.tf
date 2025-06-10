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
  zone       = "pl-waw-1"
  region     = "pl-waw"
}

# Create an instance IP for the server
resource "scaleway_instance_ip" "vpn_ip" {
  zone = "pl-waw-1"
}

resource "scaleway_instance_server" "vpn_server" {
  name            = "openvpn"
  image           = "docker"
  type            = "DEV1-S"
  zone            = "pl-waw-1"
  tags            = ["openvpn","docker"]
  ip_id           = scaleway_instance_ip.vpn_ip.id

  root_volume {
    size_in_gb  = 10
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
      "set -e",
      "if [ ! -d /etc/openvpn/pki ] || [ -z \"$(ls -A /etc/openvpn/pki 2>/dev/null)\" ]; then",
      "  echo 'Initializing OpenVPN PKI and creating server certificates...'",
      # Generate config with DNS leak protection
      "  docker run -v /etc/openvpn:/etc/openvpn --rm -it kylemanna/openvpn ovpn_genconfig \\",
      "    -u udp://${var.dynhost_hostname} \\",
      "    -p 'block-outside-dns' \\",
      "    -p 'dhcp-option DNS 1.1.1.1' \\",
      "    -p 'dhcp-option DNS 1.0.0.1' \\",
      "    -p 'redirect-gateway def1'",
      "  echo 'yes' | docker run -v /etc/openvpn:/etc/openvpn --rm -i kylemanna/openvpn ovpn_initpki nopass",
      "  # Create a marker file to indicate this is a new installation",
      "  touch /tmp/new_installation_marker",
      "else",
      "  echo 'Using existing OpenVPN configuration'",
      "fi"
    ]
  }

  # Démarre le conteneur OpenVPN avec les privilèges nécessaires
  provisioner "remote-exec" {
    inline = [
      <<-EOT
        # Stop and remove any existing container
        docker rm -f openvpn 2>/dev/null || true
        
        # Start OpenVPN with DNS leak protection
        docker run -d \
          --name openvpn \
          --restart unless-stopped \
          --cap-add=NET_ADMIN \
          --device=/dev/net/tun \
          --sysctl net.ipv6.conf.all.disable_ipv6=0 \
          --dns 1.1.1.1 \
          --dns 1.0.0.1 \
          -v /etc/openvpn:/etc/openvpn \
          -p 1194:1194/udp \
          kylemanna/openvpn
      EOT
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

  # Debug: List directories and files
  provisioner "local-exec" {
    command = <<-EOT
      echo -e "\n=== /workspace/scripts directory ==="
      ls -la /workspace/scripts 2>/dev/null || echo "/workspace/scripts not found"
    EOT
  }

  # Appel d'un script DynHost local pour mettre à jour l'IP publique sur OVH
  provisioner "local-exec" {
    command = <<-EOT
      # Debug: Show script content
      echo "=== Script content ==="
      cat /workspace/scripts/update_dynhost.sh
      
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
