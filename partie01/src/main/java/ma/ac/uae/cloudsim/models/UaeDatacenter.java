package ma.ac.uae.cloudsim.models;

import ma.ac.uae.cloudsim.builder.UaeDatacenterBuilder;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicy;
import org.cloudsimplus.core.Simulation;
import org.cloudsimplus.datacenters.Datacenter;

/**
 * Classe modèle pour les Datacenters de l'Université Abdelmalek Essaâdi.
 * Déléguée au builder {@link UaeDatacenterBuilder}.
 */
public class UaeDatacenter {

    public static Datacenter createDatacenter(Simulation simulation, int hostCount, VmAllocationPolicy allocationPolicy) {
        UaeDatacenterBuilder builder = new UaeDatacenterBuilder(simulation);
        return builder.createDatacenter("UAE-Datacenter", hostCount, allocationPolicy);
    }
}
