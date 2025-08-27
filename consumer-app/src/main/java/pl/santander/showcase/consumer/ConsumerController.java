package pl.santander.showcase.consumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.bind.annotation.*;
import pl.santander.showcase.consumer.client.ProducerClient;
import pl.santander.showcase.consumer.dto.DecisionResponse;
import io.micrometer.core.instrument.search.Search;

import java.util.Map;

@RestController
@RequestMapping("/consumer")
public class ConsumerController {
    private final ProducerClient producer;
    private final FeatureFlagService flags;
    private final MeterRegistry reg;

    public ConsumerController(ProducerClient producer, FeatureFlagService flags, MeterRegistry reg) {
        this.producer = producer;
        this.flags = flags;
        this.reg = reg;
    }

    @GetMapping("/do")
    public Map<String, Object> operate(@RequestParam("userId") String userId) {
        boolean on = flags.isOnForUser(userId);
        String segment = on ? "canary" : "control";
        try {
            DecisionResponse r = producer.process(userId);
            increment("shadow_diff_total", "metricKey", "decision_delta", "outcome", r.isPrimaryDecision() == r.isShadowDecision() ? "same" : "diff");
            increment("feature_flag_requests_total", "service", "consumer-service", "flag", "release.new_rules_v2", "segment", segment, "outcome", "ok");
            return Map.of("on", on, "producerOk", true, "primary", r.isPrimaryDecision(), "shadow", r.isShadowDecision(), "tookMs", r.getProcessingMs());
        } catch (Exception ex) {
            increment("feature_flag_requests_total", "service", "consumer-service", "flag", "release.new_rules_v2", "segment", segment, "outcome", "error");
            return Map.of("on", on, "producerOk", false, "error", ex.getClass().getSimpleName());
        }
    }

    private void increment(String name, String... kv) {
        Search s = reg.find(name);
        for (int i = 0; i < kv.length; i += 2) s = s.tag(kv[i], kv[i + 1]);
        Counter c = s.counter();
        if (c == null) c = Counter.builder(name).tags(kv).register(reg);
        c.increment();
    }
}
