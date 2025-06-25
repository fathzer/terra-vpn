FROM hashicorp/terraform:1.12

# Install required packages
RUN apk add --no-cache \
    openjdk21-jre \
#    openssh \
    && rm -rf /var/cache/apk/*

# TODO Everything under this line is outdated

# Add initialization script
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Create workspace directory and copy Terraform files
WORKDIR /workspace

ENTRYPOINT ["/entrypoint.sh"]
