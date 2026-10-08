export interface EventProbe {
    counterMetric(metricName: string): void;

    addDimensions(dimensions: Record<string, string>): void;
}
