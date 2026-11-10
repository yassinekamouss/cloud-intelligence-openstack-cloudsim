variable "cloud_name" {
  description = "Nom du profil OpenStack défini dans /etc/openstack/clouds.yaml"
  type        = string
  default     = "devstack-admin"
}

variable "instance_name" {
  description = "Nom de l'instance Ubuntu provisionnée par Terraform"
  type        = string
  default     = "instance-terraform"
}

variable "image_name" {
  description = "Nom de l'image Cloud Ubuntu importée dans Glance (Partie 2)"
  type        = string
  default     = "ubuntu-20.04"
}

variable "flavor_name" {
  description = "Gabarit (Flavor) adapté pour Ubuntu Server"
  type        = string
  default     = "m1.small"
}

variable "private_network_name" {
  description = "Nom du réseau privé créé lors de la Partie 2"
  type        = string
  default     = "private-net"
}

variable "public_network_name" {
  description = "Pool de réseau externe pour l'allocation d'IP flottantes"
  type        = string
  default     = "public"
}

variable "keypair_name" {
  description = "Nom de la paire de clés SSH existante dans OpenStack"
  type        = string
  default     = "key-cirros"
}

variable "ssh_private_key_path" {
  description = "Chemin vers la clé privée SSH sur la VM DevStack pour Ansible"
  type        = string
  default     = "~/key-cirros.pem"
}
