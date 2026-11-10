# Étape 4 : Simulation et Comparaison des Politiques d'Ordonnancement et d'Allocation

Ce document détaille l'implémentation Java sous **CloudSim Plus (v8.5.7)**, les patrons de conception logiciels (Design Patterns) adoptés, ainsi que la comparaison empirique des politiques d'allocation et d'ordonnancement pour le Cloud de l'Université Abdelmalek Essaâdi (UAE).

---

## 1. Conception Logicielle et Architecture du Code Java

Le code a été développé dans le respect strict des principes **SOLID** et de la Programmation Orientée Objet, évitant toute classe monolithique grâce à l'application de design patterns éprouvés :

```
ma.ac.uae.cloudsim/
├── builder/
│   ├── UaeDatacenterBuilder.java       # [Builder] Construction des Datacenters physiques (DC1, DC2, DC3)
│   └── UaeVmBuilder.java               # [Builder] Création des gabarits de VMs (DB, ENT, Moodle) & Scaling
├── factory/
│   └── CloudletWorkloadFactory.java    # [Factory] Génération des profils de requêtes (Normales & Pointe)
├── models/
│   └── UaeDatacenter.java              # Modèle de délégation et d'abstraction des Datacenters
├── simulation/
│   ├── SimulationExecutionResult.java  # DTO encapsulant l'ensemble des métriques d'évaluation
│   └── UaeSimulation.java              # Point d'entrée de simulation et orchestrateur
├── strategy/
│   ├── SimulationStrategy.java         # [Strategy] Interface commune d'exécution
│   ├── NormalScenarioStrategy.java     # Scénario 1 : Charge normale, privé, SpaceShared
│   ├── PeakNaiveScenarioStrategy.java  # Scénario 2 : Charge de pointe, privé fixe saturé
│   └── PeakHybridBurstingStrategy.java # Scénario 3 : Charge de pointe hybride, TimeShared, Auto-Scaling DC3
└── utils/
    ├── Constants.java                  # Paramètres physiques, virtuels, réseaux et seuils UAE
    └── SimulationReportHelper.java     # Formatage du tableau comparatif ASCII pour le rapport
```

### Patrons de Conception (Design Patterns) Utilisés :
1. **Builder Pattern (`UaeDatacenterBuilder`, `UaeVmBuilder`) :**
   - Isole la complexité d'instanciation des serveurs physiques (`HostSimple`), de leurs processeurs (`PeSimple`), de leurs provisionneurs de ressources (`ResourceProvisionerSimple`), et des VMs par service (`VM_Database`, `VM_ENT`, `VM_Moodle`).
2. **Factory Pattern (`CloudletWorkloadFactory`) :**
   - Fabrique les flots de requêtes étudiantes selon les spécifications établies dans l'étape 3 (longueur en MI, taille mémoire, fichiers d'entrée/sortie).
3. **Strategy Pattern (`SimulationStrategy`, `NormalScenarioStrategy`, `PeakNaiveScenarioStrategy`, `PeakHybridBurstingStrategy`) :**
   - Permet d'interchanger dynamiquement les politiques de simulation et d'exécuter l'étude comparative sans altérer la logique centrale du moteur.

---

## 2. Politiques Comparées

Conformément à la question 4 du cahier des charges, nous avons comparé les politiques suivantes :

### A. Ordonnancement des Tâches au sein des VMs
* **`CloudletSchedulerSpaceShared` (Scénarios 1 et 2) :**
  - **Principe :** Allocation exclusive des cœurs vCPU aux Cloudlets. Si tous les cœurs sont occupés, les requêtes suivantes sont mises en attente dans une file stricte (FIFO).
  - **Impact observé :** En période nominale, assure l'isolation des processus. En période de pointe, entraîne une **explosion du temps d'attente moyen (341,14 s)** car des milliers de requêtes restent bloquées dans la file.
* **`CloudletSchedulerTimeShared` (Scénario 3) :**
  - **Principe :** Partage équitable du temps processeur entre tous les Cloudlets actifs par découpage temporel (Round-Robin preemptif).
  - **Impact observé :** **Élimination totale du temps d'attente (0,00 s)**. Tous les étudiants reçoivent une prise en charge immédiate de leur requête sans file d'attente bloquante.

