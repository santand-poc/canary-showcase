package pl.santander.showcase.consumer;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FeatureFlagService {
    private final AtomicInteger canaryPercent = new AtomicInteger(1); // start: 1%

    public boolean isOnForUser(String userId) {
        int b = Math.floorMod(userId.hashCode(), 100);
        return b < canaryPercent.get();
    }

    public int getCanaryPercent() {
        return canaryPercent.get();
    }

    public void setCanaryPercent(int p) {
        canaryPercent.set(Math.max(0, Math.min(100, p)));
    }
}