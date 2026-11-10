package ma.ac.uae.cloudsim.factory;

import ma.ac.uae.cloudsim.utils.Constants;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;

import java.util.ArrayList;
import java.util.List;

/**
 * Design Pattern Factory pour la génération des requêtes étudiantes (Cloudlets).
 * Modélise les deux profils de charge définis dans docs/03_modelisation_charges.md :
 * 1. Période Normale (requêtes de consultation légères, 10 000 MI).
 * 2. Période de Pointe (requêtes lourdes d'examens et inscriptions, 40 000 MI).
 */
public class CloudletWorkloadFactory {

    /**
     * Génère la charge d'une période normale (~3 000 étudiants simultanés).
     */
    public List<Cloudlet> createNormalWorkload(int count) {
        List<Cloudlet> cloudlets = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            Cloudlet cloudlet = new CloudletSimple(Constants.NORMAL_CLOUDLET_LENGTH, 1)
                    .setFileSize(Constants.NORMAL_FILE_SIZE)
                    .setOutputSize(Constants.NORMAL_OUTPUT_SIZE)
                    .setUtilizationModelCpu(new UtilizationModelFull());

            cloudlets.add(cloudlet);
        }

        return cloudlets;
    }

    /**
     * Génère la charge d'une période de pointe (~15 000+ étudiants simultanés).
     * Les requêtes sont plus complexes (40 000 MI) pour modéliser les inscriptions massives et les examens en ligne.
     */
    public List<Cloudlet> createPeakWorkload(int count) {
        List<Cloudlet> cloudlets = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            Cloudlet cloudlet = new CloudletSimple(Constants.PEAK_CLOUDLET_LENGTH, 1)
                    .setFileSize(Constants.PEAK_FILE_SIZE)
                    .setOutputSize(Constants.PEAK_OUTPUT_SIZE)
                    .setUtilizationModelCpu(new UtilizationModelFull());

            cloudlets.add(cloudlet);
        }

        return cloudlets;
    }
}
