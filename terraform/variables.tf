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

variable "ssh_key_name" {
  type        = string
  description = "Nom de la clé SSH existante sur Scaleway"
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
