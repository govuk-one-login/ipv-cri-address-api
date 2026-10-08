export interface EventProbe {
    log(error: unknown): EventProbe;
    counterMetric(metricName: string): EventProbe;
}
