package wtf.dettex.common.util.time;

public class TimerUtil {
    private long lastMS = System.currentTimeMillis();

    public boolean isReached(long time) {
        return System.currentTimeMillis() - this.lastMS > time;
    }

    public void reset() {
        this.lastMS = System.currentTimeMillis();
    }

    public boolean hasTimeElapsed(long time, boolean reset) {
        if (System.currentTimeMillis() - this.lastMS > time) {
            if (reset) {
                this.reset();
            }
            return true;
        } else {
            return false;
        }
    }

    public long getLastMS() {
        return this.lastMS;
    }

    public void setLastMC() {
        this.lastMS = System.currentTimeMillis();
    }

    public boolean hasTimeElapsed(long time) {
        return System.currentTimeMillis() - this.lastMS > time;
    }

    public long getTime() {
        return System.currentTimeMillis() - this.lastMS;
    }

    public void setTime(long time) {
        this.lastMS = time;
    }

    public long getTimeElapsed() {
        return System.currentTimeMillis() - this.lastMS;
    }

    public boolean passed(float time) {
        return this.passed((long)time);
    }

    public boolean passed(long time) {
        return System.currentTimeMillis() - this.lastMS > time;
    }
}
