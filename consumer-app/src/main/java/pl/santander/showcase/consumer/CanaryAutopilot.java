package pl.santander.showcase.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.santander.featureflag.engine.GuardEngine;
import pl.santander.featureflag.spring.CanaryProperties;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CanaryAutopilot {
    private static final Logger log = LoggerFactory.getLogger(CanaryAutopilot.class);

    private final GuardEngine engine;
    private final CanaryProperties props;
    private final FeatureFlagService flags;

    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AtomicInteger passStreak = new AtomicInteger(0);

    public CanaryAutopilot(GuardEngine engine, CanaryProperties props, FeatureFlagService flags) {
        this.engine = engine;
        this.props = props;
        this.flags = flags;
    }

    @Scheduled(fixedDelayString = "${canary.autopilot.tick-ms:2000}")
    public void tick() {
        if (!enabled.get()) return;

        GuardEngine.CompiledGuard compiled = engine.compile(props.getExpr());
        GuardEngine.Result res = compiled.evaluate(props.getTimeoutMs());

        switch (res.status()) {
            case PASS -> {
                int s = passStreak.incrementAndGet();
                log.debug("GUARD PASS (streak={}/{})", s, props.getPassStreak());
                if (s >= props.getPassStreak()) {
                    promote();
                    passStreak.set(0);
                }
            }
            case FAIL -> {
                log.info("GUARD FAIL → rollback one step");
                rollback();
                passStreak.set(0);
            }
            case WAIT -> {
                log.debug("GUARD WAIT: {}", res.note());
                passStreak.set(0);
            }
        }
    }

    private void promote() {
        List<Integer> ladder = props.getLadder();
        int cur = flags.getCanaryPercent();
        int i = ladder.indexOf(cur);
        if (i >= 0 && i < ladder.size() - 1) {
            int next = ladder.get(i + 1);
            flags.setCanaryPercent(next);
            log.info("PROMOTE canary {}% → {}%", cur, next);
        }
    }

    private void rollback() {
        List<Integer> ladder = props.getLadder();
        int cur = flags.getCanaryPercent();
        int i = ladder.indexOf(cur);
        if (i > 0) {
            int prev = ladder.get(i - 1);
            flags.setCanaryPercent(prev);
            log.warn("ROLLBACK canary {}% → {}%", cur, prev);
        } else {
            log.warn("ROLLBACK requested - already lowest: {}%", cur);
        }
    }

    public boolean isEnabled() { return enabled.get(); }
    public void setEnabled(boolean on) { enabled.set(on); }
}
