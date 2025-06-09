FROM hashicorp/terraform:light

# Install required packages
RUN apk add --no-cache \
    wget \
    openssh \
    && rm -rf /var/cache/apk/*

# Add initialization script
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Create workspace directory and copy Terraform files
WORKDIR /workspace

ENTRYPOINT ["/entrypoint.sh"]
