package ma.ac.uae.cloudsim.utils;

import ma.ac.uae.cloudsim.simulation.SimulationExecutionResult;

import java.util.List;

/**
 * Utilitaire pour formater et afficher les tableaux comparatifs de résultats
 * pour le rapport final (Questions 4 et 5 du cahier des charges).
 */
public class SimulationReportHelper {

    public static void printComparisonTable(List<SimulationExecutionResult> results) {
        String separator = "+----------------------------------------------------------------+----------------------------+-------------------------+--------------+-----------+--------------+------------------+------------------+---------------+--------------+";
        String header    = "| Scénario Simulé                                                | Ordonnancement (VM)        | Allocation (Datacenter) | Auto-Scaling | Cloudlets | VMs (Init/Tot)| Temps Total (s)  | Tps Réponse Moy. | Tps Attente   | CPU Hôtes (%) | Coût (DH)    |";

        System.out.println("\n" + "=".repeat(168));
        System.out.println("                 RÉSULTATS COMPARATIFS DE LA SIMULATION DU CLOUD UNIVERSITAIRE UAE (CLOUDSIM PLUS)");
        System.out.println("=".repeat(168));
        System.out.println(separator);
        System.out.println(header);
        System.out.println(separator);

        for (SimulationExecutionResult r : results) {
            System.out.printf("| %-62s | %-26s | %-23s | %-12s | %-9d | %-12s | %-16.2f | %-16.2f | %-13.2f | %-13.1f | %-12.2f |\n",
                    r.getScenarioName(),
                    r.getSchedulingPolicy(),
                    r.getAllocationPolicy(),
                    r.isAutoScalingEnabled() ? "OUI (Burst)" : "NON",
                    r.getCloudletsFinished(),
                    r.getInitialVms() + " / " + r.getTotalVmsCreated(),
                    r.getFinishTime(),
                    r.getAvgTurnaroundTime(),
                    r.getAvgWaitingTime(),
                    r.getAvgCpuUtilizationPercent(),
                    r.getEstimatedCostDh()
            );
        }

        System.out.println(separator);
        System.out.println();
    }
}
