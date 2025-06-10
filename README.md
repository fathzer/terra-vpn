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
├── docker/                 # Docker configuration files
│   ├── Dockerfile
│   └── entrypoint.sh
├── terraform/              # Terraform configuration files
│   ├── scripts/
│   │   └── update_dynhost.sh
│   ├── main.tf
│   └── variables.tf
├── launch.sh               # Helper script for Terraform operations
└── README.md
```

## ⚙️ Setup Instructions

1. 🧪 Configure your variables

Copy and edit the variable file:

```bash
cp terraform.tfvars.example terraform.tfvars
nano terraform.tfvars
```

2. 🏗️ Build the Docker image //TODO or pull from dockerhub

```bash
docker build -f docker/Dockerfile -t terraform-openvpn .
```

3. 🔑 Copy your ssh keys to the `data/ssh` folder (Optional)

SSH keys are required to connect to the virtual private server to install openvpn and copy your local config on it. If you already have a key pair registered on Scaleway, you can copy it to the `data/ssh` folder (there's two files: `id_rsa` and `id_rsa.pub`).

If you have none, no problem, the next step will generate them for you.

4. ⚙️ Init terraform

```bash
# Run Terraform init (only once)
./launch.sh init
```

After this step, a data folder will be created in the root directory with the following structure:

```text
.
├── data/                   # Data files
│   ├── openvpn/            # OpenVPN configuration files
│   ├── ssh/                # SSH keys
│   └── terraform/          # Terraform state files
├── ...
```

If you did not copy your ssh keys in previous step, their are generated for you and saved in `data/ssh` folder and displayed in the console. You are invited to register them on Scaleway console (console > SSH Keys).
Once it's done, press enter to continue.

## Deploy the VPN server, create users, delete server

1. 🚀 Deploy the VPN server

```bash
# Deploy the VPN server
./launch.sh apply
```

This will:

- Create the VPS
- Install OpenVPN and copy your local config if you have provided one. If not, it will generate a new one.
- Start the OpenVPN server
- Update your DynHost via OVH

2. 👥 Create a new user

First connect to the VPS using the SSH key:

```bash
ssh -i data/ssh/id_rsa root@<SERVER_ADDRESS>
```
Don't forget to ignore server key, because it may change next time you deploy the server.

Once connected, you can create a new user:

```bash
# Create a new user
# Replace <username> with the desired username
create_vpn_user <username>
```

This will:

- Create a new user in the OpenVPN server
- Generate a new client certificate and key
- Display the client configuration file on the screen.

Copy this file to your local machine and save it as `<username>.ovpn` and add it to your OpenVPN client.

3. 🗑️ Delete the VPN server

```bash
# Destroy the VPS
./launch.sh destroy
```

This will delete all the resources used by the OpenVPN server (VPS, IP, etc...).
