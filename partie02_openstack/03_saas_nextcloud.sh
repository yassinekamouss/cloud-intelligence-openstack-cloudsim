#!/usr/bin/env bash
# ==============================================================================
# Partie 2 - Question 3 : Déploiement SaaS Ubuntu Server 20.04 LTS & Nextcloud
# Étudiant : Yassine Kamouss (R140087494)
# ==============================================================================
set -euo pipefail

source ~/devstack/openrc admin admin

# 1. Optimisation des ressources et création du gabarit m1.saas (1.5 Go RAM)
date
openstack server stop instance-cirros
openstack server list
openstack flavor create --ram 1536 --disk 10 --vcpus 1 m1.saas

# 2. Groupe de sécurité Web (SSH 22, HTTP 80, ICMP)
date
openstack security group create secgroup-saas --description "Autoriser Web, SSH et Ping"
openstack security group rule create --protocol tcp --dst-port 22 secgroup-saas
openstack security group rule create --protocol tcp --dst-port 80 secgroup-saas
openstack security group rule create --protocol icmp secgroup-saas

# 3. Téléchargement et importation de l'image Cloud Ubuntu 20.04 LTS dans Glance
wget -c https://cloud-images.ubuntu.com/focal/current/focal-server-cloudimg-amd64.img
openstack image create "ubuntu-20.04" \
  --file focal-server-cloudimg-amd64.img \
  --disk-format qcow2 \
  --container-format bare \
  --public

# 4. Lancement de l'instance SaaS et association d'une IP flottante
NET_ID=$(openstack network show private-net -f value -c id)
date
openstack server create \
  --flavor m1.saas \
  --image ubuntu-20.04 \
  --network "$NET_ID" \
  --security-group secgroup-saas \
  --key-name key-cirros \
  --wait \
  instance-saas

FIP_SAAS=$(openstack floating ip create public -f value -c floating_ip_address)
openstack server add floating ip instance-saas "$FIP_SAAS"

# 5. Déploiement de Nextcloud via Snap sur l'instance Ubuntu
ssh -i ~/key-cirros.pem ubuntu@"$FIP_SAAS" << 'EOF'
echo "127.0.0.1 instance-saas" | sudo tee -a /etc/hosts
echo "nameserver 8.8.8.8" | sudo tee /etc/resolv.conf
sudo snap install nextcloud
snap services nextcloud
EOF
