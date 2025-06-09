FROM hashicorp/terraform:light

RUN apk add --no-cache curl openssh

# Ajout du script d'initialisation
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

WORKDIR /workspace
ENTRYPOINT ["/entrypoint.sh"]
