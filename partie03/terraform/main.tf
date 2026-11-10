# ==============================================================================
# 1. SOURCES DE DONNÉES (Récupération dynamique des ressources existantes)
# ==============================================================================
data "openstack_networking_network_v2" "private_net" {
  name = var.private_network_name
}

# ==============================================================================
# 2. SÉCURITÉ : GROUPE DE SÉCURITÉ DÉDIÉ (SSH, HTTP, ICMP)
# ==============================================================================
resource "openstack_networking_secgroup_v2" "secgroup_iac" {
  name        = "secgroup-iac"
  description = "Groupe de securite IaC (Terraform) - Autorise SSH, HTTP et ICMP"
}

# Règle SSH (TCP port 22) - Indispensable pour l'accès et Ansible
resource "openstack_networking_secgroup_rule_v2" "rule_ssh" {
  direction         = "ingress"
  ethertype         = "IPv4"
  protocol          = "tcp"
  port_range_min    = 22
  port_range_max    = 22
  remote_ip_prefix  = "0.0.0.0/0"
  security_group_id = openstack_networking_secgroup_v2.secgroup_iac.id
}

# Règle HTTP (TCP port 80) - Indispensable pour le serveur web Nginx
resource "openstack_networking_secgroup_rule_v2" "rule_http" {
  direction         = "ingress"
  ethertype         = "IPv4"
  protocol          = "tcp"
  port_range_min    = 80
  port_range_max    = 80
  remote_ip_prefix  = "0.0.0.0/0"
  security_group_id = openstack_networking_secgroup_v2.secgroup_iac.id
}

# Règle ICMP (Ping) - Pour le diagnostic de connectivité réseau
resource "openstack_networking_secgroup_rule_v2" "rule_icmp" {
  direction         = "ingress"
  ethertype         = "IPv4"
  protocol          = "icmp"
  remote_ip_prefix  = "0.0.0.0/0"
  security_group_id = openstack_networking_secgroup_v2.secgroup_iac.id
}

# ==============================================================================
# 3. PORT RÉSEAU & CALCUL (COMPUTE) : INSTANCE UBUNTU SERVER
# ==============================================================================
resource "openstack_networking_port_v2" "port_iac" {
  name               = "port-${var.instance_name}"
  network_id         = data.openstack_networking_network_v2.private_net.id
  security_group_ids = [openstack_networking_secgroup_v2.secgroup_iac.id]
  admin_state_up     = true
}

resource "openstack_compute_instance_v2" "ubuntu_iac" {
  name        = var.instance_name
  image_name  = var.image_name
  flavor_name = var.flavor_name
  key_pair    = var.keypair_name

  network {
    port = openstack_networking_port_v2.port_iac.id
  }

  # Initialisation Cloud-Init :
  # Corrige proactivement la résolution DNS (8.8.8.8) et le fichier /etc/hosts
  # dès le premier démarrage pour garantir qu'Ansible (apt install nginx)
  # s'exécute rapidement et sans erreur de résolution dans DevStack.
  user_data = <<-EOF
              #cloud-config
              manage_etc_hosts: true
              runcmd:
                - sed -i 's/^#*DNS=.*/DNS=8.8.8.8 1.1.1.1/' /etc/systemd/resolved.conf
                - systemctl restart systemd-resolved
                - echo "nameserver 8.8.8.8" > /etc/resolv.conf
              EOF
}

# ==============================================================================
# 4. RÉSEAU : ALLOCATION ET ASSOCIATION DE L'IP FLOTTANTE (API Neutron v2)
# ==============================================================================
resource "openstack_networking_floatingip_v2" "fip_iac" {
  pool = var.public_network_name
}

resource "openstack_networking_floatingip_associate_v2" "fip_assoc" {
  floating_ip = openstack_networking_floatingip_v2.fip_iac.address
  port_id     = openstack_networking_port_v2.port_iac.id
}

# ==============================================================================
# 5. INTÉGRATION CONTINUE IaC : GÉNÉRATION AUTOMATIQUE DE L'INVENTAIRE ANSIBLE
# ==============================================================================
resource "local_file" "ansible_inventory" {
  content = templatefile("${path.module}/inventory.tftpl", {
    floating_ip   = openstack_networking_floatingip_v2.fip_iac.address
    instance_name = openstack_compute_instance_v2.ubuntu_iac.name
    ssh_key_path  = var.ssh_private_key_path
  })
  filename        = "${path.module}/../ansible/inventory.ini"
  file_permission = "0644"

  depends_on = [openstack_networking_floatingip_associate_v2.fip_assoc]
}
