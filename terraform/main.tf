terraform {
  required_providers {
    scaleway = {
      source  = "scaleway/scaleway"
      version = "~> 2.36"
    }
  }
}

provider "scaleway" {
  access_key = var.scaleway_access_key
  secret_key = var.scaleway_secret_key
  project_id = var.scaleway_project_id
  zone       = "nl-ams-1"
  region     = "nl-ams"
}

resource "scaleway_instance_ip" "vpn_ip" {}

resource "scaleway_instance_server" "vpn_server" {
  name                = "openvpn"
  image               = "ubuntu_jammy"
  commercial_type     = "DEV1-S"
  zone                = "nl-ams-1"
  tags                = ["openvpn"]
  enable_ipv6         = false
  dynamic_ip_required = false

  public_ip {
    id = scaleway_instance_ip.vpn_ip.id
  }

  ssh_key = var.ssh_key_name
}

resource "null_resource" "provision_openvpn" {
  depends_on = [scaleway_instance_server.vpn_server]

  connection {
    type        = "ssh"
    user        = "root"
    host        = scaleway_instance_server.vpn_server.public_ip[0].address
    private_key = file("/root/.ssh/id_rsa")
  }

  # Copie de la configuration OpenVPN (avec PKI déjà prête)
  provisioner "file" {
    source      = "openvpn"
    destination = "/etc/openvpn"
  }

  # Installation d'OpenVPN
  provisioner "remote-exec" {
    inline = [
      "apt-get update",
      "apt-get install -y openvpn iptables curl",
      "systemctl enable openvpn@server",
      "systemctl start openvpn@server"
    ]
  }

  # Appel d'un script DynHost local pour mettre à jour l'IP publique sur OVH
  provisioner "local-exec" {
    command = "bash scripts/update_dynhost.sh ${var.dynhost_user} ${var.dynhost_password} ${var.dynhost_hostname} ${scaleway_instance_server.vpn_server.public_ip[0].address}"
  }
}
