Openstack:
Username: INF8480-33
Password: X5Nym9Hhh



Code dans le dossier src.


Pour simple_server :
Faire : source ./INF8480-33-projet-openrc.sh     et entrer le password
Lancer le gabarit heat correspondant via Openstack s'il ne roule pas déjà
puis faire : nova add-floating-ip server 132.207.12.93
et voila ça marche (ouvrez votre browser sur 132.207.12.93:8000 pour un test rapide)


Pour Complex :
Lancer le gabarit heat correspondant via Openstack s'il ne roule pas déjà, c'est tout!


Rouler benchmark.sh après chacune des manipulations plus tôt
Ne pas oublier de faire avant: chmod u+x ./benchmark.sh
NOTE: wget stocke le contenu de la page, d'ou l'option -q0

Nos résultats (30 requêtes):
simple_server: 32.034s
Complex : 19.936s

