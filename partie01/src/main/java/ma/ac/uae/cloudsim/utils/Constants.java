package ma.ac.uae.cloudsim.utils;

/**
 * Constantes de configuration matérielle, virtuelle et applicative
 * pour la simulation du Cloud de l'Université Abdelmalek Essaâdi (UAE).
 * Ces valeurs correspondent strictement aux dimensions justifiées dans docs/01_02_recherche_architecture.md
 * et aux schémas d'architecture diagrams/01_architecture_physique.drawio et 02_architecture_logique_bursting.drawio.
 */
public final class Constants {

    private Constants() {
        // Empêche l'instanciation de cette classe utilitaire
    }

    // =========================================================================
    // 1. INFRASTRUCTURE PHYSIQUE (HOSTS & DATACENTERS)
    // =========================================================================
    /** Nombre de serveurs physiques par datacenter privé (Tétouan et Tanger) */
    public static final int HOSTS_PER_PRIVATE_DC = 10;
    
    /** Nombre de serveurs physiques élastiques dans le cloud public pour absorber le pic */
    public static final int HOSTS_PUBLIC_DC = 30;

    /** Nombre de cœurs (Processing Elements) par Host physique */
    public static final int HOST_PES = 64;

    /** Puissance unitaire de chaque cœur en MIPS (1 000 MIPS = 1 GIPS par PE) */
    public static final double HOST_MIPS = 1000;

    /** Mémoire vive par serveur physique : 256 Go (exprimé en Mo) */
    public static final long HOST_RAM = 256 * 1024; // 262 144 Mo

    /** Bande passante réseau par serveur physique : 10 Gbps (exprimé en Mbps) */
    public static final long HOST_BW = 10000;

    /** Stockage baie SAN partagée : 10 To (exprimé en Mo) */
    public static final long HOST_STORAGE = 10L * 1024 * 1024; // 10 485 760 Mo

    // =========================================================================
    // 2. MACHINES VIRTUELLES (VMS) PAR SERVICE NUMÉRIQUE UAE
    // =========================================================================
    // VM Database Apogée (Maître & Réplication - IaaS Privé)
    public static final int VM_DB_PES = 16;
    public static final long VM_DB_RAM = 32 * 1024; // 32 Go (32 768 Mo)
    public static final long VM_DB_BW = 1000;       // 1 Gbps
    public static final long VM_DB_SIZE = 50000;    // 50 Go image disque

    // VM Moodle (PaaS E-Learning - Déployée sur privé + débordement public)
    public static final int VM_MOODLE_PES = 8;
    public static final long VM_MOODLE_RAM = 16 * 1024; // 16 Go (16 384 Mo)
    public static final long VM_MOODLE_BW = 1000;       // 1 Gbps
    public static final long VM_MOODLE_SIZE = 30000;    // 30 Go image disque

    // VM ENT (Portail web, Messagerie & Authentification CAS - SaaS Privé)
    public static final int VM_ENT_PES = 4;
    public static final long VM_ENT_RAM = 8 * 1024; // 8 Go (8 192 Mo)
    public static final long VM_ENT_BW = 1000;      // 1 Gbps
    public static final long VM_ENT_SIZE = 20000;   // 20 Go image disque

    // MIPS alloués aux VMs
    public static final double VM_MIPS = 1000;

    // =========================================================================
    // 3. CHARGES DE TRAVAIL ÉTUDIANTE (CLOUDLETS)
    // =========================================================================
    // Période Normale (~3 000 utilisateurs - consultations légères)
    public static final int NORMAL_CLOUDLET_COUNT = 1500;
    public static final long NORMAL_CLOUDLET_LENGTH = 10000; // MI (Millions d'Instructions)
    public static final long NORMAL_FILE_SIZE = 300;         // Ko
    public static final long NORMAL_OUTPUT_SIZE = 300;       // Ko

    // Période de Pointe (~15 000+ étudiants - inscriptions massives & examens en ligne)
    public static final int PEAK_CLOUDLET_COUNT = 2500;
    public static final long PEAK_CLOUDLET_LENGTH = 40000;   // MI (Requêtes lourdes)
    public static final long PEAK_FILE_SIZE = 2048;          // 2 Mo
    public static final long PEAK_OUTPUT_SIZE = 2048;        // 2 Mo

    // =========================================================================
    // 4. PARAMÈTRES D'AUTO-SCALING ET DE SURVEILLANCE
    // =========================================================================
    /** Seuil d'utilisation CPU déclenchant le Scale-Out (80%) */
    public static final double CPU_OVERLOAD_THRESHOLD = 0.80;

    /** Intervalle de vérification des politiques et métriques d'ordonnancement (en secondes) */
    public static final double SCHEDULING_INTERVAL = 1.0;
}
