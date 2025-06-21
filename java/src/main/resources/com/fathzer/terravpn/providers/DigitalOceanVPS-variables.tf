variable "token" {
  type        = string
  description = "DigitalOcean token"
}

variable "ssh_key_name" {
    type = string
    default = "terra-vpn"
}