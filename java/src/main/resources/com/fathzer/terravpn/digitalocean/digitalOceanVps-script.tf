provider "digitalocean" {
  token = var.digital_ocean_token
}

data "digitalocean_ssh_key" "terraform" {
  name = var.digitalocean_ssh_key_name
}

resource "digitalocean_droplet" "vpn" {
    image = "docker-20-04"
    name = "my-own-vpn"
    region = var.vps_zone
    size = "s-1vcpu-1gb"
    ssh_keys = [
      data.digitalocean_ssh_key.terraform.id
    ]
}

locals {
  vps_ip_address = digitalocean_droplet.vpn.ipv4_address
}
