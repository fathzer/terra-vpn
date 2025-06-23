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

variable "ssh_user" {
  type        = string
  description = "SSH user to use to connect to the server"
  default     = "root"
}
