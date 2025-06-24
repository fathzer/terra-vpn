provider "vultr" {
  api_key = var.token
}

data "vultr_ssh_key" "terraform" {
  filter {
    name   = "name"
    values = [var.ssh_key_name]
  }
}

resource "vultr_instance" "vpn" {
  plan     = var.instance_type
  region   = var.zone
  os_id    = 477 # Debian 11 (Found no way to directly install docker)
  label    = "my-own-vpn-${replace(timestamp(),":","")}"
  hostname = "vpn-instance"
  ssh_key_ids = [
    data.vultr_ssh_key.terraform.id
  ]
  enable_ipv6 = false
  backups      = "disabled"
  ddos_protection = false
}

locals {
  ip_address = vultr_instance.vpn.main_ip
}
