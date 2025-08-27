package pl.santander.showcase.consumer;

import org.springframework.web.bind.annotation.*;
import pl.santander.featureflag.engine.GuardEngine;
import pl.santander.featureflag.engine.MetricsProvider;
import pl.santander.featureflag.spring.CanaryProperties;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/ui")
public class MetricsUiController {
    private final TrafficSimulator sim;
    private final MetricsProvider metrics;
    private final GuardEngine engine;
    private final CanaryProperties props;
    private final FeatureFlagService flags;
    private final CanaryAutopilot autopilot;

    public MetricsUiController(TrafficSimulator sim, MetricsProvider metrics, GuardEngine engine,
                               CanaryProperties props, FeatureFlagService flags, CanaryAutopilot autopilot) {
        this.sim = sim;
        this.metrics = metrics;
        this.engine = engine;
        this.props = props;
        this.flags = flags;
        this.autopilot = autopilot;
    }

    @PostMapping("/start")
    public Map<String, Object> start() {
        sim.start();
        return Map.of("running", sim.isRunning());
    }

    @PostMapping("/stop")
    public Map<String, Object> stop() {
        sim.stop();
        return Map.of("running", sim.isRunning());
    }

    @PostMapping("/inject")
    public Map<String, Object> inject(@RequestParam(name = "err", defaultValue = "0.02") double err,
                                      @RequestParam(name = "latencyMs", defaultValue = "200") int latencyMs,
                                      @RequestParam(name = "diff", defaultValue = "0.10") double diff,
                                      @RequestParam(name = "sec", defaultValue = "30") int sec) {
        sim.injectOnProducer(err, latencyMs, diff, sec);
        return Map.of("ok", true);
    }

    @PostMapping("/percent")
    public Map<String, Object> setPct(@RequestParam("pct") int pct) {
        flags.setCanaryPercent(pct);
        return Map.of("pct", flags.getCanaryPercent());
    }

    @PostMapping("/autopilot")
    public Map<String, Object> setAutopilot(@RequestParam("on") boolean on) {
        autopilot.setEnabled(on);
        return Map.of("autopilot", autopilot.isEnabled());
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        double er = metrics.errorRatePct("consumer-service", "release.new_rules_v2", "canary", Duration.ofSeconds(30)).orElse(Double.NaN);
        double p95 = metrics.p95Ms("consumer-service", "/consumer/do", Duration.ofSeconds(30)).orElse(Double.NaN);
        double dr = metrics.diffRatePct("decision_delta", Duration.ofSeconds(30)).orElse(Double.NaN);

        GuardEngine.CompiledGuard compiled = engine.compile(props.getExpr());
        GuardEngine.Result res = compiled.evaluate(props.getTimeoutMs());

        return Map.of(
                "running", sim.isRunning(),
                "autopilot", autopilot.isEnabled(),
                "ladder", props.getLadder(),
                "canaryPercent", flags.getCanaryPercent(),
                "errorRatePct", er,
                "p95ms", p95,
                "diffRatePct", dr,
                "guard", res.status().toString(),
                "guardNote", res.note()
        );
    }
}
