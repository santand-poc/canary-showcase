package pl.santander.featureflag.engine;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class MicrometerMetricsProvider implements MetricsProvider {
    private final MeterRegistry reg;
    public MicrometerMetricsProvider(MeterRegistry reg) { this.reg = reg; }

    @Override
    public Optional<Double> errorRatePct(String service, String flag, String segment, Duration window) {
        double ok = counterRate("feature_flag_requests_total",
                Tag.of("service", service), Tag.of("flag", flag), Tag.of("segment", segment), Tag.of("outcome", "ok"), window);
        double err = counterRate("feature_flag_requests_total",
                Tag.of("service", service), Tag.of("flag", flag), Tag.of("segment", segment), Tag.of("outcome", "error"), window);
        double total = ok + err;
        if (total <= 0.0) return Optional.empty();
        return Optional.of(100.0 * err / total);
    }

    @Override
    public Optional<Double> p95Ms(String service, String endpoint, Duration window) {
        Timer timer = reg.find("http.server.requests").tag("service", service).tag("uri", endpoint).timer();
        if (timer == null) return Optional.empty();
        HistogramSnapshot snap = timer.takeSnapshot();
        ValueAtPercentile[] arr = snap.percentileValues();
        if (arr == null || arr.length == 0) return Optional.empty();
        double p95 = java.util.Arrays.stream(arr)
                .filter(v -> Math.abs(v.percentile() - 0.95) < 1e-6)
                .mapToDouble(v -> v.value(TimeUnit.MILLISECONDS))
                .findFirst().orElse(Double.NaN);
        return Double.isNaN(p95) ? Optional.empty() : Optional.of(p95);
    }

    @Override
    public Optional<Double> diffRatePct(String metricKey, Duration window) {
        double same = counterRate("shadow_diff_total", Tag.of("metricKey", metricKey), Tag.of("outcome", "same"), window);
        double diff = counterRate("shadow_diff_total", Tag.of("metricKey", metricKey), Tag.of("outcome", "diff"), window);
        double total = same + diff;
        if (total <= 0.0) return Optional.empty();
        return Optional.of(100.0 * diff / total);
    }

    private double counterRate(String name, Tag t1, Tag t2, Tag t3, Tag t4, Duration window) {
        Counter c = reg.find(name).tags(java.util.List.of(t1, t2, t3, t4)).counter();
        if (c == null) return 0.0;
        return c.count() / Math.max(1.0, window.toSeconds());
    }

    private double counterRate(String name, Tag t1, Tag t2, Duration window) {
        Counter c = reg.find(name).tags(java.util.List.of(t1, t2)).counter();
        if (c == null) return 0.0;
        return c.count() / Math.max(1.0, window.toSeconds());
    }
}
