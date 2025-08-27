package pl.santander.showcase.consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TrafficSimulator {
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "traffic-sim");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Random rnd = new Random();
    private final RestClient http;
    private final String baseUrl;
    private final String producerUrl;

    public TrafficSimulator(@Value("${server.port:8080}") int port,
                            @Value("${producer.url:http://localhost:9090}") String producerUrl) {
        this.baseUrl = "http://localhost:" + port;
        this.producerUrl = producerUrl;
        this.http = RestClient.create();
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            exec.scheduleAtFixedRate(this::tick, 0, 100, TimeUnit.MILLISECONDS);
        }
    }

    public void stop() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public void injectOnProducer(double err, int latencyMs, double diff, int sec) {
        try {
            http.post().uri(URI.create(producerUrl + "/api/producer/inject?err=" + err + "&latencyMs=" + latencyMs + "&diff=" + diff + "&sec=" + sec))
                    .retrieve().toBodilessEntity();
        } catch (Exception ignored) {
        }
    }

    private void tick() {
        if (!running.get()) return;
        for (int i = 0; i < 50; i++) {
            String userId = "u" + rnd.nextInt(1_000_000);
            try {
                http.get().uri(URI.create(baseUrl + "/consumer/do?userId=" + userId)).retrieve().toBodilessEntity();
            } catch (Exception ignored) {
            }
        }
    }
}
