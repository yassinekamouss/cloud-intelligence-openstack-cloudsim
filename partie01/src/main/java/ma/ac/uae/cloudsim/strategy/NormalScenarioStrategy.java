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
 * Scénario 1 : Période Normale (Trafic régulier quotidien de l'UAE).
 * - Ordonnancement des tâches : CloudletSchedulerSpaceShared (isolation stricte par cœur).
 * - Allocation des VMs : VmAllocationPolicySimple (premier hôte disponible).
 * - Datacenters mobilisés : DC1 (Tétouan) et DC2 (Tanger) privés uniquement.
 * - Auto-scaling : Désactivé (inutile en charge nominale).
 */
public class NormalScenarioStrategy implements SimulationStrategy {

    @Override
    public String getStrategyName() {
        return "1. Période Normale (SpaceShared + Simple Allocation)";
    }

    @Override
    public SimulationExecutionResult execute() {
        CloudSimPlus simulation = new CloudSimPlus();
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);

        // 1. Datacenters privés
        UaeDatacenterBuilder dcBuilder = new UaeDatacenterBuilder(simulation);
        Datacenter dc1 = dcBuilder.buildDc1Tetouan(new VmAllocationPolicySimple());
        Datacenter dc2 = dcBuilder.buildDc2Tanger(new VmAllocationPolicySimple());

        // 2. Création des VMs privées (20 VMs au total réparties sur les 20 Hosts)
        UaeVmBuilder vmBuilder = new UaeVmBuilder();
        List<Vm> vmList = new ArrayList<>();

        // Base de données Apogée Master (DC1) & Replica (DC2)
        vmList.add(vmBuilder.buildDatabaseMasterVm(new CloudletSchedulerSpaceShared()));
        vmList.add(vmBuilder.buildDatabaseReplicaVm(new CloudletSchedulerSpaceShared()));

        // Portail ENT & CAS (4 instances sur DC1)
        for (int i = 0; i < 4; i++) {
            vmList.add(vmBuilder.buildEntVm(new CloudletSchedulerSpaceShared()));
        }

        // Moodle LMS de base (14 instances sur DC2 et pool privé)
        for (int i = 0; i < 14; i++) {
            vmList.add(vmBuilder.buildMoodleVm(new CloudletSchedulerSpaceShared()));
        }

        broker.submitVmList(vmList);

        // 3. Génération de la charge normale (10 000 Cloudlets)
        CloudletWorkloadFactory factory = new CloudletWorkloadFactory();
        List<Cloudlet> cloudletList = factory.createNormalWorkload(Constants.NORMAL_CLOUDLET_COUNT);
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

        // Calcul utilisation CPU moyenne des hôtes
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

        // Coût estimé (Infrastructure privée : coût énergétique/maintenance fixe ~0.15 DH / sec de calcul actif)
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
