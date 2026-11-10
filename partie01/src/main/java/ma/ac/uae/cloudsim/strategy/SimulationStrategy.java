package ma.ac.uae.cloudsim.strategy;

import ma.ac.uae.cloudsim.simulation.SimulationExecutionResult;

/**
 * Interface du Design Pattern Strategy pour alterner entre les scénarios et politiques
 * de simulation (ordonnancement des tâches et allocation des VMs).
 */
public interface SimulationStrategy {

    /**
     * Nom descriptif de la stratégie simulée.
     */
    String getStrategyName();

    /**
     * Exécute le scénario dans CloudSim Plus et extrait les métriques de performance.
     */
    SimulationExecutionResult execute();
}
