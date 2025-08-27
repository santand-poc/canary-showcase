package pl.santander.featureflag.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "canary")
public class CanaryProperties {
    private String expr = "true";
    private List<Integer> ladder = List.of(1, 5, 25, 100);
    private int passStreak = 3;
    private long timeoutMs = 50;

    public String getExpr() {
        return expr;
    }

    public void setExpr(String expr) {
        this.expr = expr;
    }

    public List<Integer> getLadder() {
        return ladder;
    }

    public void setLadder(List<Integer> ladder) {
        this.ladder = ladder;
    }

    public int getPassStreak() {
        return passStreak;
    }

    public void setPassStreak(int passStreak) {
        this.passStreak = passStreak;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
