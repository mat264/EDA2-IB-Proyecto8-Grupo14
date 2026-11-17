package com.epn.eda2.metrics;

import java.util.Locale;

/**
 * Almacena las métricas resultantes de la evaluación experimental (Train/Test 80/20)
 * y benchmarks de rendimiento (Tiempo y Memoria).
 */
public class BenchmarkResult {
    private final int k;
    private final double precisionAtK;
    private final double recallAtK;
    private final double graphBuildTimeMs;
    private final double avgRecommendationTimeMs;
    private final double memoryUsedMb;
    private final int evaluatedUsers;
    private final int trainEdges;
    private final int testEdges;

    public BenchmarkResult(
            int k,
            double precisionAtK,
            double recallAtK,
            double graphBuildTimeMs,
            double avgRecommendationTimeMs,
            double memoryUsedMb,
            int evaluatedUsers,
            int trainEdges,
            int testEdges) {
        this.k = k;
        this.precisionAtK = precisionAtK;
        this.recallAtK = recallAtK;
        this.graphBuildTimeMs = graphBuildTimeMs;
        this.avgRecommendationTimeMs = avgRecommendationTimeMs;
        this.memoryUsedMb = memoryUsedMb;
        this.evaluatedUsers = evaluatedUsers;
        this.trainEdges = trainEdges;
        this.testEdges = testEdges;
    }

    public int getK() {
        return k;
    }

    public double getPrecisionAtK() {
        return precisionAtK;
    }

    public double getRecallAtK() {
        return recallAtK;
    }

    public double getGraphBuildTimeMs() {
        return graphBuildTimeMs;
    }

    public double getAvgRecommendationTimeMs() {
        return avgRecommendationTimeMs;
    }

    public double getMemoryUsedMb() {
        return memoryUsedMb;
    }

    public int getEvaluatedUsers() {
        return evaluatedUsers;
    }

    public int getTrainEdges() {
        return trainEdges;
    }

    public int getTestEdges() {
        return testEdges;
    }

    @Override
    public String toString() {
        return String.format(
                Locale.US,
                "Benchmark @K=%d | Precision@K=%.4f | Recall@K=%.4f | Build=%.3f ms | AvgRec=%.3f ms | Mem=%.2f MB",
                k, precisionAtK, recallAtK, graphBuildTimeMs, avgRecommendationTimeMs, memoryUsedMb
        );
    }
}
