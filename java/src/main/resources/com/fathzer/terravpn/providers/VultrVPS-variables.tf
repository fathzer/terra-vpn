variable "token" {
  type        = string
  description = "Vultr API token"
}

variable "ssh_key_name" {
    type = string
    default = "terra-vpn"
}