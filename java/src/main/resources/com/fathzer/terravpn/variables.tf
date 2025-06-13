variable "zone" {
  type        = string
  description = "Zone VPS où déployer l'instance"
}

variable "instance_type" {
  type        = string
  description = "Type d'instance VPS à utiliser"
}

variable "root_volume_size_gb" {
  type        = number
  description = "Taille du volume racine du VPS en Go"
  default     = 10
  
  validation {
    condition     = var.root_volume_size_gb >= 1 && var.root_volume_size_gb <= 2000
    error_message = "La taille du volume racine doit être comprise entre 1 et 2000 Go."
  }
}

variable "openvpn_protocol" {
  type        = string
  description = "Protocole à utiliser pour OpenVPN (udp ou tcp)"
  default     = "udp"
  
  validation {
    condition     = contains(["udp", "tcp"], lower(var.openvpn_protocol))
    error_message = "Le protocole doit être 'udp' ou 'tcp'."
  }
}

variable "openvpn_port" {
  type        = number
  description = "Port à utiliser pour OpenVPN"
  default     = 1194
  
  validation {
    condition     = var.openvpn_port > 0 && var.openvpn_port <= 65535
    error_message = "Le port doit être compris entre 1 et 65535."
  }
}

variable "dns_servers" {
  type        = list(string)
  description = "Liste des serveurs DNS à utiliser pour les clients OpenVPN (1-4 serveurs)"
  default     = ["1.1.1.1", "1.0.0.1"]
  
  validation {
    condition     = length(var.dns_servers) > 0 && length(var.dns_servers) <= 4
    error_message = "Vous devez spécifier entre 1 et 4 serveurs DNS."
  }
}

variable "ddns_hostname" {
  type        = string
  description = "Nom d'hôte complet pour la mise à jour DDNS (ex: vpn.example.com)"
}

