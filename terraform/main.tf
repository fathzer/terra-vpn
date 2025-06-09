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
  zone       = "nl-ams-1"
  region     = "nl-ams"
}

# Create an instance IP for the server
resource "scaleway_instance_ip" "vpn_ip" {
  zone = "nl-ams-1"
}

resource "scaleway_instance_server" "vpn_server" {
  name            = "openvpn"
  image           = "ubuntu_jammy"
  type            = "DEV1-S"
  zone            = "nl-ams-1"
  tags            = ["openvpn"]
  ip_id           = scaleway_instance_ip.vpn_ip.id

  root_volume {
    size_in_gb  = 20
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
      "apt-get update",
      "apt-get install -y openvpn iptables curl",
      "mkdir -p /etc/openvpn/server"
    ]
  }

  # Copie de la configuration OpenVPN si le dossier existe et n'est pas vide
  provisioner "file" {
    source      = "openvpn/"
    destination = "/etc/openvpn"
    on_failure  = continue
  }

  # Redémarre OpenVPN si la configuration a été copiée
  provisioner "remote-exec" {
    inline = [
      "[ -f /etc/openvpn/server.conf ] && systemctl enable --now openvpn-server@server.service || echo 'No server.conf found, skipping OpenVPN start'"
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
        sh /tmp/update_dynhost.sh '${var.dynhost_user}' '${var.dynhost_password}' '${var.dynhost_hostname}' '${scaleway_instance_server.vpn_server.public_ip}'
      else
        echo "Error: Script not found at /workspace/scripts/update_dynhost.sh"
        exit 1
      fi
    EOT
  }
}
