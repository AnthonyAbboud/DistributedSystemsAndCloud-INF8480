Client.java est le repartiteur
ServeurInterface est l'interface reseau
Server est le serveur de calcul

start.sh build les classes, lance rmiregistry, et eventuellement aurait pu faire les expériences pour nous

pour lancer lea differents programmes, utiliser les fichiers client, LDAP et server ;
Ex ./client <params> (ce sont des fichiers bash, ouvrez-les dans un  éditeur de texte pour voir les commandes utilisées)

Pour le repartiteur, voici les arguments :
[Nom_Fichier_Texte] [s pour securise, n pour non-securise] [adresseIP1 a adresseIP4] 

Pour le server, voici les arguments:
[serverName] [int port] [int workCapacity] [float maliciousness]

Pas d'arguments pour LDAP (le code correspondant dans server et client n'est pas la)
