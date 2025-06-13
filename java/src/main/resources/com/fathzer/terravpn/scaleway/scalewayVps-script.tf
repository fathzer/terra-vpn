provider "scaleway" {
  access_key = var.scaleway_access_key
  secret_key = var.scaleway_secret_key
  project_id = var.scaleway_project_id
  zone       = var.zone
}

# Create an instance IP for the server
resource "scaleway_instance_ip" "vpn_ip" {
  zone = var.zone
  ip   = scaleway_instance_ip.vpn_ip.address
}

resource "scaleway_instance_server" "vpn_server" {
  name            = "openvpn"
  image           = "docker"
  type            = var.instance_type
  zone            = var.zone
  tags            = ["openvpn","docker"]
  ip_id           = scaleway_instance_ip.vpn_ip.id

  root_volume {
    size_in_gb  = var.root_volume_size_gb
    volume_type = "l_ssd"
  }
  
  user_data = {
    cloud-init = <<-EOT
      #cloud-config
      ssh_authorized_keys:
        - "${file("~/.ssh/id_rsa.pub")}"
    EOT
  }
}