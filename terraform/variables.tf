variable "scaleway_access_key" {
  type        = string
  description = "Clé d'accès Scaleway"
}

variable "scaleway_secret_key" {
  type        = string
  description = "Clé secrète Scaleway"
}

variable "scaleway_project_id" {
  type        = string
  description = "ID du projet Scaleway"
}

# DynHost OVH
variable "dynhost_user" {
  type        = string
  description = "Identifiant DynHost OVH"
}

variable "dynhost_password" {
  type        = string
  description = "Mot de passe DynHost OVH"
  sensitive   = true
}

variable "dynhost_hostname" {
  type        = string
  description = "Nom de domaine ou sous-domaine DynHost"
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

variable "openvpn_protocol" {
  type        = string
  description = "Protocole à utiliser pour OpenVPN (udp ou tcp)"
  default     = "udp"
  
  validation {
    condition     = contains(["udp", "tcp"], lower(var.openvpn_protocol))
    error_message = "Le protocole doit être 'udp' ou 'tcp'."
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

variable "instance_type" {
  type        = string
  description = "Type d'instance Scaleway à utiliser"
  default     = "DEV1-S"
}

variable "zone" {
  type        = string
  description = "Zone Scaleway où déployer l'instance"
  default     = "pl-waw-1"
}

variable "root_volume_size_gb" {
  type        = number
  description = "Taille du volume racine en Go"
  default     = 10
  
  validation {
    condition     = var.root_volume_size_gb >= 1 && var.root_volume_size_gb <= 2000
    error_message = "La taille du volume racine doit être comprise entre 1 et 2000 Go."
  }
}
