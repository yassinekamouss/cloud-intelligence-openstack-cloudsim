#!/usr/bin/env bash
# ==============================================================================
# Partie 2 - Question 2 : Déploiement IaaS avec CirrOS (Réseau, Sécurité, VM, FIP)
# Étudiant : Yassine Kamouss (R140087494)
# ==============================================================================
set -euo pipefail

source ~/devstack/openrc admin admin

# 1. Création du réseau privé, du sous-réseau et du routeur externe
date
openstack network create private-net
openstack subnet create --network private-net --subnet-range 10.0.0.0/24 private-subnet
openstack router create router1

ROUTER_ID=$(openstack router show router1 -f value -c id)
SUBNET_ID=$(openstack subnet show private-subnet -f value -c id)

openstack router add subnet "$ROUTER_ID" "$SUBNET_ID"
openstack router set --external-gateway public "$ROUTER_ID"

# 2. Création du groupe de sécurité (SSH + ICMP) et de la paire de clés SSH
date
openstack security group create secgroup-cirros --description "Autoriser SSH et Ping"
openstack security group rule create --protocol icmp secgroup-cirros
openstack security group rule create --protocol tcp --dst-port 22 secgroup-cirros

openstack keypair create key-cirros > ~/key-cirros.pem
chmod 600 ~/key-cirros.pem
openstack keypair list

# 3. Création du gabarit m1.nano et lancement de l'instance CirrOS
openstack flavor create --ram 128 --disk 1 --vcpus 1 m1.nano

date
openstack server create \
  --flavor m1.nano \
  --image cirros-0.6.3-x86_64-disk \
  --network private-net \
  --security-group secgroup-cirros \
  --key-name key-cirros \
  --wait \
  instance-cirros

# 4. Allocation et association d'une IP flottante
FIP_CIRROS=$(openstack floating ip create public -f value -c floating_ip_address)
openstack server add floating ip instance-cirros "$FIP_CIRROS"
openstack server show instance-cirros

# 5. Tests de connectivité (Ping ICMP et SSH)
date
ping -c 4 "$FIP_CIRROS"
ssh -i ~/key-cirros.pem cirros@"$FIP_CIRROS" "uname -a && ip a"
