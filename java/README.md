# Les commandes

## Commandes

### java -jar odvpn.jar web
Démarre l'application web. Celle-ci est une alternative aux commandes listées ci-dessous. Elle permet la gestion des VPN  grâce à des web services.

### java -jar odvpn.jar init [-f] *name* *config.json* []
Initialise la configuration *name*.
Techniquement, on commence par vérifier la cohérence de la configuration, puis on crée un dossier *name* et y place une copie du fichier *config.json*.
Si le dossier existe déjà on quitte en erreur, sauf si l'option `-f` est présente et que le serveur VPN n'est pas démarré. Dans ce cas, on remplace le fichier de configuration présent.


### java -jar odvpn.jar start *name*
Créer l'infrastructure si besoin, puis démarre le serveur VPN.

### java -jar odvpn.jar status *name*
Affiche le statut du serveur VPN

### java -jar odvpn.jar create_user *name* *user_name*
Crée un utilisateur sur le serveur VPN.

### java -jar odvpn.jar delete_user *name* *user_name*
Supprime un utilisateur sur le serveur VPN.

### java -jar odvpn.jar user_conf *name* *user_name*
Affiche le contenu du ficher de configuration du client OpenVPN.

### java -jar odvpn.jar stop *name*
Arrête le serveur VPN et supprime l'infrastructure

### java -jar odvpn.jar delete *name*
Supprime la configuration

## TIPS
Par défaut, les données sont stockées dans un répertoire `data` dans le working directory. Vous pouvez changer ce comportement en définissant la variable système `data.dir`.

## TODO
- [x] Implémentation du fournisseur de VPS Digital Ocean.
  - [x] Ouvrir les ports nécessaires.
  - [ ] Portage en full Java.
- [x] Implémentation d'un fournisseur de VPS "Permanent".
  - [x] Implémentation.
  - [x] Doc.
- [x] Implémentation du fournisseur de VPS Vultr.
- [x] Implémentation du fournisseur de VPS Hetzner.
- [ ] Portage du fournisseur de VPS Scaleway en Full Java
- [x] Tester les implémentations des fournisseurs de DDNS.
  - [x] Switcher d'une implémentation sh à Java.
  - [x] Tests pour OVH provider
- [x] Rendre la configuration plus modulaire, les variables doivent être rattachées à leur provider, pas en tas.
- [x] Rappatrier la config VPN en local, pour la redéployer lors du start afin d'éviter la re-génération des clients (au moins les clients existants).
  - [x] Faire une classe permettant d'éxécuter des scripts à distance et de télécharger/téléverser des fichiers.
- [x] implémenter la mise à jour du DNS dynamique en Java plutôt que via un script shell.
- [x] prévoir quelque chose de plus safe pour les sauvegardes de la configuration (utilisation de variable d'environnement dans la config).
- [ ] Implémenter les commandes CLI
  - [ ] Implémenter la commande `init`
    - [x] implémenter la sauvegarde du fichier de configuration.
    - [x] implémenter la vérification de l'état avant écrasement.
    - [ ] doc sur le fichier .json.
  - [x] Implémenter la commande `start`
  - [ ] Implémenter la commande `stop`
  - [ ] Implémenter la commande `create_user`
    - [x] Faire la création de l'utilisateur
    - [x] Refaire le backup de la configuration
  - [x] Implémenter la commande `delete`
  - [ ] Implémenter la commande `delete_user`
  - [ ] Implémenter la commande `show-users` qui renvoie la liste des utilisateurs.
  - [ ] Implémenter la commande `show-configuration` qui renvoie le fichier de configuration.
  - [ ] Implémenter la commande `show-backup` qui renvoie le zip de la configuration de backup.
- [ ] Implémenter une IHM web au moins pour start et stop.
- [ ] Implémenter les web services
  - [x] implémenter la génération des clefs ssh au démarrage si nécessaire
    - [x] implémenter un générateur de clef ssh.
  - [x] Create/Update VPN config
  - [x] List VPN config
  - [x] Delete VPN config
  - [x] Start
  - [x] Stop
  - [x] Add/remove user
  - [x] List users
  - [x] Recup fichier de conf d'un utilisateur
    - [x] Erreur 404 (et pas 500) si utilisateur inconnu.
  - [ ] Recup de la configuration globale du serveur
- [x] Faire une couche d'abstraction de AbstractOnDemandVPNManager pour stocker la configuration ailleurs que sur le disque ... maybe in the future.
- [ ] Implémenter des exclusions mutuelles entre les opérations (toute opération d'écriture en cours interdit d'autres opérations) avec un ReadWriteLock. Attention, le start s'éxécute dans un thread à part dans les WS.
- [ ] Documenter la limitation sur les noms de VPN (must start with a letter or a number and contain only letters, numbers, dots, underscores and hyphens).
- [ ] Donner le choix entre OpenVPN et Wireguard.
  Quelques notes sur Wireguard:
  - Il y a une image Docker activement maintenue par le projet https://github.com/wg-easy/wg-easy. Pas pratique, pratique, mais jouable ;-)
  - Il faut commencer par créer un network Docker avec `docker network create -d bridge --ipv6 --subnet 10.42.42.0/24 --subnet fdcc:ad94:bacf:61a3::/64 wg`

