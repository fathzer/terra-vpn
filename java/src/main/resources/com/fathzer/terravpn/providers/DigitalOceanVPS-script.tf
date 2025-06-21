provider "digitalocean" {
  token = var.token
}

data "digitalocean_ssh_key" "terraform" {
  name = var.ssh_key_name
}

resource "digitalocean_droplet" "vpn" {
    image = "docker-20-04"
    name = "my-own-vpn"
    region = var.zone
    size = var.instance_type
    ssh_keys = [
      data.digitalocean_ssh_key.terraform.id
    ]
}

locals {
  ip_address = digitalocean_droplet.vpn.ipv4_address
}
