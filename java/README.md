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

### java -jar odvpn.jar create_user *name* *user_name*
Crée un utilisateur sur le serveur VPN.

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
- [ ] Portage du fournisseur de VPS Scaleway en Full Java
- [x] Tester les implémentations des fournisseurs de DDNS.
  - [x] Switcher d'une implémentation sh à Java.
  - [x] Tests pour OVH provider
- [x] Rendre la configuration plus modulaire, les variables doivent être rattachées à leur provider, pas en tas.
- [x] Rappatrier la config VPN en local, pour la redéployer lors du start afin d'éviter la re-génération des clients (au moins les clients existants).
  - [x] Faire une classe permettant d'éxécuter des scripts à distance et de télécharger/téléverser des fichiers.
- [ ] Implémenter la commande `init`
  - [x] implémenter la sauvegarde du fichier de configuration.
  - [x] implémenter la vérification de l'état avant écrasement.
  - [x] implémenter la génération des clefs ssh si nécessaire
    - [x] implémenter un générateur de clef ssh.
  - [x] implémenter la mise à jour du DNS dynamique en Java plutôt que via un script shell.
  - [ ] doc sur le fichier .json.
  - [x] prévoir quelque chose de plus safe pour les sauvegardes de la configuration (encryptage avec une clef propre à l'utilisateur passée en paramètre - variable d'environnement ou système).
     - [ ] Documenter ça
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
  - [ ] Implémenter les web services
    - [x] Create/Update VPN config
    - [x] List VPN config
    - [x] Delete VPN config
    - [ ] Start
    - [ ] Stop
    - [ ] Add/remove user
- [ ] Documenter limitation sur les noms de VPN ()
