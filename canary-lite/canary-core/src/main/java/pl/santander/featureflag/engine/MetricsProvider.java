package pl.santander.featureflag.engine;

import java.time.Duration;
import java.util.Optional;

public interface MetricsProvider {

    Optional<Double> errorRatePct(String service, String flag, String segment, Duration window);

    Optional<Double> p95Ms(String service, String endpoint, Duration window);

    Optional<Double> diffRatePct(String metricKey, Duration window);
}
