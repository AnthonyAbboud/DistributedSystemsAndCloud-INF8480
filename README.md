*** READ ME ***
Sujet: TP 1
Cours: INF8480
Auteurs: Anthony Abboud et Luc Courbariaux
Date: 13 Fevrier 2018
******************************************

Informations pertinentes:

OpenStack
IP externe 132.207.12.93
Username: INF8480-33
Password: X5Nym9Hhh

******************************************

Dans le repertoire root se trouvent deux repertoires:
Partie1 et Partie2

******************************************
Partie 1

Compilation

1. Ouvrir un terminal dans le repertoire Partie1

2. Compiler avec la commande "ant"

Registre RMI

3. Ouvrir un terminal dans le repertoire Partie1/bin

4. Demarrer le registre RMI avec la commande "rmiregistry &"

Machine locale

5. Via le repertoire root, demarrer le serveur local avec "./server"

Machine distante

6. Avant d'acceder a la machine distante, changer les permissions de la cle ssh "chmod 600 cloud.key"

7. Ouvrir un autre terminal pour acceder a la machine distante avec "ssh -i cloud.key ubuntu@132.207.12.93"

8. Entrer dans la directoire ResponseTime_Analyser et activer le serveur distant "./server 132.207.12.93"

Client

9. Sur un terminal de la machine local, demarrer le client avec "./client 132.207.12.93 [X]" avec [X] variant de 1 a 7

******************************************
Partie 2

Setup

1. Ouvrir le repertoire Partie 2

2. Pour demarrer le registre RMI et la machine locale, repeter les etapes de la Partie 1

Client

Le client peut maintenant effectuer une des commandes suivantes:

./client create <nom fichier> : Creation d'un fichier sur le serveur

./client list : Liste des fichiers sur le serveur

./client get <nom fichier> : Mise a jour d'un fichier specifique

./client lock <nom fichier> : Verrouillage d'un fichier specifique pour le serveur (permet au client de modifier)

./client syncLocalDirectory : Mise a jour de tout les fichiers du clients avec ceux du serveur

./client push <nom fichier> : Envoi d'une nouvelle version du fichier au serveur. Deverrouille en meme temps le fichier.