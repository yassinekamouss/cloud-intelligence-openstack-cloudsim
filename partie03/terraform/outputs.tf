output "instance_name" {
  description = "Nom de l'instance Ubuntu créée"
  value       = openstack_compute_instance_v2.ubuntu_iac.name
}

output "instance_id" {
  description = "UUID OpenStack de l'instance"
  value       = openstack_compute_instance_v2.ubuntu_iac.id
}

output "private_ip" {
  description = "Adresse IP privée de l'instance sur private-net"
  value       = openstack_compute_instance_v2.ubuntu_iac.access_ip_v4
}

output "floating_ip" {
  description = "Adresse IP flottante publique associée à l'instance"
  value       = openstack_networking_floatingip_v2.fip_iac.address
}

output "ansible_inventory_path" {
  description = "Chemin du fichier d'inventaire Ansible généré automatiquement"
  value       = local_file.ansible_inventory.filename
}
