package ma.ac.uae.cloudsim.builder;

import ma.ac.uae.cloudsim.utils.Constants;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicy;
import org.cloudsimplus.core.Simulation;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.provisioners.ResourceProvisionerSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;

import java.util.ArrayList;
import java.util.List;

/**
 * Design Pattern Builder pour la création des Datacenters physiques et hybrides de l'UAE.
 * Chaque Datacenter est modélisé selon l'infrastructure underlay validée dans l'étape 2.
 */
public class UaeDatacenterBuilder {

    private final Simulation simulation;

    public UaeDatacenterBuilder(Simulation simulation) {
        this.simulation = simulation;
    }

    /**
     * Datacenter 1 : Tétouan - Privé (Présidence de l'UAE).
     * Héberge 10 serveurs physiques pour la base centrale Apogée Master et les services ENT.
     */
    public Datacenter buildDc1Tetouan(VmAllocationPolicy allocationPolicy) {
        Datacenter dc = createDatacenter("DC1-Tetouan-Prive", Constants.HOSTS_PER_PRIVATE_DC, allocationPolicy);
        return dc;
    }

    /**
     * Datacenter 2 : Tanger - Privé (FST / ENSA Tanger).
     * Héberge 10 serveurs physiques pour la réplication continue Apogée Slave et Moodle socle.
     */
    public Datacenter buildDc2Tanger(VmAllocationPolicy allocationPolicy) {
        Datacenter dc = createDatacenter("DC2-Tanger-Prive", Constants.HOSTS_PER_PRIVATE_DC, allocationPolicy);
        return dc;
    }

    /**
     * Datacenter 3 : Cloud Public (Débordement / Cloud Bursting).
     * Pool élastique de serveurs physiques loués à la demande (AWS / Azure) pour absorber les pics.
     */
    public Datacenter buildDc3CloudPublic(VmAllocationPolicy allocationPolicy) {
        Datacenter dc = createDatacenter("DC3-CloudPublic-Bursting", Constants.HOSTS_PUBLIC_DC, allocationPolicy);
        return dc;
    }

    /**
     * Méthode générique de construction d'un Datacenter avec ses serveurs physiques (Hosts).
     */
    public Datacenter createDatacenter(String name, int hostCount, VmAllocationPolicy allocationPolicy) {
        List<Host> hostList = new ArrayList<>(hostCount);

        for (int i = 0; i < hostCount; i++) {
            List<Pe> peList = new ArrayList<>(Constants.HOST_PES);
            for (int j = 0; j < Constants.HOST_PES; j++) {
                peList.add(new PeSimple(Constants.HOST_MIPS));
            }

            Host host = new HostSimple(Constants.HOST_RAM, Constants.HOST_BW, Constants.HOST_STORAGE, peList)
                    .setRamProvisioner(new ResourceProvisionerSimple())
                    .setBwProvisioner(new ResourceProvisionerSimple())
                    .setVmScheduler(new VmSchedulerTimeShared());

            // Active la collecte des statistiques d'utilisation CPU pour le rapport
            host.enableUtilizationStats();

            hostList.add(host);
        }

        Datacenter datacenter = new DatacenterSimple(simulation, hostList, allocationPolicy);
        datacenter.setName(name);
        // Intervalle régulier d'évaluation nécessaire au déclenchement dynamique de l'HorizontalVmScaling
        datacenter.setSchedulingInterval(Constants.SCHEDULING_INTERVAL);
        return datacenter;
    }
}
