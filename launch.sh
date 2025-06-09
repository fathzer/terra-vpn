#!/bin/bash
set -e

# Get the script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR" && pwd)"
TERRAFORM_SRC="$PROJECT_ROOT/terraform"
TERRAFORM_DEST="$PROJECT_ROOT/data/terraform"

# Create necessary directories
echo "[*] Creating Terraform destination directory: $TERRAFORM_DEST"
mkdir -p "$TERRAFORM_DEST"

echo "[*] Copying Terraform files from $TERRAFORM_SRC to $TERRAFORM_DEST"
if [ -d "$TERRAFORM_SRC" ]; then
    # Create destination directory structure
    mkdir -p "$TERRAFORM_DEST"
    
    # Recursively copy all files and directories from source to destination
    cp -rv "$TERRAFORM_SRC"/. "$TERRAFORM_DEST/" 2>/dev/null || true
    
    # Create .terraform directory if it doesn't exist
    mkdir -p "$TERRAFORM_DEST/.terraform"
    
    # Make all .sh files executable
    echo "[*] Making .sh files executable in $TERRAFORM_DEST"
    find "$TERRAFORM_DEST" -type f -name "*.sh" -exec chmod +x {} \;
else
    echo "[!] Error: Source directory $TERRAFORM_SRC does not exist"
    exit 1
fi

echo "[*] Running Terraform command: $@"
# Run the container
docker run --rm -ti \
  -v "$TERRAFORM_DEST":/workspace \
  -v "$PROJECT_ROOT/data/ssh":/root/.ssh \
  -w /workspace \
  terraform-openvpn \
  "$@"