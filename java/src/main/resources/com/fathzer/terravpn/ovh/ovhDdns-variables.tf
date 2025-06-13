variable "ovhDdns_user" {
  type        = string
  description = "OVH DynHost user"
  default     = ""
}

variable "ovhDdns_password" {
  type        = string
  description = "OVH DynHost password"
  sensitive   = true
  default     = ""
}