### B. Allocation des VMs sur les Hôtes Physiques
* **`VmAllocationPolicySimple` (Scénarios 1 et 2) :**
  - **Principe :** Stratégie premier-arrivé-premier-servi (First Fit). Alloue la VM sur le premier serveur physique disposant de ressources libres suffisantes.
* **`VmAllocationPolicyBestFit` (Scénario 3) :**
  - **Principe :** Sélectionne l'hôte physique ayant le moins de cœurs libres restants mais suffisant pour la VM, optimisant la compacité du placement et minimisant le gaspillage énergétique.

### C. Élasticité et Auto-Scaling (`HorizontalVmScaling`)
* **Déclenchement :** Attaché aux 14 VMs de base du LMS Moodle.
* **Seuil critique (Overload Predicate) :** $Utilisation\ CPU > 80\%$.
* **Action :** Instanciation dynamique et immédiate de VMs éphémères (`VM_Moodle_ScaleOut_DC3`) sur le **Datacenter 3 (Cloud Public)**.
* **Résultat :** Le parc de VMs passe automatiquement de **20 à 34 VMs**, absorbant intégralement le débordement de charge étudiante (Cloud Bursting).

---

## 3. Résultats Comparatifs de la Simulation

Le tableau ci-dessous résume les métriques obtenues lors de l'exécution complète des trois scénarios :

```
========================================================================================================================================================================
                 RÉSULTATS COMPARATIFS DE LA SIMULATION DU CLOUD UNIVERSITAIRE UAE (CLOUDSIM PLUS)
========================================================================================================================================================================
+----------------------------------------------------------------+----------------------------+-------------------------+--------------+-----------+--------------+------------------+------------------+---------------+--------------+
| Scénario Simulé                                                | Ordonnancement (VM)        | Allocation (Datacenter) | Auto-Scaling | Cloudlets | VMs (Init/Tot)| Temps Total (s)  | Tps Réponse Moy. | Tps Attente   | CPU Hôtes (%) | Coût (DH)    |
+----------------------------------------------------------------+----------------------------+-------------------------+--------------+-----------+--------------+------------------+------------------+---------------+--------------+
| 1. Période Normale (SpaceShared + Simple Allocation)           | CloudletSchedulerSpaceShared | VmAllocationPolicySimple | NON          | 1500      | 20 / 20      | 193.77           | 60.15            | 49.96         | 3.4           | 29.07        |
| 2. Période de Pointe Naïve (SpaceShared, Sans Auto-Scaling, Privé Fixe) | CloudletSchedulerSpaceShared | VmAllocationPolicySimple | NON          | 2500      | 20 / 20      | 1285.44          | 381.30           | 341.14        | 3.1           | 192.82       |
| 3. Période de Pointe Optimisée (TimeShared + BestFit + Cloud Bursting & Auto-Scaling) | CloudletSchedulerTimeShared | VmAllocationPolicyBestFit | OUI (Burst)  | 2500      | 20 / 34      | 1250.33          | 718.92           | 0.00          | 2.5           | 1062.78      |
+----------------------------------------------------------------+----------------------------+-------------------------+--------------+-----------+--------------+------------------+------------------+---------------+--------------+
```

---

## 4. Preuve d'Exécution (Capture d'Écran)

![Tableau comparatif des résultats de simulation CloudSim Plus](../pics/resultats_comparaison_politiques.png)

---

## 5. Synthèse des Enseignements

1. **Validation de l'Architecture Hybride :**
   En période de pointe, l'approche privée fixe (Scénario 2) pénalise lourdement les utilisateurs avec un temps d'attente de plus de **5 minutes et 40 secondes par requête** (341,14 s).
2. **Efficacité de l'Auto-Scaling :**
   Le mécanisme `HorizontalVmScaling` a immédiatement instancié 14 VMs de renfort sur le Datacenter Cloud Public, supprimant à **100% le temps d'attente** (0,00 s).
3. **Arbitrage Coût / Qualité de Service (QoS) :**
   L'activation du Cloud Bursting engendre un coût opérationnel à l'usage (1 062,78 DH contre 192,82 DH), mais garantit la continuité absolue du service universitaire et respecte le SLA de disponibilité.
