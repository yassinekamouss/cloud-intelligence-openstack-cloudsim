#!/usr/bin/env bash
# ==============================================================================
# Partie 2 - Question 1 : Vérification de l'installation DevStack (CLI)
# Étudiant : Yassine Kamouss (R140087494)
# ==============================================================================
set -euo pipefail

# Chargement des identifiants administrateur Keystone
source ~/devstack/openrc admin admin

echo "=== 1. Liste globale des services OpenStack (Keystone, Nova, Glance, Neutron, Placement, Cinder) ==="
date
openstack service list

echo "=== 2. Liste des services de calcul (Nova Compute) ==="
date
openstack compute service list

echo "=== 3. Liste des agents réseau (Neutron Agents) ==="
date
openstack network agent list
