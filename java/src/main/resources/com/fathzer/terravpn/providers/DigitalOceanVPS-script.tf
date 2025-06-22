provider "digitalocean" {
  token = var.token
}

data "digitalocean_ssh_key" "terraform" {
  name = var.ssh_key_name
}

resource "digitalocean_droplet" "vpn" {
    image = "docker-20-04"
    name = "my-own-vpn--${local.clean_timestamp}"
    region = var.zone
    size = var.instance_type
    ssh_keys = [
      data.digitalocean_ssh_key.terraform.id
    ]
}

resource "digitalocean_firewall" "vpn_firewall" {
  name = "vpn-firewall-rules"
  
  # Explicitly depend on the droplet
  depends_on = [digitalocean_droplet.vpn]
  
  # Apply to the VPN droplet
  droplet_ids = [digitalocean_droplet.vpn.id]
  
  # Allow SSH
  inbound_rule {
    protocol         = "tcp"
    port_range       = "22"
    source_addresses = ["0.0.0.0/0", "::/0"]
  }
  
  # Allow OpenVPN UDP port
  inbound_rule {
    protocol         = var.protocol
    port_range       = var.port
    source_addresses = ["0.0.0.0/0", "::/0"]
  }
  
  # Allow all outbound traffic
  outbound_rule {
    protocol                = "tcp"
    port_range              = "1-65535"
    destination_addresses   = ["0.0.0.0/0", "::/0"]
  }
  
  outbound_rule {
    protocol                = "udp"
    port_range              = "1-65535"
    destination_addresses   = ["0.0.0.0/0", "::/0"]
  }
  
  outbound_rule {
    protocol                = "icmp"
    destination_addresses   = ["0.0.0.0/0", "::/0"]
  }
}

locals {
  ip_address = digitalocean_droplet.vpn.ipv4_address
}
