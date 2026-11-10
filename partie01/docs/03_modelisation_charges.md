# Phase 3 : Modélisation des Charges (Cloudlets)

Dans CloudSim Plus, une requête utilisateur est modélisée par un `Cloudlet`. Sa complexité est mesurée en **MI (Millions d'Instructions)**. Pour simuler le trafic de l'Université Abdelmalek Essaâdi (UAE), nous définissons deux profils de charge distincts.

## 1. Période Normale (Trafic Quotidien)
Cette période correspond à une journée type (consultation de l'ENT, quelques cours sur Moodle, emails).
- **Nombre d'utilisateurs simultanés :** ~3 000
- **Nombre de Cloudlets simulés :** 1 500 (échantillon représentatif de 10 000 requêtes réelles)
- **Complexité moyenne d'un Cloudlet :** 10 000 MI (requêtes légères)
- **Taille du fichier d'entrée/sortie :** 300 Ko
- **Politique d'ordonnancement (VM) :** `CloudletSchedulerSpaceShared` (les requêtes sont traitées séquentiellement, garantissant l'isolation des processus).
- **Politique d'allocation (Datacenter) :** `VmAllocationPolicySimple` (placement basique).

## 2. Période de Pointe (Inscriptions Apogée & Examens Moodle)
Cette période simule le rush de fin de semestre. Les serveurs sont fortement sollicités.
- **Nombre d'utilisateurs simultanés :** ~15 000+
- **Nombre de Cloudlets simulés :** 2 500 (échantillon représentatif de 50 000 requêtes réelles lourdes)
- **Complexité moyenne d'un Cloudlet :** 40 000 MI (requêtes lourdes, requêtes de bases de données, téléchargements de PDF d'examens)
- **Taille du fichier d'entrée/sortie :** 2 Mo
- **Politique d'ordonnancement (VM) :** `CloudletSchedulerTimeShared` (partage du temps CPU entre les requêtes pour éviter qu'une grosse requête ne bloque les autres).
- **Politique d'allocation (Datacenter) :** `VmAllocationPolicyBestFit` (optimisation agressive pour tasser un maximum de VMs sur le minimum de Hosts physiques, limitant le gaspillage).
- **Auto-Scaling :** Déclenchement de `HorizontalVmScaling` avec un seuil de surcharge fixé à 80% d'utilisation CPU vers le Cloud Public (DC3).
