terraform {
  required_providers {
    %required_providers%
  }
  required_version = ">= 1.12.2"
}

%vps_script%

# Wait for the VPS to be ready
resource "null_resource" "provision_openvpn" {
  depends_on = [%vps_completed%]

  connection {
    type        = "ssh"
    user        = "root"
    host        = local.ip_address
    private_key = file("%private_sshkey_path%")
  }

  # Installation d'OpenVPN
  provisioner "remote-exec" {
    inline = [
      "echo 'Server is up'"
    ]
  }
}

# Update local file containing IP adress
resource "null_resource" "create_and_delete_file" {
  depends_on = [%vps_completed%]

  provisioner "local-exec" {
    when    = create
    command = "echo '${local.ip_address}' > ip.txt"
  }

  provisioner "local-exec" {
    when    = destroy
    command = "rm -f ip.txt"
  }
}

