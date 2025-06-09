# terra-vpn

A terraform script to deploy an OpenVPN server to Scaleway

This project automates the deployment of an OpenVPN server on a **Scaleway DEV1-S instance** in **Amsterdam**, using **Terraform**, **OVH DynHost**, and a prebuilt Docker container.

---

## 🚀 Features

- 🔐 Automatically installs and configures OpenVPN using a local PKI and config folder
- 🖥️ Deploys a public server (DEV1-S) on Scaleway
- 📡 Automatically updates your DynHost (OVH) with the new IP address
- 🐳 Everything runs inside a Docker container
- 👥 Supports adding VPN users manually after deployment

---

## 🧰 Requirements

- [x] Docker installed on your machine
- [x] A Scaleway account with API keys
- [x] An OVH domain name with DynHost credentials
- [x] A local folder named `openvpn/` containing:
  - `server.conf`
  - The full PKI: `ca.crt`, `server.crt`, `server.key`, `dh.pem`, etc.

---

## 📁 Project Structure

```text
.
├── Dockerfile
├── entrypoint.sh
├── main.tf
├── variables.tf
├── terraform.tfvars.example
├── openvpn/                # Your prebuilt OpenVPN configuration and PKI
│   └── server.conf
├── scripts/
│   └── update_dynhost.sh
```

## ⚙️ Setup Instructions

1. 🧪 Configure your variables

Copy and edit the variable file:

```bash
cp terraform.tfvars.example terraform.tfvars
nano terraform.tfvars
```

2. 🏗️ Build the Docker image

```bash
docker build -t terraform-openvpn .
```

3. ⚙️ Init terraform

```bash
# Run Terraform init (only once)
docker run --rm -v "$PWD":/workspace terraform-openvpn init
```

## Deploy the VPN server, create users, delete server

1. 🚀 Deploy the VPN server

```bash
# Apply Terraform
docker run --rm -v "$PWD":/workspace terraform-openvpn apply
```

This will:

- Create the VPS
- Install OpenVPN and copy your local config
- Start the OpenVPN server
- Update your DynHost via OVH
//TODO: Add the ssh key generation and copy to scaleway

2. 👥 Create a new user

```bash
# Create a new user
# Replace <username> with the desired username
# Replace <password> with the desired password
docker run --rm -v "$PWD":/workspace terraform-openvpn create-user <username> <password>
```

This will:

- Create a new user in the OpenVPN server
- Generate a new client certificate and key
- Copy the client configuration to the `openvpn/clients` folder

3. 🗑️ Delete the VPN server

```bash
# Destroy the VPS
docker run --rm -v "$PWD":/workspace terraform-openvpn destroy
```

This will:

- Delete the VPS
- Remove the OpenVPN server
- Remove the DynHost update script