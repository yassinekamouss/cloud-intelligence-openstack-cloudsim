package ma.ac.uae.cloudsim.simulation;

/**
 * Encapsule les métriques d'évaluation d'un scénario de simulation CloudSim Plus.
 * Ces résultats alimentent l'analyse demandée à la question 5 du cahier des charges.
 */
public class SimulationExecutionResult {

    private final String scenarioName;
    private final String schedulingPolicy;
    private final String allocationPolicy;
    private final boolean autoScalingEnabled;
    private final int cloudletsSubmitted;
    private final int cloudletsFinished;
    private final int initialVms;
    private final int totalVmsCreated;
    private final double finishTime;
    private final double avgTurnaroundTime;
    private final double avgWaitingTime;
    private final double avgCpuUtilizationPercent;
    private final double estimatedCostDh;

    public SimulationExecutionResult(String scenarioName, String schedulingPolicy, String allocationPolicy,
                                     boolean autoScalingEnabled, int cloudletsSubmitted, int cloudletsFinished,
                                     int initialVms, int totalVmsCreated, double finishTime,
                                     double avgTurnaroundTime, double avgWaitingTime,
                                     double avgCpuUtilizationPercent, double estimatedCostDh) {
        this.scenarioName = scenarioName;
        this.schedulingPolicy = schedulingPolicy;
        this.allocationPolicy = allocationPolicy;
        this.autoScalingEnabled = autoScalingEnabled;
        this.cloudletsSubmitted = cloudletsSubmitted;
        this.cloudletsFinished = cloudletsFinished;
        this.initialVms = initialVms;
        this.totalVmsCreated = totalVmsCreated;
        this.finishTime = finishTime;
        this.avgTurnaroundTime = avgTurnaroundTime;
        this.avgWaitingTime = avgWaitingTime;
        this.avgCpuUtilizationPercent = avgCpuUtilizationPercent;
        this.estimatedCostDh = estimatedCostDh;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public String getSchedulingPolicy() {
        return schedulingPolicy;
    }

    public String getAllocationPolicy() {
        return allocationPolicy;
    }

    public boolean isAutoScalingEnabled() {
        return autoScalingEnabled;
    }

    public int getCloudletsSubmitted() {
        return cloudletsSubmitted;
    }

    public int getCloudletsFinished() {
        return cloudletsFinished;
    }

    public int getInitialVms() {
        return initialVms;
    }

    public int getTotalVmsCreated() {
        return totalVmsCreated;
    }

    public double getFinishTime() {
        return finishTime;
    }

    public double getAvgTurnaroundTime() {
        return avgTurnaroundTime;
    }

    public double getAvgWaitingTime() {
        return avgWaitingTime;
    }

    public double getAvgCpuUtilizationPercent() {
        return avgCpuUtilizationPercent;
    }

    public double getEstimatedCostDh() {
        return estimatedCostDh;
    }
}
