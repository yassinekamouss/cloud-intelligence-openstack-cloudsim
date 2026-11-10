# Simulation d'un Cloud Universitaire Hybride (Université Abdelmalek Essaâdi - UAE)
## Projet de Cloud Computing & Cloud Intelligence — Partie 1 (CloudSim Plus)

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.8%2B-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)
![CloudSim Plus](https://img.shields.io/badge/CloudSim%20Plus-v8.5.7-007ACC?style=for-the-badge)
![License](https://img.shields.io/badge/License-Academic-blue?style=for-the-badge)

---

## 📌 Sommaire

1. [Contexte et Présentation de l'Université (UAE)](#1-contexte-et-présentation-de-luniversité-uae)
2. [Architecture Cloud Proposée (Physique & Logique)](#2-architecture-cloud-proposée-physique--logique)
3. [Modélisation des Charges Applicatives (Cloudlets)](#3-modélisation-des-charges-applicatives-cloudlets)
4. [Conception Logicielle & Design Patterns (SOLID)](#4-conception-logicielle--design-patterns-solid)
5. [Politiques Comparées & Mécanisme d'Auto-Scaling](#5-politiques-comparées--mécanisme-dauto-scaling)
6. [Résultats Comparatifs de la Simulation](#6-résultats-comparatifs-de-la-simulation)
7. [Analyse des Métriques & Conclusion Décisionnelle](#7-analyse-des-métriques--conclusion-décisionnelle)
8. [Guide d'Installation et d'Exécution](#8-guide-dinstallation-et-dexécution)
9. [Arborescence du Dépôt](#9-arborescence-du-dépôt)

---

## 1. Contexte et Présentation de l'Université (UAE)

L'**Université Abdelmalek Essaâdi (UAE)** est l'institution publique d'enseignement supérieur et de recherche de référence dans la région Tanger-Tétouan-Al Hoceïma au Maroc.

### Chiffres Clés et Périmètre :
* **Effectif étudiant :** Plus de **120 000 étudiant(e)s**.
* **Corps enseignant & administratif :** ~2 500 professeurs et plus de 1 200 personnels administratifs et techniques.
* **Implantation multi-sites :** 16 établissements répartis sur 6 villes (Tétouan, Tanger, Al Hoceïma, Larache, Ksar El Kébir, Chefchaouen).
* **Services Numériques Critiques :**
  1. **Apogée (Gestion de la scolarité centrale) :** Données académiques ultrasensibles, inscriptions pédagogiques, délibérations et relevés de notes.
  2. **Moodle LMS :** Plateforme de formation à distance, dépôts de devoirs, cours interactifs et examens en ligne.
  3. **Portail ENT & Authentification CAS :** Guichet numérique unique des étudiants et enseignants, messagerie collaborative.

---

## 2. Architecture Cloud Proposée (Physique & Logique)

Pour concilier **souveraineté des données**, **haute disponibilité géographique** et **résilience budgétaire**, une **Architecture Cloud Hybride à 3 Datacenters** est retenue :

```
                                  [ Requêtes Utilisateurs (Étudiants / Enseignants) ]
                                                          |
                                                          v
                                       [ Datacenter Broker / Load Balancer ]
                                            /             |            \
                                           /              |             \
                                          v               v              v
               +---------------------------+  VPN Sync  +---------------------------+  Bursting   +---------------------------+
               |  DC1 : Tétouan (Privé)    |<---------->|   DC2 : Tanger (Privé)    |----------->| DC3 : Cloud Public Hypers.|
               |  - Siège Présidence UAE   |            |   - Pôle ENSA / FST       | (Élasticité| (AWS / Azure Pay-as-you-go|
               |  - 10 Serveurs (640 PEs)  |            |   - 10 Serveurs (640 PEs) | temporaire)| - Pool élastique de Hosts |
               |  - VM DB Master (IaaS)    |            |   - VM DB Replica (IaaS)  |            | - 14 VMs Moodle Burst     |
               |  - 4 VMs ENT Portal (SaaS)|            |   - 14 VMs Moodle (PaaS)  |            |   (HorizontalVmScaling)   |
               +---------------------------+            +---------------------------+            +---------------------------+
```

### A. Infrastructure Physique (Underlay)
* **Datacenter 1 (Tétouan - Privé) :** 10 serveurs physiques biprocesseurs (64 cœurs / 256 Go RAM / 10 Gbps / SAN 10 To).
* **Datacenter 2 (Tanger - Privé) :** 10 serveurs physiques équivalents pour le disaster recovery et la plateforme pédagogique.
* **Datacenter 3 (Cloud Public - Débordement) :** Pool élastique d'instances hyperscalers (AWS / Azure) mobilisées exclusivement lors des pics d'inscriptions et d'examens.

*Schémas d'architecture natifs disponibles sous :*
* `diagrams/01_architecture_physique.drawio`
* `diagrams/02_architecture_logique_bursting.drawio`

### B. Modèles de Services (Overlay)
* **IaaS (Infrastructure as a Service) :** Machines Virtuelles de Base de Données (`VM_DB`, 16 vCPU, 32 Go RAM).
* **PaaS (Platform as a Service) :** Plateforme Moodle déployée sous conteneurs / machines virtuelles applicatives (`VM_Moodle`, 8 vCPU, 16 Go RAM).
* **SaaS (Software as a Service) :** Portail ENT étudiant et CAS (`VM_ENT`, 4 vCPU, 8 Go RAM).

---

## 3. Modélisation des Charges Applicatives (Cloudlets)

Deux profils de charges étudiantes ont été calibrés pour refléter la réalité du campus :

1. **Période Normale (Trafic Quotidien) :**
   * ~3 000 utilisateurs simultanés.
   * **1 500 Cloudlets simulés** (échantillon représentatif de 10 000 requêtes réelles légères).
   * Complexité : **10 000 MI (Millions d'Instructions)** par requête.
   * Taille I/O : 300 Ko.
2. **Période de Pointe (Inscriptions massives & Examens Moodle) :**
   * ~15 000+ étudiants simultanés.
   * **2 500 Cloudlets simulés** (échantillon représentatif de 50 000 requêtes réelles lourdes).
   * Complexité : **40 000 MI** par requête (transactions SQL lourdes, téléchargements de sujets PDF).
   * Taille I/O : 2 Mo.

---

## 4. Conception Logicielle & Design Patterns (SOLID)

Le projet applique rigoureusement les principes de Programmation Orientée Objet et les Design Patterns :

* **Builder Pattern (`UaeDatacenterBuilder`, `UaeVmBuilder`) :** Modularisation de la création des hôtes physiques, des processeurs, provisionneurs de RAM/BW et gabarits de VMs.
* **Factory Pattern (`CloudletWorkloadFactory`) :** Génération des flots de Cloudlets selon le scénario.
* **Strategy Pattern (`SimulationStrategy`, `NormalScenarioStrategy`, `PeakNaiveScenarioStrategy`, `PeakHybridBurstingStrategy`) :** Isolation des scénarios et des politiques pour une interchangeabilité et extensibilité sans régression.
* **DTO Pattern (`SimulationExecutionResult`) :** Encapsulation propre de toutes les métriques d'évaluation.

---

## 5. Politiques Comparées & Mécanisme d'Auto-Scaling

Trois scénarios représentatifs ont été modélisés et exécutés :

| Paramètre | Scénario 1 : Période Normale | Scénario 2 : Pointe Naïve (Privé Fixe) | Scénario 3 : Pointe Optimisée (Hybride) |
| :--- | :--- | :--- | :--- |
| **Période** | Nominale quotidienne | Pic d'examens / inscriptions | Pic d'examens / inscriptions |
| **Ordonnancement (VM)** | `CloudletSchedulerSpaceShared` | `CloudletSchedulerSpaceShared` | `CloudletSchedulerTimeShared` |
| **Allocation (Datacenter)** | `VmAllocationPolicySimple` | `VmAllocationPolicySimple` | `VmAllocationPolicyBestFit` |
| **Datacenters** | DC1 + DC2 (Privés) | DC1 + DC2 (Privés uniquement) | DC1 + DC2 + DC3 (Cloud Public) |
| **Auto-Scaling** | Désactivé | Désactivé | **HorizontalVmScaling (Seuil CPU > 80%)** |

---

## 6. Résultats Comparatifs de la Simulation

Exécution complète produite par le moteur CloudSim Plus :

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

## 7. Analyse des Métriques & Conclusion Décisionnelle

### A. Analyse des Performances
1. **Élimination Intégrale de la File d'Attente :**
   * En scénario naïf (Scénario 2), le temps d'attente moyen atteint **341,14 s (~5,7 minutes)** par étudiant, provoquant des abandons de session et des erreurs HTTP 504.
   * En scénario hybride optimisé (Scénario 3), l'utilisation combinée du partage de temps CPU (`TimeShared`) et de l'élasticité vers DC3 fait chuter le temps d'attente à **0,00 seconde**. Chaque requête étudiante commence son exécution dès son arrivée.
2. **Efficacité du Cloud Bursting :**
   * Dès détection de la surcharge ($CPU > 80\%$), le contrôleur `HorizontalVmScaling` déploie automatiquement **14 VMs Moodle supplémentaires sur le Cloud Public (DC3)**, portant la flotte active de 20 à **34 VMs**.
3. **Arbitrage Coût / Qualité de Service (SLA) :**
   * Le coût opérationnel passe de 192,82 DH à **1 062,78 DH**.
   * Ce surcoût est parfaitement rentable pour l'UAE car il est strictement limité aux quelques semaines d'inscriptions et d'examens (facturation à la seconde), dispensant l'université d'investir des millions de dirhams en matériel physique sous-exploité le reste de l'année.

### B. Conclusion Décisionnelle : L'architecture suffit-elle en période de pointe ?
**Oui, l'architecture Cloud Hybride avec Cloud Bursting est amplement suffisante, hautement résiliente et optimale pour l'UAE.** Elle garantit 100% de disponibilité logicielle tout en préservant la souveraineté des bases de données au sein des datacenters régionaux de Tétouan et Tanger.

---

## 8. Guide d'Installation et d'Exécution

### Prérequis
* Java Development Kit (JDK) 17 ou supérieur
* Apache Maven 3.8+
* Linux Ubuntu / Debian / macOS / WSL

### Compilation et Lancement

1. **Cloner le projet :**
   ```bash
   git clone https://github.com/yassinekamouss/cloud-intelligence-openstack-cloudsim.git
   cd cloud-intelligence-openstack-cloudsim/partie01
   ```

2. **Compiler avec Maven :**
   ```bash
   mvn clean compile
   ```

3. **Lancer la simulation comparative :**
   ```bash
   java -cp "target/classes:$(find ~/.m2/repository -name '*.jar' | tr '\n' ':')" ma.ac.uae.cloudsim.simulation.UaeSimulation
   ```

---

## 9. Arborescence du Dépôt

```
partie01/
├── .gitignore                          # Fichiers ignorés par Git (target, IDE, logs)
├── README.md                           # Rapport exhaustif du projet et guide
├── pom.xml                             # Configuration Maven et dépendance CloudSim Plus 8.5.7
├── docs/                               # Rapports détaillés par étape
│   ├── 01_02_recherche_architecture.md # Données UAE & Architecture physique/logique
│   ├── 03_modelisation_charges.md      # Spécifications des Cloudlets & profils de charge
│   ├── 04_simulation_politiques.md     # Politiques comparées & résultats détaillés
│   └── 05_resultats_analyse.md         # Analyse des métriques & conclusion finale
├── diagrams/                           # Schémas d'architecture natifs Draw.io
│   ├── 01_architecture_physique.drawio
│   └── 02_architecture_logique_bursting.drawio
├── pics/                               # Captures d'écran et preuves d'exécution
└── src/
    └── main/
        ├── java/ma/ac/uae/cloudsim/
        │   ├── builder/                # Patrons Builder (Datacenters et VMs)
        │   ├── factory/                # Patron Factory (Cloudlets)
        │   ├── models/                 # Modèles de Datacenters
        │   ├── simulation/             # Moteur principal et DTO de résultats
        │   ├── strategy/               # Stratégies d'exécution (Normale, Naïve, Hybride)
        │   └── utils/                  # Constantes UAE et utilitaire de rapport
        └── resources/
            └── logback.xml             # Configuration des journaux d'exécution
```
