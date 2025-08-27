package pl.santander.showcase.producer.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Random;

@RestController
@RequestMapping("/api/producer")
public class ProducerController {
    private static final Logger log = LoggerFactory.getLogger(ProducerController.class);

    private final Random rnd = new Random();
    private volatile double extraErr = 0.0;
    private volatile int extraLatencyMs = 0;
    private volatile double diffProb = 0.0;
    private volatile long injectUntil = 0L;

    @GetMapping("/process")
    public DecisionResponse process(@RequestParam("userId") String userId) throws InterruptedException {
        long start = System.nanoTime();
        long now = System.currentTimeMillis();
        double errProb = (now < injectUntil) ? extraErr : 0.0;
        int latency = (now < injectUntil) ? extraLatencyMs : 0;
        double dprob = (now < injectUntil) ? diffProb : 0.0;

        log.info("INJECT ON:{}, err:{}, latency:{}, diffProb:{} ", now < injectUntil, errProb, latency, dprob);
        if (latency > 0) Thread.sleep(latency);

        boolean primary = rnd.nextBoolean();
        boolean shadow = (rnd.nextDouble() < dprob) ? !primary : primary;
        boolean ok = rnd.nextDouble() >= errProb;

        if (!ok) {
            throw new RuntimeException();
        }

        long tookMs = (System.nanoTime() - start) / 1_000_000;
        return new DecisionResponse(true, primary, shadow, tookMs);
    }

    @PostMapping("/inject")
    public String inject(@RequestParam(name = "err", defaultValue = "0.02") double err,
                         @RequestParam(name = "latencyMs", defaultValue = "200") int latencyMs,
                         @RequestParam(name = "diff", defaultValue = "0.1") double diff,
                         @RequestParam(name = "sec", defaultValue = "30") int sec) {
        this.extraErr = Math.max(0, err);
        this.extraLatencyMs = Math.max(0, latencyMs);
        this.diffProb = Math.max(0, Math.min(1.0, diff));
        this.injectUntil = System.currentTimeMillis() + sec * 1000L;
        return "OK";
    }
}
