terraform {
  required_providers {
    %required_providers%
  }
  required_version = ">= 1.12.2"
}

%vps_script%

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

