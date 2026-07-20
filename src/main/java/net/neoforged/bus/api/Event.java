package net.neoforged.bus.api;
public class Event {
    public enum Result { DENY, DEFAULT, ALLOW }
    private boolean canceled;
    private Result result = Result.DEFAULT;
    public boolean isCanceled() { return canceled; }
    public void setCanceled(boolean canceled) { this.canceled = canceled; }
    public Result getResult() { return result; }
    public void setResult(Result result) { this.result = result == null ? Result.DEFAULT : result; }
}
