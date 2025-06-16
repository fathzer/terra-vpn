variable "digital_ocean_token" {
  type        = string
  description = "DigitalOcean token"
}

variable "digitalocean_ssh_key_name" {
    type = string
    default = "terra-vpn"
}