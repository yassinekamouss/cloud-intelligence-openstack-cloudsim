package ma.ac.uae.cloudsim.strategy;

import ma.ac.uae.cloudsim.builder.UaeDatacenterBuilder;
import ma.ac.uae.cloudsim.builder.UaeVmBuilder;
import ma.ac.uae.cloudsim.factory.CloudletWorkloadFactory;
import ma.ac.uae.cloudsim.simulation.SimulationExecutionResult;
import ma.ac.uae.cloudsim.utils.Constants;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicySimple;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudsimplus.vms.Vm;

import java.util.ArrayList;
import java.util.List;

/**
 * Scénario 2 : Période de Pointe SANS Cloud Bursting (Approche Naïve On-Premise).
 * - Ordonnancement des tâches : CloudletSchedulerSpaceShared (les tâches restent bloquées en file d'attente).
 * - Allocation des VMs : VmAllocationPolicySimple.
 * - Datacenters mobilisés : DC1 et DC2 uniquement (capacité fixe sans débordement public).
 * - Auto-scaling : Aucun (pas d'élasticité).
 * Ce scénario démontre la saturation et l'explosion des temps de réponse lorsque l'UAE n'adopte pas le modèle hybride.
 */
public class PeakNaiveScenarioStrategy implements SimulationStrategy {

    @Override
    public String getStrategyName() {
        return "2. Période de Pointe Naïve (SpaceShared, Sans Auto-Scaling, Privé Fixe)";
    }

    @Override
    public SimulationExecutionResult execute() {
        CloudSimPlus simulation = new CloudSimPlus();
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);

        // 1. Datacenters privés à capacité figée
        UaeDatacenterBuilder dcBuilder = new UaeDatacenterBuilder(simulation);
        Datacenter dc1 = dcBuilder.buildDc1Tetouan(new VmAllocationPolicySimple());
        Datacenter dc2 = dcBuilder.buildDc2Tanger(new VmAllocationPolicySimple());

        // 2. Création des mêmes VMs privées fixes (20 VMs au total réparties sur les 20 Hosts)
        UaeVmBuilder vmBuilder = new UaeVmBuilder();
        List<Vm> vmList = new ArrayList<>();

        vmList.add(vmBuilder.buildDatabaseMasterVm(new CloudletSchedulerSpaceShared()));
        vmList.add(vmBuilder.buildDatabaseReplicaVm(new CloudletSchedulerSpaceShared()));

        for (int i = 0; i < 4; i++) {
            vmList.add(vmBuilder.buildEntVm(new CloudletSchedulerSpaceShared()));
        }

        for (int i = 0; i < 14; i++) {
            vmList.add(vmBuilder.buildMoodleVm(new CloudletSchedulerSpaceShared()));
        }

        broker.submitVmList(vmList);

        // 3. Charge lourde de pointe (50 000 Cloudlets lourds)
        CloudletWorkloadFactory factory = new CloudletWorkloadFactory();
        List<Cloudlet> cloudletList = factory.createPeakWorkload(Constants.PEAK_CLOUDLET_COUNT);
        broker.submitCloudletList(cloudletList);

        // 4. Lancement de la simulation
        simulation.start();

        // 5. Calcul des métriques
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

        double totalHostCpu = 0;
        int hostCount = 0;
        for (Host h : dc1.getHostList()) {
            double mean = h.getCpuUtilizationStats().getMean();
            if (!Double.isNaN(mean)) totalHostCpu += mean;
            hostCount++;
        }
        for (Host h : dc2.getHostList()) {
            double mean = h.getCpuUtilizationStats().getMean();
            if (!Double.isNaN(mean)) totalHostCpu += mean;
            hostCount++;
        }
        double avgHostCpuPercent = (hostCount > 0 ? (totalHostCpu / hostCount) * 100.0 : 0);

        // Coût énergétique fixe prolongé par l'extrême lenteur d'évacuation de la file
        double estimatedCost = finishTime * 0.15;

        return new SimulationExecutionResult(
                getStrategyName(),
                "CloudletSchedulerSpaceShared",
                "VmAllocationPolicySimple",
                false,
                cloudletList.size(),
                totalFinished,
                vmList.size(),
                broker.getVmCreatedList().size(),
                finishTime,
                avgTurnaround,
                avgWaiting,
                avgHostCpuPercent,
                estimatedCost
        );
    }
}
