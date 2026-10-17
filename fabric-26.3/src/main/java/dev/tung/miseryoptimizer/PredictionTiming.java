package dev.tung.miseryoptimizer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Tracks observed acknowledgements without changing the server's authority. */
public final class PredictionTiming {
    private final LongSupplier clock;
    private final Map<Integer, Long> crystals = new LinkedHashMap<>();
    private final Map<Long, Long> anchors = new LinkedHashMap<>();
    private double observedMillis;
    private int samples;
    private long confirmed;
    private long corrected;

    public PredictionTiming(LongSupplier clock) {
        this.clock = clock;
    }

    public void startCrystal(int id) { start(crystals, id); }
    public void startAnchor(long pos) { start(anchors, pos); }
    public void confirmCrystal(int id) { finish(crystals, id, false); }
    public void confirmAnchor(long pos) { finish(anchors, pos, false); }
    public void rejectCrystal(int id) { finish(crystals, id, true); }
    public void rejectAnchor(long pos) { finish(anchors, pos, true); }

    private <K> void start(Map<K, Long> pending, K key) {
        if (pending.containsKey(key)) return;
        if (pending.size() >= 256) pending.remove(pending.keySet().iterator().next());
        pending.put(key, clock.getAsLong());
    }

    private <K> void finish(Map<K, Long> pending, K key, boolean rejected) {
        Long started = pending.remove(key);
        if (started == null) return;
        long elapsed = clock.getAsLong() - started;
        if (elapsed >= 0 && elapsed <= 8_000) {
            observedMillis = samples == 0 ? elapsed : observedMillis * 0.75 + elapsed * 0.25;
            samples++;
        }
        if (rejected) corrected++;
        else confirmed++;
    }

    public long timeoutMillis(int ping, int grace, boolean adaptive) {
        long normal = PredictionRegistry.timeoutMillis(ping, grace);
        if (!adaptive || samples == 0) return normal;
        return Math.min(8_000L, Math.max(normal, Math.round(observedMillis * 2.0 + 250.0)));
    }

    public long confirmed() { return confirmed; }
    public long corrected() { return corrected; }
    public int samples() { return samples; }
    public long observedMillis() { return Math.round(observedMillis); }

    public void clear() {
        crystals.clear();
        anchors.clear();
        observedMillis = 0;
        samples = 0;
        confirmed = 0;
        corrected = 0;
    }
}
