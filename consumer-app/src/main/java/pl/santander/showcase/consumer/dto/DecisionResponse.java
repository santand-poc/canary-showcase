package pl.santander.showcase.consumer.dto;

public class DecisionResponse {
    private boolean ok;
    private boolean primaryDecision;
    private boolean shadowDecision;
    private long processingMs;

    public DecisionResponse() {
    }

    public DecisionResponse(boolean ok, boolean primaryDecision, boolean shadowDecision, long processingMs) {
        this.ok = ok;
        this.primaryDecision = primaryDecision;
        this.shadowDecision = shadowDecision;
        this.processingMs = processingMs;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public boolean isPrimaryDecision() {
        return primaryDecision;
    }

    public void setPrimaryDecision(boolean primaryDecision) {
        this.primaryDecision = primaryDecision;
    }

    public boolean isShadowDecision() {
        return shadowDecision;
    }

    public void setShadowDecision(boolean shadowDecision) {
        this.shadowDecision = shadowDecision;
    }

    public long getProcessingMs() {
        return processingMs;
    }

    public void setProcessingMs(long processingMs) {
        this.processingMs = processingMs;
    }
}