package ma.ac.uae.cloudsim.simulation;

import ch.qos.logback.classic.Level;
import ma.ac.uae.cloudsim.strategy.NormalScenarioStrategy;
import ma.ac.uae.cloudsim.strategy.PeakHybridBurstingStrategy;
import ma.ac.uae.cloudsim.strategy.PeakNaiveScenarioStrategy;
import ma.ac.uae.cloudsim.strategy.SimulationStrategy;
import ma.ac.uae.cloudsim.utils.SimulationReportHelper;
import org.cloudsimplus.util.Log;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrateur principal de simulation CloudSim Plus pour l'Université Abdelmalek Essaâdi.
 * Répond aux questions 3 et 4 du cahier des charges en exécutant et comparant :
 * 1. Période Normale (Trafic nominal, politiques de base SpaceShared et Simple).
 * 2. Période de Pointe Naïve (Surcharge sur datacenters privés sans élasticité).
 * 3. Période de Pointe Hybride & Auto-Scaling (Cloud Bursting vers Datacenter 3 avec TimeShared et BestFit).
 */
public class UaeSimulation {

    static {
        // Désactive les logs verbeux de chaque Cloudlet pour accélérer l'exécution
        Log.setLevel(Level.WARN);
        Log.setLevel(LoggerFactory.getLogger("org.cloudsimplus.autoscaling"), Level.INFO);
    }

    private final boolean isPeakPeriod;

    public UaeSimulation(boolean isPeakPeriod) {
        this.isPeakPeriod = isPeakPeriod;
    }

    /**
     * Lance la simulation pour la période spécifiée.
     */
    public void start() {
        System.out.println("================================================================================");
        System.out.println("  Démarrage de la simulation UAE - Période : " + (isPeakPeriod ? "POINTE" : "NORMALE"));
        System.out.println("================================================================================");

        SimulationStrategy strategy = isPeakPeriod 
                ? new PeakHybridBurstingStrategy() 
                : new NormalScenarioStrategy();

        SimulationExecutionResult result = strategy.execute();
        SimulationReportHelper.printComparisonTable(List.of(result));
    }

    /**
     * Exécute les 3 scénarios pour comparer formellement les politiques d'ordonnancement,
     * d'allocation et l'impact de l'auto-scaling (Question 4 du cahier des charges).
     */
    public static void runComparison() {
        System.out.println("\n>>> DÉMARRAGE DE L'ÉTUDE COMPARATIVE DES POLITIQUES CLOUDSIM PLUS POUR L'UAE...\n");

        List<SimulationStrategy> strategies = List.of(
                new NormalScenarioStrategy(),
                new PeakNaiveScenarioStrategy(),
                new PeakHybridBurstingStrategy()
        );

        List<SimulationExecutionResult> results = new ArrayList<>();

        for (SimulationStrategy strategy : strategies) {
            System.out.println("-> Exécution du scénario : " + strategy.getStrategyName() + "...");
            SimulationExecutionResult res = strategy.execute();
            results.add(res);
            System.out.println("   [Terminé] Makespan = " + String.format("%.2f", res.getFinishTime()) 
                    + "s | Tps réponse moyen = " + String.format("%.2f", res.getAvgTurnaroundTime()) 
                    + "s | VMs créées = " + res.getTotalVmsCreated() + "\n");
        }

        // Affichage du tableau récapitulatif complet
        SimulationReportHelper.printComparisonTable(results);
    }

    public static void main(String[] args) {
        // Exécute la comparaison globale pour produire les résultats du rapport
        runComparison();
    }
}
