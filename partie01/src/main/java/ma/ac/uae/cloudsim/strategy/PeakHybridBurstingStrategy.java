package ma.ac.uae.cloudsim.strategy;

import ma.ac.uae.cloudsim.builder.UaeDatacenterBuilder;
import ma.ac.uae.cloudsim.builder.UaeVmBuilder;
import ma.ac.uae.cloudsim.factory.CloudletWorkloadFactory;
import ma.ac.uae.cloudsim.simulation.SimulationExecutionResult;
import ma.ac.uae.cloudsim.utils.Constants;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicyBestFit;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudsimplus.vms.Vm;

import java.util.ArrayList;
import java.util.List;

/**
 * Scénario 3 : Période de Pointe AVEC Architecture Hybride & Cloud Bursting (Solution Recommandée UAE).
 * - Ordonnancement des tâches : CloudletSchedulerTimeShared (partage équitable du temps CPU, élimination des blocages).
 * - Allocation des VMs : VmAllocationPolicyBestFit (placement optimal réduisant la fragmentation matérielle).
 * - Datacenters mobilisés : DC1 (Tétouan), DC2 (Tanger) et DC3 (Cloud Public Hyperscaler).
 * - Auto-scaling : HorizontalVmScaling actif (création automatique de VMs Moodle sur DC3 dès que le CPU dépasse 80%).
 * Cette stratégie assure 100% de disponibilité et une réduction drastique du temps d'attente étudiant.
 */
public class PeakHybridBurstingStrategy implements SimulationStrategy {

    @Override
    public String getStrategyName() {
        return "3. Période de Pointe Optimisée (TimeShared + BestFit + Cloud Bursting & Auto-Scaling)";
    }

    @Override
    public SimulationExecutionResult execute() {
        CloudSimPlus simulation = new CloudSimPlus();
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);

        // 1. Déploiement de l'architecture hybride à 3 Datacenters avec VmAllocationPolicyBestFit
        UaeDatacenterBuilder dcBuilder = new UaeDatacenterBuilder(simulation);
        Datacenter dc1 = dcBuilder.buildDc1Tetouan(new VmAllocationPolicyBestFit());
        Datacenter dc2 = dcBuilder.buildDc2Tanger(new VmAllocationPolicyBestFit());
        Datacenter dc3 = dcBuilder.buildDc3CloudPublic(new VmAllocationPolicyBestFit());

        // Routage architectural : Les VMs privées vers DC1/DC2, les VMs de débordement vers DC3 Cloud Public
        broker.setDatacenterMapper((lastDc, vm) -> {
            if (vm.getDescription().contains("DC3")) return dc3;
            if (vm.getDescription().contains("Tetouan") || vm.getDescription().contains("ENT")) return dc1;
            return dc2;
        });

        // 2. Création des VMs initiales avec TimeShared (20 VMs initiales)
        UaeVmBuilder vmBuilder = new UaeVmBuilder();
        List<Vm> vmList = new ArrayList<>();

        vmList.add(vmBuilder.buildDatabaseMasterVm(new CloudletSchedulerTimeShared()));
        vmList.add(vmBuilder.buildDatabaseReplicaVm(new CloudletSchedulerTimeShared()));

        for (int i = 0; i < 4; i++) {
            vmList.add(vmBuilder.buildEntVm(new CloudletSchedulerTimeShared()));
        }

        // Création des 14 VMs Moodle avec attachement du mécanisme HorizontalVmScaling
        for (int i = 0; i < 14; i++) {
            Vm moodleVm = vmBuilder.buildMoodleVm(new CloudletSchedulerTimeShared());
            
            // Fournisseur dynamique instanciant une VM Moodle supplémentaire sur DC3 en cas de saturation (> 80%)
            vmBuilder.attachHorizontalScaling(
                    moodleVm,
                    () -> vmBuilder.buildScaledMoodleVm(new CloudletSchedulerTimeShared()),
                    Constants.CPU_OVERLOAD_THRESHOLD
            );

            vmList.add(moodleVm);
        }

        broker.submitVmList(vmList);

        // 3. Charge lourde de pointe (50 000 Cloudlets arrivant par vagues)
        CloudletWorkloadFactory factory = new CloudletWorkloadFactory();
        List<Cloudlet> cloudletList = factory.createPeakWorkload(Constants.PEAK_CLOUDLET_COUNT);
        broker.submitCloudletList(cloudletList);

        // 4. Lancement de la simulation
        simulation.start();

        // 5. Calcul des métriques de synthèse
        List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        int totalFinished = finishedCloudlets.size();

        double totalTurnaroundTime = 0;
        double totalWaitingTime = 0;
        for (Cloudlet c : finishedCloudlets) {
            totalTurnaroundTime += (c.getFinishTime() - c.getSubmissionDelay());
            totalWaitingTime += c.getStartWaitTime();
        }

        double avgTurnaround = totalFinished > 0 ? totalTurnaroundTime / totalFinished : 0;
        double avgWaiting = totalFinished > 0 ? totalWaitingTime / totalFinished : 0;
        double finishTime = simulation.clock();

        // Utilisation CPU moyenne sur les 3 datacenters
        double totalHostCpu = 0;
        int hostCount = 0;
        for (Datacenter dc : List.of(dc1, dc2, dc3)) {
            for (Host h : dc.getHostList()) {
                double mean = h.getCpuUtilizationStats().getMean();
                if (!Double.isNaN(mean)) totalHostCpu += mean;
                hostCount++;
            }
        }
        double avgHostCpuPercent = (hostCount > 0 ? (totalHostCpu / hostCount) * 100.0 : 0);

        // Coût estimé : Fixe local (0.15 DH/s) + Coût élastique Cloud Public Pay-as-you-go (~0.05 DH/s par VM scalée)
        int initialVmCount = 20;
        int scaledVmCount = Math.max(0, broker.getVmCreatedList().size() - initialVmCount);
        double estimatedCost = (finishTime * 0.15) + (finishTime * scaledVmCount * 0.05);

        return new SimulationExecutionResult(
                getStrategyName(),
                "CloudletSchedulerTimeShared",
                "VmAllocationPolicyBestFit",
                true,
                cloudletList.size(),
                totalFinished,
                initialVmCount,
                broker.getVmCreatedList().size(),
                finishTime,
                avgTurnaround,
                avgWaiting,
                avgHostCpuPercent,
                estimatedCost
        );
    }
}
