package pl.santander.featureflag.engine;

import java.time.Duration;

public class GuardFunctions {
    private final MetricsProvider metrics;
    public GuardFunctions(MetricsProvider metrics) { this.metrics = metrics; }

    public double error_rate(String service, String flag, String segment, String window) {
        return metrics.errorRatePct(service, flag, segment, parseWindow(window)).orElse(Double.NaN);
    }

    public double p95(String service, String endpoint, String window) {
        return metrics.p95Ms(service, endpoint, parseWindow(window)).orElse(Double.NaN);
    }

    public double diff_rate(String metricKey, String window) {
        return metrics.diffRatePct(metricKey, parseWindow(window)).orElse(Double.NaN);
    }

    private static Duration parseWindow(String w) {
        String s = w.trim().toLowerCase();
        if (s.endsWith("ms")) return Duration.ofMillis(Long.parseLong(s.substring(0, s.length()-2)));
        if (s.endsWith("s"))  return Duration.ofSeconds(Long.parseLong(s.substring(0, s.length()-1)));
        if (s.endsWith("m"))  return Duration.ofMinutes(Long.parseLong(s.substring(0, s.length()-1)));
        if (s.endsWith("h"))  return Duration.ofHours(Long.parseLong(s.substring(0, s.length()-1)));
        return Duration.parse(w);
    }
}
