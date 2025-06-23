# Les commandes

## Commandes

### init [-f] *name* *config.json* 
Initialise la configuration *name*.
Techniquement, crée un dossier *name* et y place les fichiers terraform, ainsi qu'une copie encryptée du fichier *config.json* sans la clef ssh qu'on conserve dans un fichier.
Si le dossier existe déjà, on affiche l'état de l'infra (initialisée, démarrée ou arrêtée), puis on quitte sauf si l'option `-f` est présente. Dans ce cas, on ne supprime pas le dossier (pour conserver l'état de Terraform) mais on recrée les fichiers Terraform et la copie de la configuration.
Si le fichier *config.json* ne contient pas de clef ssh:
  -S'il y a une clef ssh dans le dossier (on a utilisé l'option `-f`), on ne fait rien.
  -Sinon, on crée une paire de clef et on affiche la clef publique.
En fin de traitement, on fait un `terraform init` si besoin et un `terraform plan`.

### start *name*
Démarre l'infrastructure

Crée l'infrastructure via `terraform apply`.

### create_user *name* *user_name*
Crée un utilisateur sur le serveur.

### stop *name*
Arrête l'infrastructure

Arrête l'infrastructure via `terraform destroy`.

### delete *name*
Supprime la configuration

Fait un `terraform destroy` et supprime le dossier

## TIPS
Par défaut, les données sont stockées dans un répertoire `data` dans le working directory. Vous pouvez changer ce comportement en définissant la variable système `data.dir`.

## TODO
- [x] Implémentation le fournisseur de VPS Digital Ocean.
  - [x] Ouvrir les ports nécessaires.
- [x] Implémentation le fournisseur de VPS Permanent.
  - [x] Implémentation.
  - [x] Doc.
- [x] Tester les implémentations des fournisseurs de DDNS.
  - [x] Switcher d'une implémentation sh à Java.
  - [x] Tests pour OVH provider
- [x] Rendre la configuration plus modulaire, les variables doivent être rattachées à leur provider, pas en tas.
- [x] Rappatrier la config VPN en local, pour la redéployer lors du start afin d'éviter la re-génération des clients (au moins les clients existants).
  - [x] Faire une classe permettant d'éxécuter des scripts à distance et de télécharger/téléverser des fichiers.
- [ ] Implémenter la commande `init`
  - [x] Retirer les variables inutiles de `terraform.tfvars`
  - [x] implémenter la sauvegarde du fichier de configuration.
  - [ ] implémenter la vérification de l'état avant écrasement.
  - [ ] implémenter l'appel de `terraform init` si nécessaire.
  - [ ] implémenter la génération des clefs ssh si nécessaire
    - [x] implémenter un générateur de clef ssh.
  - [x] implémenter la mise à jour du DNS dynamique en Java plutôt que via un script shell.
  - [ ] doc sur le fichier .json.
  - [ ] prévoir quelque chose de plus safe pour les sauvegardes de la configuration (encryptage avec une clef propre à l'utilisateur passée en paramètre - variable d'environnement ou système).
- [x] Implémenter la commande `start`
- [ ] Implémenter la commande `stop`
- [ ] Implémenter la commande `create_user`
  - [x] Faire la création de l'utilisateur
  - [ ] Refaire le backup de la configuration
- [ ] Implémenter la commande `delete`
- [ ] Implémenter la commande `delete_user`
- [ ] Implémenter la commande `show users` qui renvoie la liste des utilisateurs.
- [ ] Implémenter la commande `show configuration` qui renvoie le fichier de configuration.
- [ ] Implémenter une IHM web au moins pour start et stop.
- [ ] Implémenter le fournisseur de VPS [Vultr](https://www.vultr.com/).
- [ ] Implémenter le fournisseur de VPS [Kamatera](https://try.kamatera.com/).
