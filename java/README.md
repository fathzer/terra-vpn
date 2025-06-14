# Les commandes à implémenter

## init [-f] *name* *config.json* 
Initialise la configuration *name*.
Techniquement, crée un dossier *name* et y place les fichiers terraform, ainsi qu'une copie encryptée du fichier *config.json* sans la clef ssh qu'on conserve dans un fichier.
Si le dossier existe déjà, on affiche l'état de l'infra (initialisée, démarrée ou arrêtée), puis on quitte sauf si l'option `-f` est présente. Dans ce cas, on ne supprime pas le dossier (pour conserver l'état de Terraform) mais on recrée les fichiers Terraform et la copie de la configuration.
Si le fichier *config.json* ne contient pas de clef ssh:
  -S'il y a une clef ssh dans le dossier (on a utilisé l'option `-f`), on ne fait rien.
  -Sinon, on crée une paire de clef et on affiche la clef publique.
En fin de traitement, on fait un `terraform init` si besoin et un `terraform plan`.

## start *name*
Démarre l'infrastructure

Crée l'infrastructure via `terraform apply`.

## create_user *name* *user_name*
Crée un utilisateur sur le serveur.

## stop *name*
Arrête l'infrastructure

Arrête l'infrastructure via `terraform destroy`.

## delete *name*
Supprime la configuration

Fait un `terraform destroy` et supprime le dossier