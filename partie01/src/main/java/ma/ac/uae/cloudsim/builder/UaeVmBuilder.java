package ma.ac.uae.cloudsim.builder;

import ma.ac.uae.cloudsim.utils.Constants;
import org.cloudsimplus.autoscaling.HorizontalVmScaling;
import org.cloudsimplus.autoscaling.HorizontalVmScalingSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletScheduler;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.util.function.Supplier;

/**
 * Design Pattern Builder pour la création des Machines Virtuelles (VMs) de l'UAE.
 * Chaque gabarit correspond précisément aux spécifications logiques modélisées dans l'étape 2.
 */
public class UaeVmBuilder {

    /**
     * VM Database Apogée Master (Scolarité centrale, hébergée sur DC1 Tétouan).
     * Profil IaaS critique : 16 vCPU, 32 Go RAM.
     */
    public Vm buildDatabaseMasterVm(CloudletScheduler scheduler) {
        return new VmSimple(Constants.VM_MIPS, Constants.VM_DB_PES)
                .setRam(Constants.VM_DB_RAM)
                .setBw(Constants.VM_DB_BW)
                .setSize(Constants.VM_DB_SIZE)
                .setCloudletScheduler(scheduler)
                .setDescription("VM_Database_Master_Tetouan");
    }

    /**
     * VM Database Apogée Replica (Miroir de réplication et failover, hébergée sur DC2 Tanger).
     * Profil IaaS critique : 16 vCPU, 32 Go RAM.
     */
    public Vm buildDatabaseReplicaVm(CloudletScheduler scheduler) {
        return new VmSimple(Constants.VM_MIPS, Constants.VM_DB_PES)
                .setRam(Constants.VM_DB_RAM)
                .setBw(Constants.VM_DB_BW)
                .setSize(Constants.VM_DB_SIZE)
                .setCloudletScheduler(scheduler)
                .setDescription("VM_Database_Replica_Tanger");
    }

    /**
     * VM ENT Portal & Authentification CAS (Portail étudiant, hébergée sur DC1).
     * Profil SaaS : 4 vCPU, 8 Go RAM.
     */
    public Vm buildEntVm(CloudletScheduler scheduler) {
        return new VmSimple(Constants.VM_MIPS, Constants.VM_ENT_PES)
                .setRam(Constants.VM_ENT_RAM)
                .setBw(Constants.VM_ENT_BW)
                .setSize(Constants.VM_ENT_SIZE)
                .setCloudletScheduler(scheduler)
                .setDescription("VM_ENT_Portal");
    }

    /**
     * VM Moodle LMS Principal (Plateforme pédagogique socle, hébergée sur DC2).
     * Profil PaaS : 8 vCPU, 16 Go RAM.
     */
    public Vm buildMoodleVm(CloudletScheduler scheduler) {
        return new VmSimple(Constants.VM_MIPS, Constants.VM_MOODLE_PES)
                .setRam(Constants.VM_MOODLE_RAM)
                .setBw(Constants.VM_MOODLE_BW)
                .setSize(Constants.VM_MOODLE_SIZE)
                .setCloudletScheduler(scheduler)
                .setDescription("VM_Moodle_Base");
    }

    /**
     * VM Moodle Éphémère (Instance provisionnée dynamiquement sur DC3 Cloud Public).
     * Profil PaaS éphémère : 8 vCPU, 16 Go RAM.
     */
    public Vm buildScaledMoodleVm(CloudletScheduler scheduler) {
        return new VmSimple(Constants.VM_MIPS, Constants.VM_MOODLE_PES)
                .setRam(Constants.VM_MOODLE_RAM)
                .setBw(Constants.VM_MOODLE_BW)
                .setSize(Constants.VM_MOODLE_SIZE)
                .setCloudletScheduler(scheduler)
                .setDescription("VM_Moodle_ScaleOut_DC3");
    }

    /**
     * Attache un mécanisme d'élasticité horizontale (HorizontalVmScaling) à une VM.
     * Dès que l'utilisation CPU dépasse le seuil défini (80%), une nouvelle VM est instanciée
     * sur le Datacenter 3 (Cloud Public) pour absorber la charge.
     */
    public Vm attachHorizontalScaling(Vm vm, Supplier<Vm> scaledVmSupplier, double cpuThreshold) {
        java.util.concurrent.atomic.AtomicBoolean alreadyScaled = new java.util.concurrent.atomic.AtomicBoolean(false);
        HorizontalVmScaling scaling = new HorizontalVmScalingSimple();
        scaling.setVmSupplier(() -> {
            alreadyScaled.set(true);
            return scaledVmSupplier.get();
        });
        scaling.setOverloadPredicate(v -> !alreadyScaled.get() && v.getCpuPercentUtilization() > cpuThreshold);
        vm.setHorizontalScaling(scaling);
        return vm;
    }
}
